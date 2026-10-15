package br.com.grupo117.oficina;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import br.com.grupo117.oficina.catalogoestoque.domain.EstoqueBaixado;
import br.com.grupo117.oficina.catalogoestoque.domain.EstoqueReabastecido;
import br.com.grupo117.oficina.catalogoestoque.domain.EventoDeDominio;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import br.com.grupo117.oficina.catalogoestoque.domain.PecaReservada;
import br.com.grupo117.oficina.catalogoestoque.domain.TipoItemEstoque;
import br.com.grupo117.oficina.catalogoestoque.infrastructure.api.BaixaResponse;
import br.com.grupo117.oficina.catalogoestoque.infrastructure.api.PecaResponse;
import br.com.grupo117.oficina.catalogoestoque.infrastructure.api.ReservaResponse;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Import(CatalogoEstoqueApiIntegrationTest.EventosConfig.class)
class CatalogoEstoqueApiIntegrationTest extends AbstractIntegrationTest {
    private static final String PECAS = "/api/pecas";
    private static final String RESERVAS = "/api/estoque/reservas";

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PecaRepositorio pecas;
    @Autowired
    private PlatformTransactionManager transacoes;
    @Autowired
    private EventosRegistrados eventos;

    @BeforeEach
    void limparEstoque() {
        jdbc.execute("TRUNCATE TABLE peca CASCADE");
        eventos.limpar();
    }

    @Test
    void cadastraReservaEBaixaConsumindoAReserva() {
        PecaResponse filtro = cadastrar("Filtro de oleo", 5);
        UUID os = UUID.randomUUID();

        ResponseEntity<ReservaResponse> reserva = post(RESERVAS, reserva(os, filtro.id(), 3), "ATENDENTE",
                ReservaResponse.class);
        assertThat(reserva.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reserva.getBody()).isNotNull();
        assertThat(detalhar(filtro.id())).extracting(PecaResponse::quantidadeDisponivel,
                PecaResponse::quantidadeReservada, PecaResponse::quantidadeLivre).containsExactly(5, 3, 2);

        BaixaResponse baixa = post(baixa(os), null, "MECANICO", BaixaResponse.class).getBody();
        assertThat(baixa).isEqualTo(new BaixaResponse(1));
        assertThat(detalhar(filtro.id())).extracting(PecaResponse::quantidadeDisponivel,
                PecaResponse::quantidadeReservada).containsExactly(2, 0);
        assertThat(jdbc.queryForObject("select status from reserva_de_estoque where id = ?", String.class,
                reserva.getBody().reservaId())).isEqualTo("BAIXADA");

        BaixaResponse repetida = post(baixa(os), null, "MECANICO", BaixaResponse.class).getBody();
        assertThat(repetida).isEqualTo(new BaixaResponse(0));
        assertThat(detalhar(filtro.id()).quantidadeDisponivel()).isEqualTo(2);

        assertThat(eventos.stream(PecaReservada.class)).hasSize(1);
        assertThat(eventos.stream(EstoqueBaixado.class)).singleElement()
                .extracting(EstoqueBaixado::quantidade).isEqualTo(3);
    }

