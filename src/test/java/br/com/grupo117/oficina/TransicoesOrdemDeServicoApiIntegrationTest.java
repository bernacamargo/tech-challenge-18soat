package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.*;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import br.com.grupo117.oficina.atendimento.infrastructure.api.OrdemDeServicoResponse;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

class TransicoesOrdemDeServicoApiIntegrationTest extends AbstractIntegrationTest {
    private static final String PATH = "/api/ordens-de-servico/";
    @Autowired private OrdemDeServicoService service;
    @Autowired private OrdemDeServicoRepositorio ordens;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void iniciaDiagnosticoEPreservaItens() {
        OrdemDeServico os = criar();
        ItemDaOS item = new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Diagnóstico", 1, new Dinheiro(new BigDecimal("50.00")));
        os.incluirItem(item);
        ordens.salvar(os);
        var resposta = executar(os.id(), "diagnostico", "MECANICO", OrdemDeServicoResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().status()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().itens()).containsExactly(item);
        assertThat(resposta.getBody().itens()).hasSize(1);
    }

    @Test
    void concluiEEntregaPreservandoOrcamentoEHorarioDaConclusao() {
        OrdemDeServico os = criar();
        os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Serviço", 1, new Dinheiro(new BigDecimal("50.00"))));
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        os.gerarOrcamento();
        // Aprovação ainda não tem endpoint: prepara o estado anterior à conclusão.
        os.transicionarPara(StatusOS.EM_EXECUCAO);
        ordens.salvar(os);
        Instant antes = Instant.now();
        var conclusao = executar(os.id(), "conclusao", "MECANICO", OrdemDeServicoResponse.class);
        Instant depois = Instant.now();
        assertThat(conclusao.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(conclusao.getBody().status()).isEqualTo(StatusOS.FINALIZADA);
        Instant finalizada = conclusao.getBody().finalizadaEm();
        assertThat(finalizada).isBetween(antes, depois);
        var entrega = executar(os.id(), "entrega", "ATENDENTE", OrdemDeServicoResponse.class);
        assertThat(entrega.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entrega.getBody().status()).isEqualTo(StatusOS.ENTREGUE);
        assertThat(entrega.getBody().finalizadaEm()).isEqualTo(finalizada);
        assertThat(entrega.getBody().orcamento()).isEqualTo(conclusao.getBody().orcamento());
        OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
        assertThat(lida.status()).isEqualTo(StatusOS.ENTREGUE);
        assertThat(lida.finalizadaEm()).contains(finalizada);
    }

    @ParameterizedTest
    @CsvSource({"diagnostico,RECEBIDA", "conclusao,EM_EXECUCAO", "entrega,FINALIZADA"})
    void administradorPodeExecutarAsTresAcoes(String acao, StatusOS origem) {
        OrdemDeServico os = preparar(origem);
        assertThat(executar(os.id(), acao, "ADMINISTRADOR", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @ParameterizedTest
    @CsvSource({"diagnostico,ATENDENTE", "conclusao,ATENDENTE", "entrega,MECANICO"})
    void bloqueiaPapelSemPermissao(String acao, String papel) {
        OrdemDeServico os = criar();
        assertThat(executar(os.id(), acao, papel, String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().status()).isEqualTo(StatusOS.RECEBIDA);
    }

    @ParameterizedTest
    @CsvSource({"diagnostico,EM_DIAGNOSTICO", "conclusao,RECEBIDA", "entrega,RECEBIDA",
            "conclusao,FINALIZADA", "entrega,ENTREGUE"})
    void transicaoInvalidaOuRepetidaRetornaConflitoSemAlterarEstado(String acao, StatusOS origem) {
        OrdemDeServico os = preparar(origem);
        var resposta = executar(os.id(), acao, "ADMINISTRADOR", JsonNode.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().path("mensagem").asText()).contains("não permitida");
        OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
        assertThat(lida.status()).isEqualTo(origem);
        assertThat(lida.finalizadaEm()).isEqualTo(os.finalizadaEm());
    }

    @ParameterizedTest
    @CsvSource({"diagnostico", "conclusao", "entrega"})
    void exigeAutenticacaoETrataIdentificador(String acao) {
        assertThat(executar(UUID.randomUUID(), acao, null, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(executar(UUID.randomUUID(), acao, "ADMINISTRADOR", String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        HttpHeaders headers = headers("ADMINISTRADOR");
        assertThat(restTemplate.exchange(PATH + "invalido/" + acao, HttpMethod.POST,
                new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void falhaNaPersistenciaDesfazTransicao() {
        OrdemDeServico os = criar();
        jdbc.execute("alter table ordem_de_servico add constraint ck_transicao_teste check (status <> 'EM_DIAGNOSTICO')");
        try {
            assertThat(executar(os.id(), "diagnostico", "MECANICO", String.class).getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(ordens.buscarPorId(os.id()).orElseThrow().status()).isEqualTo(StatusOS.RECEBIDA);
        } finally {
            jdbc.execute("alter table ordem_de_servico drop constraint ck_transicao_teste");
        }
    }

    @Test
    void swaggerDocumentaAcoesEConflitos() {
        JsonNode docs = restTemplate.getForObject("/v3/api-docs", JsonNode.class);
        for (String acao : new String[]{"diagnostico", "conclusao", "entrega"}) {
            JsonNode operacao = docs.path("paths").path(PATH + "{id}/" + acao).path("post");
            assertThat(operacao.path("responses").has("409")).isTrue();
            assertThat(operacao.path("security").size()).isPositive();
        }
    }

    private OrdemDeServico criar() {
        return service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
    }

    private OrdemDeServico preparar(StatusOS status) {
        OrdemDeServico os = criar();
        Instant finalizacao = status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE
                ? Instant.parse("2026-10-13T12:00:00Z") : null;
        return ordens.salvar(OrdemDeServico.restaurar(os.id(), os.clienteId(), os.veiculoId(), status, finalizacao));
    }

    private HttpHeaders headers(String papel) {
        HttpHeaders headers = new HttpHeaders();
        if (papel != null) headers.setBearerAuth(JwtTestSupport.token(papel));
        return headers;
    }

    private <T> ResponseEntity<T> executar(UUID id, String acao, String papel, Class<T> resposta) {
        return restTemplate.exchange(PATH + id + "/" + acao, HttpMethod.POST,
                new HttpEntity<>(headers(papel)), resposta);
    }
}
