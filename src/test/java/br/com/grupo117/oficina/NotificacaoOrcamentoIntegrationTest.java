package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.NotificacaoPort;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.*;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import br.com.grupo117.oficina.atendimento.infrastructure.notification.MensagemDeStatus;
import br.com.grupo117.oficina.atendimento.infrastructure.notification.OrcamentoEmailDispatcher;
import br.com.grupo117.oficina.atendimento.infrastructure.service.ItensOrcamentoOSService;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@TestPropertySource(properties = {"oficina.notificacoes.habilitadas=true", "oficina.notificacoes.intervalo-ms=3600000", "management.health.mail.enabled=false"})
class NotificacaoOrcamentoIntegrationTest extends AbstractIntegrationTest {
    @Autowired OrdemDeServicoService service;
    @Autowired ItensOrcamentoOSService itens;
    @Autowired OrdemDeServicoRepositorio ordens;
    @Autowired NotificacaoPort notificacoes;
    @Autowired JdbcTemplate jdbc;
    @Autowired OrcamentoEmailDispatcher dispatcher;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean JavaMailSender mail;

    @BeforeEach
    void limparFila() { jdbc.update("DELETE FROM notificacao_orcamento"); }

    @ParameterizedTest
    @ValueSource(strings = {"/submissao", "/diagnostico/conclusao"})
    void geraPelaApiEEnviaUmaVezComSnapshotCompleto(String rota) {
        var os = preparar(true);
        if (rota.contains("diagnostico")) service.iniciarDiagnostico(os.id());
        var resposta = restTemplate.exchange("/api/ordens-de-servico/" + os.id() + rota,
                HttpMethod.POST, new HttpEntity<>(headers()), String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(mail);
        int emails = rota.contains("diagnostico") ? 3 : 2;
        assertThat(quantidade()).isEqualTo(emails);
        assertThat(contarStatus("AGUARDANDO_APROVACAO")).isEqualTo(1);
        // Repetição HTTP inválida não cria outra mensagem.
        assertThat(restTemplate.exchange("/api/ordens-de-servico/" + os.id() + rota,
                HttpMethod.POST, new HttpEntity<>(headers()), String.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(contarStatus("AGUARDANDO_APROVACAO")).isEqualTo(1);
        dispatcher.enviarPendentes();
        dispatcher.enviarPendentes();
        var captura = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, times(emails)).send(captura.capture());
        assertThat(captura.getAllValues()).anySatisfy(mensagem -> {
            assertThat(mensagem.getTo()).containsExactly("maria@exemplo.com");
            assertThat(mensagem.getText()).contains(os.id().toString(), "Troca de óleo", "Óleo",
                    "80,50", "25,25", "156,25", "ABC1D23");
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacao_orcamento WHERE enviada_em IS NOT NULL", Integer.class))
                .isEqualTo(emails);
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
    }

    @Test
    void falhaSmtpPreservaOrcamentoEReenviaDepoisSemDuplicarBaixa() {
        var os = preparar(true);
        service.submeter(os.id());
        doThrow(new MailSendException("indisponível")).when(mail).send(any(SimpleMailMessage.class));
        dispatcher.enviarPendentes();
        assertThat(erro("AGUARDANDO_APROVACAO")).isEqualTo("FALHA_SMTP");
        assertThat(tentativas("AGUARDANDO_APROVACAO")).isEqualTo(1);
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().orcamento()).isPresent();
        dispatcher.enviarPendentes(); // não tenta de novo antes do prazo
        verify(mail, times(2)).send(any(SimpleMailMessage.class));
        doNothing().when(mail).send(any(SimpleMailMessage.class));
        liberarTentativa();
        dispatcher.enviarPendentes();
        dispatcher.enviarPendentes();
        verify(mail, times(4)).send(any(SimpleMailMessage.class));
        assertThat(tentativas("AGUARDANDO_APROVACAO")).isEqualTo(2);
        assertThat(enviada("AGUARDANDO_APROVACAO")).isTrue();
    }

    @Test
    void legadoSemEmailAguardaCadastroESoEntaoEnvia() {
        var os = preparar(false);
        service.submeter(os.id());
        dispatcher.enviarPendentes();
        verifyNoInteractions(mail);
        assertThat(erro("AGUARDANDO_APROVACAO")).isEqualTo("AGUARDANDO_EMAIL");
        var atualizado = restTemplate.exchange("/api/clientes/52998224725", HttpMethod.PUT,
                new HttpEntity<>(Map.of("nome", "Maria", "email", "maria@exemplo.com"), headers()), String.class);
        assertThat(atualizado.getStatusCode()).isEqualTo(HttpStatus.OK);
        liberarTentativa();
        dispatcher.enviarPendentes();
        verify(mail, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void rollbackNaoPublicaMensagemNemEnviaEmail() {
        var os = preparar(true);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            service.submeter(os.id());
            assertThat(contarStatus("AGUARDANDO_APROVACAO")).isEqualTo(1);
            dispatcher.enviarPendentes(); // transação separada não vê o orçamento ainda não confirmado
            tx.setRollbackOnly();
        });
        assertThat(contarStatus("AGUARDANDO_APROVACAO")).isZero();
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().status()).isEqualTo(StatusOS.RECEBIDA);
        dispatcher.enviarPendentes();
        verify(mail, never()).send(org.mockito.ArgumentMatchers.<SimpleMailMessage>argThat(mensagem -> mensagem.getText() != null
                && mensagem.getText().contains("pronto para aprovação")));
    }

    @Test
    void falhaNaFilaDesfazGeracaoDoOrcamento() {
        var os = preparar(true);
        jdbc.execute("ALTER TABLE notificacao_orcamento ADD CONSTRAINT ck_fila_teste CHECK (status <> 'AGUARDANDO_APROVACAO')");
        try {
            assertThatThrownBy(() -> service.submeter(os.id())).isInstanceOf(RuntimeException.class);
            assertThat(ordens.buscarPorId(os.id()).orElseThrow().orcamento()).isEmpty();
            assertThat(contarStatus("AGUARDANDO_APROVACAO")).isZero();
            assertThat(contarStatus("RECEBIDA")).isEqualTo(1);
            verifyNoInteractions(mail);
        } finally { jdbc.execute("ALTER TABLE notificacao_orcamento DROP CONSTRAINT ck_fila_teste"); }
    }

    @Test
    void cadastroValidaEmailPreservaContatoEConfidencialidade() {
        for (String email : new String[]{"invalido", "", "a@b.com\r\nBcc:x@y.com", "a@" + "a.".repeat(10000)}) {
            var resposta = restTemplate.exchange("/api/clientes", HttpMethod.POST,
                    new HttpEntity<>(Map.of("nome", "Maria", "cpfCnpj", "52998224725", "email", email), headers()), String.class);
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        var os = preparar(true);
        var atualizado = restTemplate.exchange("/api/clientes/52998224725", HttpMethod.PUT,
                new HttpEntity<>(Map.of("nome", "Maria Souza"), headers()), Map.class);
        assertThat(atualizado.getBody()).containsEntry("email", "maria@exemplo.com");
        var publico = restTemplate.getForObject("/api/acompanhamento/ordens-de-servico/" + os.id(), Map.class);
        assertThat(publico).doesNotContainKey("email");
        assertThat(restTemplate.getForEntity("/api/clientes/52998224725", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aberturaDaOsPersisteContatoSemSobrescreverContatoExistente() {
        var corpo = Map.of("documento", "52998224725", "nome", "Maria", "placa", "ABC1D23",
                "marca", "Fiat", "modelo", "Uno", "ano", 2012, "email", "maria@exemplo.com");
        assertThat(restTemplate.exchange("/api/ordens-de-servico", HttpMethod.POST,
                new HttpEntity<>(corpo, headers()), String.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(jdbc.queryForObject("SELECT email FROM cliente WHERE cpf_cnpj = '52998224725'", String.class))
                .isEqualTo("maria@exemplo.com");
        service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012,
                null, null, null, "outro@exemplo.com");
        assertThat(jdbc.queryForObject("SELECT email FROM cliente WHERE cpf_cnpj = '52998224725'", String.class))
                .isEqualTo("maria@exemplo.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"aprovacao", "rejeicao"})
    void cancelaPendenteSemEmailAposDecisaoMesmoComContatoCadastradoDepois(String decisao) {
        var os = prepararSemReservas();
        service.submeter(os.id());
        dispatcher.enviarPendentes();
        assertThat(erro("AGUARDANDO_APROVACAO")).isEqualTo("AGUARDANDO_EMAIL");
        decidir(os.id(), decisao);
        assertThat(restTemplate.exchange("/api/clientes/52998224725", HttpMethod.PUT,
                new HttpEntity<>(Map.of("nome", "Maria", "email", "maria@exemplo.com"), headers()),
                String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        liberarTentativa();
        dispatcher.enviarPendentes();
        dispatcher.enviarPendentes();
        verify(mail, never()).send(org.mockito.ArgumentMatchers.<SimpleMailMessage>argThat(mensagem -> mensagem.getSubject() != null
                && mensagem.getSubject().startsWith("Orçamento da OS ")));
        assertThat(jdbc.queryForObject("SELECT cancelada_em IS NOT NULL AND enviada_em IS NULL "
                + "AND ultimo_erro IS NULL FROM notificacao_orcamento WHERE status = 'AGUARDANDO_APROVACAO'", Boolean.class)).isTrue();
        assertThat(tentativas("AGUARDANDO_APROVACAO")).isZero();
        assertThat(enviada(decisao.equals("rejeicao") ? MensagemDeStatus.REJEICAO : "EM_EXECUCAO")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"aprovacao", "rejeicao"})
    void naoReenviaAposFalhaSmtpSeClienteJaDecidiu(String decisao) {
        var os = prepararSemReservas();
        jdbc.update("UPDATE cliente SET email = 'maria@exemplo.com'");
        service.submeter(os.id());
        doThrow(new MailSendException("indisponivel")).when(mail).send(any(SimpleMailMessage.class));
        dispatcher.enviarPendentes();
        decidir(os.id(), decisao);
        liberarTentativa();
        dispatcher.enviarPendentes();
        verify(mail, times(1)).send(org.mockito.ArgumentMatchers.<SimpleMailMessage>argThat(mensagem -> mensagem.getSubject() != null
                && mensagem.getSubject().startsWith("Orçamento da OS ")));
        assertThat(jdbc.queryForObject("SELECT cancelada_em IS NOT NULL AND enviada_em IS NULL "
                + "FROM notificacao_orcamento WHERE status = 'AGUARDANDO_APROVACAO'", Boolean.class)).isTrue();
        assertThat(tentativas("AGUARDANDO_APROVACAO")).isEqualTo(1);
    }

    @Test
    void naoEnviaEnquantoDecisaoDaOsEstaEmTransacaoConcorrente() {
        var os = prepararSemReservas();
        jdbc.update("UPDATE cliente SET email = 'maria@exemplo.com'");
        service.submeter(os.id());
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            jdbc.queryForObject("SELECT id FROM ordem_de_servico WHERE id = ? FOR UPDATE", UUID.class, os.id());
            dispatcher.enviarPendentes();
            verifyNoInteractions(mail);
        });
        dispatcher.enviarPendentes();
        verify(mail, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void cadaTransicaoEnfileiraUmEmailERepetirNaoDuplica() {
        var os = prepararServico();
        conferirEEnviar(os.id(), "RECEBIDA", "Recebemos o seu veículo", "OS recebida " + os.id());
        service.iniciarDiagnostico(os.id());
        assertThatIllegalStateException().isThrownBy(() -> service.iniciarDiagnostico(os.id()));
        conferirEEnviar(os.id(), "EM_DIAGNOSTICO", "diagnóstico do seu veículo foi iniciado",
                "OS em diagnóstico " + os.id());
        service.gerarOrcamento(os.id());
        conferirEEnviar(os.id(), "AGUARDANDO_APROVACAO", "Troca de óleo", "Orçamento da OS " + os.id());
        itens.aprovar(os.id());
        conferirEEnviar(os.id(), "EM_EXECUCAO", "execução dos serviços começou", "OS em execução " + os.id());
        service.concluirServicos(os.id());
        conferirEEnviar(os.id(), "FINALIZADA", "foram concluídos", "OS finalizada " + os.id());
        service.registrarEntrega(os.id());
        conferirEEnviar(os.id(), "ENTREGUE", "veículo foi entregue", "OS entregue " + os.id());
        assertThatIllegalStateException().isThrownBy(() -> service.registrarEntrega(os.id()));
        new TransactionTemplate(transactions).executeWithoutResult(tx ->
                notificacoes.agendar(ordens.buscarPorId(os.id()).orElseThrow()));
        assertThat(contar(os.id(), "ENTREGUE")).isEqualTo(1);
        assertThat(quantidade()).isEqualTo(6);
        dispatcher.enviarPendentes();
        verify(mail, times(6)).send(any(SimpleMailMessage.class));
    }

    @Test
    void rejeicaoEnfileiraEmailProprioECancelaSolicitacaoAindaPendente() {
        var os = prepararServico();
        service.submeter(os.id());
        itens.rejeitar(os.id(), "Preço acima do esperado");
        assertThatIllegalStateException().isThrownBy(() -> itens.rejeitar(os.id(), "Outro motivo"));
        conferir(os.id(), MensagemDeStatus.REJEICAO, "Preço acima do esperado",
                "Orçamento rejeitado da OS " + os.id());
        assertThat(contar(os.id(), "AGUARDANDO_APROVACAO")).isEqualTo(1);
        dispatcher.enviarPendentes();
        verify(mail, never()).send(org.mockito.ArgumentMatchers.<SimpleMailMessage>argThat(mensagem -> mensagem.getSubject() != null
                && mensagem.getSubject().startsWith("Orçamento da OS ")));
        verify(mail, times(2)).send(any(SimpleMailMessage.class));
        assertThat(jdbc.queryForObject("""
                SELECT cancelada_em IS NOT NULL AND enviada_em IS NULL
                FROM notificacao_orcamento WHERE ordem_de_servico_id = ? AND status = 'AGUARDANDO_APROVACAO'
                """, Boolean.class, os.id())).isTrue();
        assertThat(enviadaDaOs(os.id(), MensagemDeStatus.REJEICAO)).isTrue();
        assertThat(enviadaDaOs(os.id(), "RECEBIDA")).isTrue();
    }

    private OrdemDeServico prepararSemReservas() {
        var os = service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
        os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Revisao", 1, new Dinheiro(new BigDecimal("80.50"))));
        return ordens.salvar(os);
    }

    private void decidir(UUID id, String decisao) {
        Object corpo = decisao.equals("rejeicao") ? Map.of("motivo", "Nao autorizado") : null;
        assertThat(restTemplate.postForEntity("/api/acompanhamento/ordens-de-servico/" + id + "/" + decisao,
                corpo, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private OrdemDeServico preparar(boolean email) {
        if (email) assertThat(restTemplate.exchange("/api/clientes", HttpMethod.POST,
                new HttpEntity<>(Map.of("nome", "Maria", "cpfCnpj", "52998224725", "email", "maria@exemplo.com"), headers()),
                String.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var os = service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
        os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Troca de óleo", 1, new Dinheiro(new BigDecimal("80.50"))));
        os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.PECA,
                "Óleo", 3, new Dinheiro(new BigDecimal("25.25"))));
        return ordens.salvar(os);
    }

    private OrdemDeServico prepararServico() {
        assertThat(restTemplate.exchange("/api/clientes", HttpMethod.POST,
                new HttpEntity<>(Map.of("nome", "Maria", "cpfCnpj", "52998224725", "email", "maria@exemplo.com"), headers()),
                String.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var os = service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
        os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Troca de óleo", 1, new Dinheiro(new BigDecimal("80.50"))));
        return ordens.salvar(os);
    }

    private void conferirEEnviar(UUID os, String status, String trecho, String assunto) {
        conferir(os, status, trecho, assunto);
        dispatcher.enviarPendentes();
        assertThat(enviadaDaOs(os, status)).isTrue();
    }

    private void conferir(UUID os, String status, String trecho, String assunto) {
        assertThat(contar(os, status)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT corpo FROM notificacao_orcamento WHERE ordem_de_servico_id = ? AND status = ?",
                String.class, os, status)).contains(trecho, os.toString(), "ABC1D23");
        assertThat(jdbc.queryForObject(
                "SELECT assunto FROM notificacao_orcamento WHERE ordem_de_servico_id = ? AND status = ?",
                String.class, os, status)).isEqualTo(assunto);
    }

    private int contar(UUID os, String status) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM notificacao_orcamento WHERE ordem_de_servico_id = ? AND status = ?",
                Integer.class, os, status);
    }

    private boolean enviadaDaOs(UUID os, String status) {
        return jdbc.queryForObject(
                "SELECT enviada_em IS NOT NULL FROM notificacao_orcamento WHERE ordem_de_servico_id = ? AND status = ?",
                Boolean.class, os, status);
    }

    private int quantidade() { return jdbc.queryForObject("SELECT count(*) FROM notificacao_orcamento", Integer.class); }
    private int contarStatus(String status) {
        return jdbc.queryForObject("SELECT count(*) FROM notificacao_orcamento WHERE status = ?", Integer.class, status);
    }
    private String erro(String status) {
        return jdbc.queryForObject("SELECT ultimo_erro FROM notificacao_orcamento WHERE status = ?", String.class, status);
    }
    private int tentativas(String status) {
        return jdbc.queryForObject("SELECT tentativas FROM notificacao_orcamento WHERE status = ?", Integer.class, status);
    }
    private boolean enviada(String status) {
        return jdbc.queryForObject("SELECT enviada_em IS NOT NULL FROM notificacao_orcamento WHERE status = ?", Boolean.class, status);
    }
    private void liberarTentativa() { jdbc.update("UPDATE notificacao_orcamento SET proxima_tentativa_em = CURRENT_TIMESTAMP"); }
    private HttpHeaders headers() {
        var headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestSupport.token("ADMINISTRADOR"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