    @Test
    void crudCompletoEEntradaNoEstoque() {
        PecaResponse oleo = cadastrar("Oleo 5W30", 10);
        assertThat(oleo.precoUnitario()).isEqualByComparingTo("35.90");

        ResponseEntity<PecaResponse[]> lista = restTemplate.exchange(PECAS, HttpMethod.GET,
                new HttpEntity<>(headers("ATENDENTE")), PecaResponse[].class);
        assertThat(lista.getBody()).containsExactly(oleo);

        Map<String, Object> alteracao = Map.of("nome", "Oleo 5W40", "tipo", "INSUMO", "precoUnitario", "42");
        ResponseEntity<PecaResponse> alterada = restTemplate.exchange(PECAS + "/" + oleo.id(), HttpMethod.PUT,
                new HttpEntity<>(alteracao, headers("ADMINISTRADOR")), PecaResponse.class);
        assertThat(alterada.getBody()).isNotNull();
        assertThat(alterada.getBody().nome()).isEqualTo("Oleo 5W40");
        assertThat(alterada.getBody().precoUnitario()).isEqualByComparingTo("42.00");
        assertThat(alterada.getBody().quantidadeDisponivel()).isEqualTo(10);

        PecaResponse reabastecida = post(PECAS + "/" + oleo.id() + "/entradas", Map.of("quantidade", 5),
                "ADMINISTRADOR", PecaResponse.class).getBody();
        assertThat(reabastecida).isNotNull();
        assertThat(reabastecida.quantidadeDisponivel()).isEqualTo(15);
        assertThat(eventos.stream(EstoqueReabastecido.class)).hasSize(1);

        ResponseEntity<Void> removida = restTemplate.exchange(PECAS + "/" + oleo.id(), HttpMethod.DELETE,
                new HttpEntity<>(headers("ADMINISTRADOR")), Void.class);
        assertThat(removida.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(getStatus(PECAS + "/" + oleo.id(), "ADMINISTRADOR")).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void reservaAcimaDoLivreRetorna409SemAlterarOEstoque() {
        PecaResponse vela = cadastrar("Vela", 2);

        ResponseEntity<String> resposta = post(RESERVAS, reserva(UUID.randomUUID(), vela.id(), 3), "ATENDENTE",
                String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).contains("Estoque insuficiente: solicitado 3, livre 2");
        assertThat(detalhar(vela.id()).quantidadeReservada()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from reserva_de_estoque", Long.class)).isZero();
    }

    @Test
    void pecaInexistenteRetorna404() {
        UUID ausente = UUID.randomUUID();

        assertThat(getStatus(PECAS + "/" + ausente, "ATENDENTE")).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(post(PECAS + "/" + ausente + "/entradas", Map.of("quantidade", 1), "ADMINISTRADOR",
                String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ResponseEntity<String> reserva = post(RESERVAS, reserva(UUID.randomUUID(), ausente, 1), "ATENDENTE",
                String.class);
        assertThat(reserva.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(reserva.getBody()).contains("Peca nao encontrada");
    }

    @Test
    void entradaInvalidaRetorna400() {
        Map<String, Object> semPreco = Map.of("nome", "Vela", "tipo", "PECA", "quantidadeInicial", 1);
        Map<String, Object> tipoInvalido = Map.of("nome", "Vela", "tipo", "OUTRO", "precoUnitario", "10",
                "quantidadeInicial", 1);
        PecaResponse vela = cadastrar("Vela", 1);

        assertThat(post(PECAS, semPreco, "ADMINISTRADOR", String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(post(PECAS, tipoInvalido, "ADMINISTRADOR", String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(post(PECAS + "/" + vela.id() + "/entradas", Map.of("quantidade", 0), "ADMINISTRADOR",
                String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(getStatus(PECAS + "/invalido", "ADMINISTRADOR")).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void exigeTokenEPapelAdequado() {
        PecaResponse vela = cadastrar("Vela", 4);

        assertThat(restTemplate.getForEntity(PECAS, String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.postForEntity(RESERVAS, reserva(UUID.randomUUID(), vela.id(), 1), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(post(PECAS, novaPeca("Pastilha", 1), "ATENDENTE", String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post(PECAS + "/" + vela.id() + "/entradas", Map.of("quantidade", 1), "MECANICO",
                String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post(RESERVAS, reserva(UUID.randomUUID(), vela.id(), 1), "OUTRO", String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getStatus(PECAS, "MECANICO")).isEqualTo(HttpStatus.OK);
        assertThat(detalhar(vela.id()).quantidadeReservada()).isZero();
    }

    @Test
    void gravacaoConcorrenteDaMesmaPecaFalhaComLockOtimista() {
        UUID id = cadastrar("Filtro de ar", 5).id();
        TransactionTemplate externa = new TransactionTemplate(transacoes);
        TransactionTemplate concorrente = new TransactionTemplate(transacoes);
        concorrente.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> externa.executeWithoutResult(status -> {
            Peca lida = pecas.buscarPorId(id).orElseThrow();
            concorrente.executeWithoutResult(outra -> {
                Peca peca = pecas.buscarPorId(id).orElseThrow();
                peca.reabastecer(1);
                pecas.salvar(peca);
            });
            lida.reabastecer(10);
            pecas.salvar(lida);
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(detalhar(id).quantidadeDisponivel()).isEqualTo(6);
    }

    @Test
    void swaggerDocumentaPecasEEstoque() {
        ResponseEntity<String> docs = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(docs.getBody()).contains(PECAS, PECAS + "/{id}", PECAS + "/{id}/entradas", RESERVAS,
                "/api/estoque/ordens-de-servico/{ordemDeServicoId}/baixa", "Pecas e insumos", "Estoque da OS");
    }

    private PecaResponse cadastrar(String nome, int quantidade) {
        ResponseEntity<PecaResponse> criada = post(PECAS, novaPeca(nome, quantidade), "ADMINISTRADOR",
                PecaResponse.class);
        assertThat(criada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criada.getBody()).isNotNull();
        assertThat(criada.getHeaders().getLocation()).hasToString(PECAS + "/" + criada.getBody().id());
        return criada.getBody();
    }

    private PecaResponse detalhar(UUID id) {
        return restTemplate.exchange(PECAS + "/" + id, HttpMethod.GET, new HttpEntity<>(headers("ATENDENTE")),
                PecaResponse.class).getBody();
    }

    private HttpStatus getStatus(String path, String papel) {
        return HttpStatus.valueOf(restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers(papel)),
                String.class).getStatusCode().value());
    }

    private <T> ResponseEntity<T> post(String path, Object corpo, String papel, Class<T> tipo) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(corpo, headers(papel)), tipo);
    }

    private static Map<String, Object> novaPeca(String nome, int quantidade) {
        return Map.of("nome", nome, "descricao", "Item de teste", "tipo", TipoItemEstoque.PECA.name(),
                "precoUnitario", new BigDecimal("35.90"), "quantidadeInicial", quantidade);
    }

    private static Map<String, Object> reserva(UUID os, UUID pecaId, int quantidade) {
        return Map.of("ordemDeServicoId", os, "pecaId", pecaId, "quantidade", quantidade);
    }

    private static String baixa(UUID os) {
        return "/api/estoque/ordens-de-servico/" + os + "/baixa";
    }

    private static HttpHeaders headers(String papel) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestSupport.token(papel));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /** Guarda os eventos publicados nas threads das requisicoes (o servidor roda fora da thread do teste). */
    static final class EventosRegistrados {
        private final List<EventoDeDominio> recebidos = new CopyOnWriteArrayList<>();

        @EventListener
        void registrar(EventoDeDominio evento) { recebidos.add(evento); }

        <T extends EventoDeDominio> Stream<T> stream(Class<T> tipo) {
            return recebidos.stream().filter(tipo::isInstance).map(tipo::cast);
        }

        void limpar() { recebidos.clear(); }
    }

    @TestConfiguration
    static class EventosConfig {
        @Bean
        EventosRegistrados eventosRegistrados() { return new EventosRegistrados(); }
    }
}
