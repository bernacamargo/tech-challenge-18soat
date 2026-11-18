package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.application.usecase.OrdemDeServicoNaoEncontradaException;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import br.com.grupo117.oficina.atendimento.infrastructure.api.OrdemDeServicoResponse;
import br.com.grupo117.oficina.atendimento.infrastructure.service.ItensOrcamentoOSService;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import br.com.grupo117.oficina.catalogoestoque.infrastructure.api.PecaResponse;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.*;

class ExclusaoOSIntegrationTest extends AbstractIntegrationTest {
    private static final String PATH = "/api/ordens-de-servico/";
    @Autowired private OrdemDeServicoService service;
    @Autowired private ItensOrcamentoOSService itens;
    @Autowired private OrdemDeServicoRepositorio ordens;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void limparPecas() {
        jdbc.execute("truncate table peca cascade");
    }

    @Test
    void apenasAdministradorPodeExcluir() {
        UUID id = abrir().id();
        assertThat(excluir(id.toString(), null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        for (String perfil : List.of("ATENDENTE", "MECANICO", "DESCONHECIDO")) {
            assertThat(excluir(id.toString(), perfil).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ordens.buscarPorId(id)).isPresent();
            assertThat(excluir("invalido", perfil).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
        assertThat(excluir(id.toString(), "ADMINISTRADOR").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @ParameterizedTest
    @EnumSource(StatusOS.class)
    void ocultaTodosOsStatusEPreservaRegistroFisico(StatusOS status) {
        OrdemDeServico criada = abrir();
        OrdemDeServico os = OrdemDeServico.restaurar(criada.id(), criada.clienteId(), criada.veiculoId(), status,
                status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE ? Instant.now() : null);
        ordens.salvar(os);

        assertThat(excluir(os.id().toString(), "ADMINISTRADOR").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(ordens.buscarPorId(os.id())).isEmpty();
        assertThat(ordens.listar()).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from ordem_de_servico where id = ? and excluida_em is not null",
                Integer.class, os.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from ordem_de_servico where id = ?", String.class, os.id()))
                .isEqualTo(status.name());
        assertThatThrownBy(() -> ordens.salvar(os)).isInstanceOf(OrdemDeServicoNaoEncontradaException.class);
        assertThat(restTemplate.exchange(PATH + os.id(), HttpMethod.GET,
                new HttpEntity<>(headers("ADMINISTRADOR")), String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.getForEntity("/api/acompanhamento/ordens-de-servico/" + os.id(), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.exchange("/api/ordens-de-servico", HttpMethod.GET,
                new HttpEntity<>(headers("ADMINISTRADOR")), OrdemDeServicoResponse[].class).getBody()).isEmpty();
        assertThat(excluir(os.id().toString(), "ADMINISTRADOR").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cancelaReservasENotificacoesEBloqueiaComandosPosteriores() {
        UUID peca = cadastrarPeca();
        OrdemDeServico os = abrir();
        itens.incluirPeca(os.id(), peca, 3);
        service.submeter(os.id());

        assertThat(excluir(os.id().toString(), "ADMINISTRADOR").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertEstoque(peca, 10, 0);
        assertThat(jdbc.queryForObject("select status from reserva_de_estoque where ordem_de_servico_id = ?",
                String.class, os.id())).isEqualTo("CANCELADA");
        assertThat(jdbc.queryForObject("select count(*) from item_da_os where ordem_de_servico_id = ?",
                Integer.class, os.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from notificacao_orcamento where ordem_de_servico_id = ? and cancelada_em is not null and enviada_em is null",
                Integer.class, os.id())).isEqualTo(1);
        for (String comando : List.of("diagnostico", "conclusao", "entrega", "diagnostico/conclusao", "submissao")) {
            assertThat(restTemplate.exchange(PATH + os.id() + "/" + comando, HttpMethod.POST,
                    new HttpEntity<>(headers("ADMINISTRADOR")), String.class).getStatusCode())
                    .as(comando).isEqualTo(HttpStatus.NOT_FOUND);
        }
        assertThat(restTemplate.exchange(PATH + os.id() + "/pecas", HttpMethod.POST,
                new HttpEntity<>(Map.of("pecaId", peca, "quantidade", 1), headers("ADMINISTRADOR")),
                String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.exchange("/api/acompanhamento/ordens-de-servico/" + os.id() + "/aprovacao",
                HttpMethod.POST, HttpEntity.EMPTY, String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.exchange("/api/acompanhamento/ordens-de-servico/" + os.id() + "/rejeicao",
                HttpMethod.POST, new HttpEntity<>(Map.of("motivo", "Desistência")), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void exclusaoAposAprovacaoPreservaEstoqueJaConsumido() {
        UUID peca = cadastrarPeca();
        OrdemDeServico os = abrir();
        itens.incluirPeca(os.id(), peca, 3);
        service.submeter(os.id());
        itens.aprovar(os.id());

        assertThat(excluir(os.id().toString(), "ADMINISTRADOR").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertEstoque(peca, 7, 0);
        assertThat(jdbc.queryForObject("select status from reserva_de_estoque where ordem_de_servico_id = ?",
                String.class, os.id())).isEqualTo("BAIXADA");
    }

    @Test
    void falhaNaMarcacaoReverteLiberacaoDeEstoqueENotificacao() {
        UUID peca = cadastrarPeca();
        OrdemDeServico os = abrir();
        itens.incluirPeca(os.id(), peca, 3);
        service.submeter(os.id());
        jdbc.execute("""
                CREATE FUNCTION falhar_exclusao_os() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'Falha de teste'; END $$;
                """);
        jdbc.execute("""
                CREATE TRIGGER falhar_exclusao_os BEFORE UPDATE OF excluida_em ON ordem_de_servico
                FOR EACH ROW WHEN (NEW.excluida_em IS NOT NULL) EXECUTE FUNCTION falhar_exclusao_os();
                """);
        try {
            assertThat(excluir(os.id().toString(), "ADMINISTRADOR").getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(ordens.buscarPorId(os.id())).isPresent();
            assertEstoque(peca, 10, 3);
            assertThat(jdbc.queryForObject("select status from reserva_de_estoque where ordem_de_servico_id = ?",
                    String.class, os.id())).isEqualTo("ATIVA");
            assertThat(jdbc.queryForObject("select count(*) from notificacao_orcamento where ordem_de_servico_id = ? and cancelada_em is null",
                    Integer.class, os.id())).isEqualTo(1);
        } finally {
            jdbc.execute("drop trigger falhar_exclusao_os on ordem_de_servico");
            jdbc.execute("drop function falhar_exclusao_os()");
        }
    }

    @Test
    void exclusoesConcorrentesLiberamEstoqueUmaVez() throws Exception {
        UUID peca = cadastrarPeca();
        UUID os = abrir().id();
        itens.incluirPeca(os, peca, 3);
        var primeira = CompletableFuture.supplyAsync(() -> excluir(os.toString(), "ADMINISTRADOR").getStatusCode());
        var segunda = CompletableFuture.supplyAsync(() -> excluir(os.toString(), "ADMINISTRADOR").getStatusCode());
        assertThat(List.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(HttpStatus.NO_CONTENT, HttpStatus.NOT_FOUND);
        assertEstoque(peca, 10, 0);
    }

    @Test
    void validaIdentificadorEDocumentaExclusao() {
        assertThat(excluir("invalido", "ADMINISTRADOR").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(excluir(UUID.randomUUID().toString(), "ADMINISTRADOR").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode docs = restTemplate.getForObject("/v3/api-docs", JsonNode.class);
        JsonNode operacao = docs.path("paths").path("/api/ordens-de-servico/{id}").path("delete");
        assertThat(operacao.path("security").size()).isPositive();
        for (String status : List.of("204", "400", "401", "403", "404", "409")) {
            assertThat(operacao.path("responses").has(status)).as(status).isTrue();
        }
    }

    private OrdemDeServico abrir() {
        return service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
    }

    private UUID cadastrarPeca() {
        var resposta = restTemplate.exchange("/api/pecas", HttpMethod.POST,
                new HttpEntity<>(Map.of("nome", "Óleo", "tipo", "PECA", "precoUnitario", new BigDecimal("25.25"),
                        "quantidadeInicial", 10), headers("ADMINISTRADOR")), PecaResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resposta.getBody().id();
    }

    private void assertEstoque(UUID peca, int disponivel, int reservado) {
        var saldo = restTemplate.exchange("/api/pecas/" + peca, HttpMethod.GET,
                new HttpEntity<>(headers("ADMINISTRADOR")), PecaResponse.class).getBody();
        assertThat(saldo.quantidadeDisponivel()).isEqualTo(disponivel);
        assertThat(saldo.quantidadeReservada()).isEqualTo(reservado);
    }

    private ResponseEntity<String> excluir(String id, String perfil) {
        return restTemplate.exchange(PATH + id, HttpMethod.DELETE, new HttpEntity<>(headers(perfil)), String.class);
    }

    private static HttpHeaders headers(String perfil) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (perfil != null) headers.setBearerAuth(JwtTestSupport.token(perfil));
        return headers;
    }
}
