package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import br.com.grupo117.oficina.atendimento.infrastructure.api.OrdemDeServicoResponse;
import br.com.grupo117.oficina.catalogoestoque.infrastructure.api.PecaResponse;
import br.com.grupo117.oficina.catalogoestoque.infrastructure.api.ServicoResponse;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class ItensAprovacaoOSApiIntegrationTest extends AbstractIntegrationTest {
    private static final String PATH = "/api/ordens-de-servico/";
    @Autowired private JdbcTemplate jdbc;
    @Autowired private OrdemDeServicoRepositorio ordens;

    @BeforeEach
    void limparEstoque() { jdbc.execute("truncate table peca cascade"); }

    @ParameterizedTest
    @ValueSource(strings = {"ATENDENTE", "ADMINISTRADOR"})
    void fluxoCompletoViaApiReservaAprovaIniciaConcluiEEntrega(String papel) {
        UUID servicoId = cadastrarServico();
        UUID pecaId = cadastrarPeca();
        UUID osId = abrirOS();
        var servico = post(osId, "servicos", Map.of("servicoId", servicoId, "quantidade", 1), "MECANICO");
        assertThat(servico.getStatusCode()).isEqualTo(HttpStatus.OK);
        var inclusao = post(osId, "pecas", Map.of("pecaId", pecaId, "quantidade", 3), "MECANICO");
        assertThat(inclusao.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(inclusao.getBody().itens()).hasSize(2);
        assertThat(inclusao.getBody().itens().get(1).precoUnitario()).isEqualByComparingTo("25.25");
        estoque(pecaId, 10, 3, 7);
        assertThat(post(osId, "diagnostico", null, "MECANICO").getStatusCode()).isEqualTo(HttpStatus.OK);
        var orcamento = post(osId, "diagnostico/conclusao", null, "MECANICO");
        assertThat(orcamento.getBody().orcamento().total()).isEqualByComparingTo("156.25");
        assertThat(orcamento.getBody().status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        Instant antes = Instant.now().minusSeconds(1);
        var aprovada = decidir(osId, "aprovacao", null);
        assertThat(aprovada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aprovada.getBody().path("status").asText()).isEqualTo("EM_EXECUCAO");
        OrdemDeServicoResponse detalhe = consultar(osId, papel);
        assertThat(detalhe.iniciadaEm()).isBetween(antes, Instant.now());
        assertThat(detalhe.finalizadaEm()).isNull();
        assertThat(detalhe.rejeicaoOrcamento()).isNull();
        assertThat(ordens.buscarPorId(osId).orElseThrow().iniciadaEm()).contains(detalhe.iniciadaEm());
        estoque(pecaId, 7, 0, 7);
        assertThat(decidir(osId, "aprovacao", null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        estoque(pecaId, 7, 0, 7);
        JsonNode publico = restTemplate.getForObject("/api/acompanhamento/ordens-de-servico/" + osId, JsonNode.class);
        assertThat(publico.size()).isEqualTo(2);
        assertThat(publico.path("status").asText()).isEqualTo("EM_EXECUCAO");
        assertThat(post(osId, "conclusao", null, "MECANICO").getStatusCode()).isEqualTo(HttpStatus.OK);
        var entregue = post(osId, "entrega", null, papel);
        assertThat(entregue.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entregue.getBody().status()).isEqualTo(StatusOS.ENTREGUE);
        assertThat(entregue.getBody().iniciadaEm()).isEqualTo(detalhe.iniciadaEm());
        assertThat(entregue.getBody().finalizadaEm()).isAfterOrEqualTo(detalhe.iniciadaEm());
    }

    @Test
    void aprovaOSSomenteComServicoSemMovimentarEstoque() {
        UUID os = abrirOS();
        assertThat(post(os, "servicos", Map.of("servicoId", cadastrarServico(), "quantidade", 2), "MECANICO")
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        prepararOrcamento(os);
        var aprovada = decidir(os, "aprovacao", null);
        assertThat(aprovada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aprovada.getBody().path("status").asText()).isEqualTo("EM_EXECUCAO");
        assertThat(consultar(os, "ATENDENTE").orcamento().total()).isEqualByComparingTo("161.00");
        assertThat(jdbc.queryForObject("select count(*) from reserva_de_estoque", Integer.class)).isZero();
    }

    @Test
    void exigeAutenticacaoEPapeisAntesDeAlterarOS() {
        UUID os = abrirOS();
        Map<String, Object> servico = Map.of("servicoId", cadastrarServico(), "quantidade", 1);
        Map<String, Object> peca = Map.of("pecaId", cadastrarPeca(), "quantidade", 1);
        for (String rota : new String[]{"servicos", "pecas"}) {
            Object body = rota.equals("servicos") ? servico : peca;
            assertThat(postErro(os, rota, body, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(postErro(os, rota, body, "OUTRO").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
        assertThat(postErro(os, "servicos", servico, "ATENDENTE").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(decidir(os, "aprovacao", null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ordens.buscarPorId(os).orElseThrow().itens()).isEmpty();
    }

    @Test
    void validaCorpoCatalogoEIdentificadorSemReservar() {
        UUID os = abrirOS();
        UUID peca = cadastrarPeca();
        for (String rota : new String[]{"servicos", "pecas"}) {
            String campo = rota.equals("servicos") ? "servicoId" : "pecaId";
            for (int quantidade : new int[]{0, -1}) {
                assertThat(postErro(os, rota, Map.of(campo, peca, "quantidade", quantidade), "ADMINISTRADOR")
                        .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            }
            assertThat(postErro(os, rota, Map.of(campo, peca), "ADMINISTRADOR").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(postErro(os, rota, Map.of("quantidade", 1), "ADMINISTRADOR").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(postErro(os, rota, Map.of(campo, "invalido", "quantidade", 1), "ADMINISTRADOR")
                    .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(postErro(os, rota, Map.of(campo, UUID.randomUUID(), "quantidade", 1), "ADMINISTRADOR")
                    .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        for (String rota : new String[]{"servicos", "pecas"}) {
            Object corpo = Map.of(rota.equals("pecas") ? "pecaId" : "servicoId", peca, "quantidade", 1);
            assertThat(postErro(UUID.randomUUID(), rota, corpo, "ADMINISTRADOR").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(restTemplate.exchange(PATH + "invalido/" + rota, HttpMethod.POST,
                    new HttpEntity<>(corpo, headers("ADMINISTRADOR")), String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(decidir(UUID.randomUUID(), "aprovacao", null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ordens.buscarPorId(os).orElseThrow().itens()).isEmpty();
        estoque(peca, 10, 0, 10);
    }

    @Test
    void faltaDeSaldoEEstadosInvalidosRetornam409SemEfeitos() {
        UUID os = abrirOS();
        UUID peca = cadastrarPeca();
        var resposta = postErro(os, "pecas", Map.of("pecaId", peca, "quantidade", 11), "ATENDENTE");
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().path("mensagem").asText()).contains("Estoque insuficiente");
        assertThat(ordens.buscarPorId(os).orElseThrow().itens()).isEmpty();
        estoque(peca, 10, 0, 10);
        assertThat(decidir(os, "aprovacao", null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(post(os, "pecas", Map.of("pecaId", peca, "quantidade", 1), "ATENDENTE").getStatusCode()).isEqualTo(HttpStatus.OK);
        prepararOrcamento(os);
        assertThat(postErro(os, "pecas", Map.of("pecaId", peca, "quantidade", 1), "MECANICO").getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(decidir(os, "rejeicao", Map.of("motivo", "Cliente não autorizou")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(decidir(os, "aprovacao", null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        estoque(peca, 10, 0, 10);
        assertThat(ordens.buscarPorId(os).orElseThrow().iniciadaEm()).isEmpty();
    }

    @Test
    void falhaDePersistenciaNaAprovacaoViaHttpDesfazBaixa() {
        UUID os = abrirOS();
        UUID peca = cadastrarPeca();
        assertThat(post(os, "pecas", Map.of("pecaId", peca, "quantidade", 2), "ATENDENTE").getStatusCode()).isEqualTo(HttpStatus.OK);
        prepararOrcamento(os);
        jdbc.execute("alter table ordem_de_servico add constraint ck_execucao_api_teste check (status <> 'EM_EXECUCAO')");
        try {
            assertThat(decidir(os, "aprovacao", null).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            estoque(peca, 10, 2, 8);
            assertThat(ordens.buscarPorId(os).orElseThrow().status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
            assertThat(ordens.buscarPorId(os).orElseThrow().iniciadaEm()).isEmpty();
            assertThat(jdbc.queryForObject("select status from reserva_de_estoque where ordem_de_servico_id = ?", String.class, os))
                    .isEqualTo("ATIVA");
        } finally {
            jdbc.execute("alter table ordem_de_servico drop constraint ck_execucao_api_teste");
        }
    }

    @Test
    void swaggerDocumentaNovosComandosEAutorizacao() {
        JsonNode docs = restTemplate.getForObject("/v3/api-docs", JsonNode.class);
        for (String rota : new String[]{"servicos", "pecas"}) {
            JsonNode operacao = docs.path("paths").path(PATH + "{id}/" + rota).path("post");
            assertThat(operacao.path("security").size()).isPositive();
            for (String status : new String[]{"200", "400", "401", "403", "404", "409"}) {
                assertThat(operacao.path("responses").has(status)).as(rota + " " + status).isTrue();
            }
        }
        JsonNode aprovacao = docs.path("paths").path("/api/acompanhamento/ordens-de-servico/{id}/aprovacao").path("post");
        JsonNode rejeicao = docs.path("paths").path("/api/acompanhamento/ordens-de-servico/{id}/rejeicao").path("post");
        assertThat(aprovacao.path("responses").has("200")).isTrue();
        assertThat(rejeicao.path("responses").has("200")).isTrue();
        assertThat(docs.path("paths").has(PATH + "{id}/aprovacao")).isFalse();
    }

    @Test
    void aberturaApontaPecaDaListaDeDisponibilidadeEReserva() {
        UUID peca = cadastrarPeca();
        var resposta = restTemplate.exchange("/api/ordens-de-servico", HttpMethod.POST,
                new HttpEntity<>(Map.of("documento", "52998224725", "nome", "Maria", "placa", "DEF2E34",
                        "marca", "Fiat", "modelo", "Uno", "ano", 2012, "pecaId", peca, "quantidade", 2),
                        headers("ATENDENTE")), OrdemDeServicoResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody().status()).isEqualTo(StatusOS.RECEBIDA);
        assertThat(resposta.getBody().itens()).hasSize(1);
        estoque(peca, 10, 2, 8);
    }

    private UUID abrirOS() {
        var resposta = restTemplate.exchange("/api/ordens-de-servico", HttpMethod.POST,
                new HttpEntity<>(Map.of("documento", "52998224725", "nome", "Maria", "placa", "ABC1D23",
                        "marca", "Fiat", "modelo", "Uno", "ano", 2012), headers("ATENDENTE")), OrdemDeServicoResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resposta.getBody().id();
    }

    private UUID cadastrarServico() {
        var resposta = restTemplate.exchange("/api/servicos", HttpMethod.POST,
                new HttpEntity<>(Map.of("nome", "Troca de óleo", "preco", new BigDecimal("80.50")), headers("ADMINISTRADOR")), ServicoResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resposta.getBody().id();
    }

    private UUID cadastrarPeca() {
        var resposta = restTemplate.exchange("/api/pecas", HttpMethod.POST,
                new HttpEntity<>(Map.of("nome", "Óleo", "tipo", "PECA", "precoUnitario", new BigDecimal("25.25"),
                        "quantidadeInicial", 10), headers("ADMINISTRADOR")), PecaResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resposta.getBody().id();
    }

    private void prepararOrcamento(UUID os) {
        assertThat(post(os, "diagnostico", null, "MECANICO").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(post(os, "diagnostico/conclusao", null, "MECANICO").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private OrdemDeServicoResponse consultar(UUID os, String papel) {
        var resposta = restTemplate.exchange(PATH + os, HttpMethod.GET,
                new HttpEntity<>(headers(papel)), OrdemDeServicoResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody();
    }

    private ResponseEntity<JsonNode> decidir(UUID os, String comando, Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange("/api/acompanhamento/ordens-de-servico/" + os + "/" + comando,
                HttpMethod.POST, new HttpEntity<>(corpo, headers), JsonNode.class);
    }

    private void estoque(UUID peca, int disponivel, int reservado, int livre) {
        var resposta = restTemplate.exchange("/api/pecas/" + peca, HttpMethod.GET,
                new HttpEntity<>(headers("ADMINISTRADOR")), PecaResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().quantidadeDisponivel()).isEqualTo(disponivel);
        assertThat(resposta.getBody().quantidadeReservada()).isEqualTo(reservado);
        assertThat(resposta.getBody().quantidadeLivre()).isEqualTo(livre);
    }

    private HttpHeaders headers(String papel) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (papel != null) headers.setBearerAuth(JwtTestSupport.token(papel));
        return headers;
    }

    private ResponseEntity<OrdemDeServicoResponse> post(UUID os, String rota, Object corpo, String papel) {
        return restTemplate.exchange(PATH + os + "/" + rota, HttpMethod.POST,
                new HttpEntity<>(corpo, headers(papel)), OrdemDeServicoResponse.class);
    }

    private ResponseEntity<JsonNode> postErro(UUID os, String rota, Object corpo, String papel) {
        return restTemplate.exchange(PATH + os + "/" + rota, HttpMethod.POST,
                new HttpEntity<>(corpo, headers(papel)), JsonNode.class);
    }
}
