package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.*;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import br.com.grupo117.oficina.atendimento.infrastructure.notification.OrcamentoEmailDispatcher;
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
    @Autowired OrdemDeServicoRepositorio ordens;
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
        assertThat(quantidade()).isEqualTo(1);
        // Repetição HTTP inválida não cria outra mensagem.
        assertThat(restTemplate.exchange("/api/ordens-de-servico/" + os.id() + rota,
                HttpMethod.POST, new HttpEntity<>(headers()), String.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        dispatcher.enviarPendentes();
        dispatcher.enviarPendentes();
        var captura = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, times(1)).send(captura.capture());
        assertThat(captura.getValue().getTo()).containsExactly("maria@exemplo.com");
        assertThat(captura.getValue().getText()).contains(os.id().toString(), "Troca de óleo", "Óleo",
                "80,50", "25,25", "156,25", "ABC1D23");
        assertThat(jdbc.queryForObject("SELECT enviada_em IS NOT NULL FROM notificacao_orcamento", Boolean.class)).isTrue();
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
    }

    @Test
    void falhaSmtpPreservaOrcamentoEReenviaDepoisSemDuplicarBaixa() {
        var os = preparar(true);
        service.submeter(os.id());
        doThrow(new MailSendException("indisponível")).doNothing().when(mail).send(any(SimpleMailMessage.class));
        dispatcher.enviarPendentes();
        assertThat(jdbc.queryForObject("SELECT ultimo_erro FROM notificacao_orcamento", String.class)).isEqualTo("FALHA_SMTP");
        assertThat(jdbc.queryForObject("SELECT tentativas FROM notificacao_orcamento", Integer.class)).isEqualTo(1);
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().orcamento()).isPresent();
        dispatcher.enviarPendentes(); // não tenta de novo antes do prazo
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
        liberarTentativa();
        dispatcher.enviarPendentes();
        dispatcher.enviarPendentes();
        verify(mail, times(2)).send(any(SimpleMailMessage.class));
        assertThat(jdbc.queryForObject("SELECT tentativas FROM notificacao_orcamento", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT enviada_em IS NOT NULL FROM notificacao_orcamento", Boolean.class)).isTrue();
    }

    @Test
    void legadoSemEmailAguardaCadastroESoEntaoEnvia() {
        var os = preparar(false);
        service.submeter(os.id());
        dispatcher.enviarPendentes();
        verifyNoInteractions(mail);
        assertThat(jdbc.queryForObject("SELECT ultimo_erro FROM notificacao_orcamento", String.class)).isEqualTo("AGUARDANDO_EMAIL");
        var atualizado = restTemplate.exchange("/api/clientes/52998224725", HttpMethod.PUT,
                new HttpEntity<>(Map.of("nome", "Maria", "email", "maria@exemplo.com"), headers()), String.class);
        assertThat(atualizado.getStatusCode()).isEqualTo(HttpStatus.OK);
        liberarTentativa();
        dispatcher.enviarPendentes();
        verify(mail).send(any(SimpleMailMessage.class));
    }

    @Test
    void rollbackNaoPublicaMensagemNemEnviaEmail() {
        var os = preparar(true);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            service.submeter(os.id());
            assertThat(quantidade()).isEqualTo(1);
            dispatcher.enviarPendentes(); // transação separada não vê dados não confirmados
            verifyNoInteractions(mail);
            tx.setRollbackOnly();
        });
        assertThat(quantidade()).isZero();
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().status()).isEqualTo(StatusOS.RECEBIDA);
        dispatcher.enviarPendentes();
        verifyNoInteractions(mail);
    }

    @Test
    void falhaNaFilaDesfazGeracaoDoOrcamento() {
        var os = preparar(true);
        jdbc.execute("ALTER TABLE notificacao_orcamento ADD CONSTRAINT ck_fila_teste CHECK (false)");
        try {
            assertThatThrownBy(() -> service.submeter(os.id())).isInstanceOf(RuntimeException.class);
            assertThat(ordens.buscarPorId(os.id()).orElseThrow().orcamento()).isEmpty();
            assertThat(quantidade()).isZero();
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

    private int quantidade() { return jdbc.queryForObject("SELECT count(*) FROM notificacao_orcamento", Integer.class); }
    private void liberarTentativa() { jdbc.update("UPDATE notificacao_orcamento SET proxima_tentativa_em = CURRENT_TIMESTAMP"); }
    private HttpHeaders headers() {
        var headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestSupport.token("ADMINISTRADOR"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
