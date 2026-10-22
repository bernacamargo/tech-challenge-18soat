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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

class GerarOrcamentoApiIntegrationTest extends AbstractIntegrationTest {
    private static final String PATH = "/api/ordens-de-servico/";
    @Autowired private OrdemDeServicoService service;
    @Autowired private OrdemDeServicoRepositorio ordens;
    @Autowired private JdbcTemplate jdbc;

    @ParameterizedTest
    @ValueSource(strings = {"ATENDENTE", "ADMINISTRADOR"})
    void geraTotalDosItensPersistidosEPreservaNaConsulta(String papel) {
        OrdemDeServico os = preparar(StatusOS.EM_DIAGNOSTICO, true);
        var resposta = gerar(os.id().toString(), papel, OrdemDeServicoResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(resposta.getBody().orcamento().total()).isEqualByComparingTo("156.25");
        assertThat(resposta.getBody().finalizadaEm()).isNull();
        OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
        assertThat(lida.itens()).containsExactlyElementsOf(os.itens());
        assertThat(lida.orcamento().orElseThrow().total().valor()).isEqualByComparingTo("156.25");
        var detalhe = restTemplate.exchange(PATH + os.id(), HttpMethod.GET,
                new HttpEntity<>(headers(papel)), OrdemDeServicoResponse.class);
        assertThat(detalhe.getBody()).isEqualTo(resposta.getBody());
        JsonNode publico = restTemplate.getForObject("/api/acompanhamento/ordens-de-servico/" + os.id(), JsonNode.class);
        assertThat(publico.path("status").asText()).isEqualTo("AGUARDANDO_APROVACAO");
        assertThat(publico.size()).isEqualTo(2);
    }

    @Test
    void osSemItensRetorna400SemAlterarDiagnostico() {
        OrdemDeServico os = preparar(StatusOS.EM_DIAGNOSTICO, false);
        var resposta = gerar(os.id().toString(), "ATENDENTE", JsonNode.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().path("mensagem").asText()).contains("ao menos um item");
        OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
        assertThat(lida.status()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        assertThat(lida.orcamento()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, mode = EnumSource.Mode.EXCLUDE, names = "EM_DIAGNOSTICO")
    void rejeitaEstadoIncompativel(StatusOS status) {
        OrdemDeServico os = preparar(status, true);
        assertThat(gerar(os.id().toString(), "ADMINISTRADOR", String.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
        assertThat(lida.status()).isEqualTo(status);
        assertThat(lida.itens()).isEqualTo(os.itens());
        assertThat(lida.orcamento()).isEqualTo(os.orcamento());
    }

    @Test
    void repeticaoRetorna409EPreservaOrcamentoOriginal() {
        OrdemDeServico os = preparar(StatusOS.EM_DIAGNOSTICO, true);
        var primeira = gerar(os.id().toString(), "ATENDENTE", OrdemDeServicoResponse.class);
        assertThat(primeira.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(gerar(os.id().toString(), "ATENDENTE", String.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().orcamento().orElseThrow().total().valor())
                .isEqualByComparingTo(primeira.getBody().orcamento().total());
    }

    @Test
    void exigePapelAutorizadoETrataIdentificador() {
        OrdemDeServico os = preparar(StatusOS.EM_DIAGNOSTICO, true);
        assertThat(gerar(os.id().toString(), null, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(gerar(os.id().toString(), "MECANICO", String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().orcamento()).isEmpty();
        assertThat(gerar(UUID.randomUUID().toString(), "ATENDENTE", String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(gerar("invalido", "ATENDENTE", String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void falhaAoGravarOrcamentoDesfazTransicao() {
        OrdemDeServico os = preparar(StatusOS.EM_DIAGNOSTICO, true);
        jdbc.execute("alter table ordem_de_servico add constraint ck_orcamento_teste check (orcamento_total is null)");
        try {
            assertThat(gerar(os.id().toString(), "ATENDENTE", String.class).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
            assertThat(lida.status()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
            assertThat(lida.orcamento()).isEmpty();
            assertThat(lida.itens()).isEqualTo(os.itens());
        } finally {
            jdbc.execute("alter table ordem_de_servico drop constraint ck_orcamento_teste");
        }
    }

    @Test
    void swaggerDocumentaGeracao() {
        JsonNode docs = restTemplate.getForObject("/v3/api-docs", JsonNode.class);
        JsonNode operacao = docs.path("paths").path(PATH + "{id}/orcamento").path("post");
        assertThat(operacao.path("responses").has("400")).isTrue();
        assertThat(operacao.path("responses").has("409")).isTrue();
        assertThat(operacao.path("security").size()).isPositive();
    }

    private OrdemDeServico preparar(StatusOS status, boolean comItens) {
        OrdemDeServico os = service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
        if (comItens) {
            os.incluirItem(item(TipoItemOS.SERVICO, "Troca de óleo", 1, "80.50"));
            os.incluirItem(item(TipoItemOS.PECA, "Óleo", 3, "25.25"));
        }
        Instant finalizacao = status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE
                ? Instant.parse("2026-10-13T12:00:00Z") : null;
        return ordens.salvar(OrdemDeServico.restaurar(os.id(), os.clienteId(), os.veiculoId(), status,
                finalizacao, os.itens(), null));
    }

    private ItemDaOS item(TipoItemOS tipo, String descricao, int quantidade, String preco) {
        return new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), tipo, descricao, quantidade, new Dinheiro(new BigDecimal(preco)));
    }

    private HttpHeaders headers(String papel) {
        HttpHeaders headers = new HttpHeaders();
        if (papel != null) headers.setBearerAuth(JwtTestSupport.token(papel));
        return headers;
    }

    private <T> ResponseEntity<T> gerar(String id, String papel, Class<T> resposta) {
        return restTemplate.exchange(PATH + id + "/orcamento", HttpMethod.POST, new HttpEntity<>(headers(papel)), resposta);
    }
}
