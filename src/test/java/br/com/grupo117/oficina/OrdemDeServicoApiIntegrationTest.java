package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import br.com.grupo117.oficina.atendimento.infrastructure.api.OrdemDeServicoRequest;
import br.com.grupo117.oficina.atendimento.infrastructure.api.OrdemDeServicoResponse;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class OrdemDeServicoApiIntegrationTest extends AbstractIntegrationTest {
    private static final String PATH = "/api/ordens-de-servico";
    @Autowired
    private OrdemDeServicoRepositorio ordens;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void criaListaEDetalhaOsPersistida() {
        ResponseEntity<OrdemDeServicoResponse> criada = criar("ATENDENTE", request());
        assertThat(criada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        OrdemDeServicoResponse os = criada.getBody();
        assertThat(os).isNotNull();
        assertThat(os.status()).isEqualTo(StatusOS.RECEBIDA);
        assertThat(os.clienteId()).isEqualTo("52998224725");
        assertThat(os.veiculoId()).isEqualTo("ABC1D23");
        assertThat(criada.getHeaders().getLocation()).hasToString(PATH + "/" + os.id());

        ResponseEntity<OrdemDeServicoResponse[]> lista = restTemplate.exchange(PATH, HttpMethod.GET,
                new HttpEntity<>(headers("MECANICO")), OrdemDeServicoResponse[].class);
        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).containsExactly(os);
        ResponseEntity<OrdemDeServicoResponse> detalhe = restTemplate.exchange(PATH + "/" + os.id(),
                HttpMethod.GET, new HttpEntity<>(headers("ADMINISTRADOR")), OrdemDeServicoResponse.class);
        assertThat(detalhe.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detalhe.getBody()).isEqualTo(os);
        assertThat(ordens.buscarPorId(os.id())).isPresent();
    }

    @Test
    void preservaStatusEFinalizacaoNaPersistencia() {
        OrdemDeServicoResponse criada = criar("ADMINISTRADOR", request()).getBody();
        assertThat(criada).isNotNull();
        Instant momento = Instant.parse("2026-10-12T17:00:00Z");
        OrdemDeServico os = OrdemDeServico.restaurar(criada.id(), criada.clienteId(), criada.veiculoId(),
                StatusOS.ENTREGUE, momento);
        ordens.salvar(os);
        OrdemDeServico restaurada = ordens.buscarPorId(os.id()).orElseThrow();
        assertThat(restaurada.status()).isEqualTo(StatusOS.ENTREGUE);
        assertThat(restaurada.finalizadaEm()).contains(momento);
        ResponseEntity<OrdemDeServicoResponse> detalhe = restTemplate.exchange(PATH + "/" + os.id(),
                HttpMethod.GET, new HttpEntity<>(headers("ATENDENTE")), OrdemDeServicoResponse.class);
        assertThat(detalhe.getBody()).isNotNull();
        assertThat(detalhe.getBody().finalizadaEm()).isEqualTo(momento);
    }

    @Test
    void falhaNoVeiculoDesfazCadastroDoCliente() {
        OrdemDeServicoRequest invalida = new OrdemDeServicoRequest("52998224725", "Maria", "ABC1D23",
                "Fiat", "Uno", 1800);
        ResponseEntity<String> resposta = restTemplate.exchange(PATH, HttpMethod.POST,
                new HttpEntity<>(invalida, headers("ATENDENTE")), String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(jdbc.queryForObject("select count(*) from cliente", Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from veiculo", Long.class)).isZero();
        assertThat(ordens.listar()).isEmpty();
    }

    @Test
    void falhaNaGravacaoDaOsDesfazClienteEVeiculo() {
        // Impõe uma falha real no PostgreSQL depois dos dois cadastros.
        jdbc.execute("ALTER TABLE ordem_de_servico ADD CONSTRAINT ck_os_falha_teste CHECK (false)");
        try {
            ResponseEntity<String> resposta = restTemplate.exchange(PATH, HttpMethod.POST,
                    new HttpEntity<>(request(), headers("ATENDENTE")), String.class);
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(jdbc.queryForObject("select count(*) from cliente", Long.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from veiculo", Long.class)).isZero();
            assertThat(ordens.listar()).isEmpty();
        } finally {
            jdbc.execute("ALTER TABLE ordem_de_servico DROP CONSTRAINT ck_os_falha_teste");
        }
    }

    @Test
    void semTokenNaoCriaListaOuDetalha() {
        assertThat(restTemplate.getForEntity(PATH, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity(PATH + "/" + UUID.randomUUID(), String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.postForEntity(PATH, request(), String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void papelDesconhecidoNaoConsultaEMecanicoNaoCria() {
        ResponseEntity<String> consulta = restTemplate.exchange(PATH, HttpMethod.GET,
                new HttpEntity<>(headers("OUTRO")), String.class);
        assertThat(consulta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> criacao = restTemplate.exchange(PATH, HttpMethod.POST,
                new HttpEntity<>(request(), headers("MECANICO")), String.class);
        assertThat(criacao.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ordens.listar()).isEmpty();
    }

    @Test
    void osInexistenteRetorna404() {
        ResponseEntity<String> resposta = restTemplate.exchange(PATH + "/" + UUID.randomUUID(), HttpMethod.GET,
                new HttpEntity<>(headers("ATENDENTE")), String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).contains("Ordem de serviço não encontrada");
    }

    @Test
    void entradaInvalidaRetorna400() {
        OrdemDeServicoRequest invalida = new OrdemDeServicoRequest("52998224725", "", "ABC1D23", "Fiat", "Uno", 2012);
        ResponseEntity<String> resposta = restTemplate.exchange(PATH, HttpMethod.POST,
                new HttpEntity<>(invalida, headers("ATENDENTE")), String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseEntity<String> idInvalido = restTemplate.exchange(PATH + "/invalido", HttpMethod.GET,
                new HttpEntity<>(headers("ATENDENTE")), String.class);
        assertThat(idInvalido.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void swaggerDocumentaConsultasECriacao() {
        ResponseEntity<String> docs = restTemplate.getForEntity("/v3/api-docs", String.class);
        assertThat(docs.getBody()).contains(PATH, PATH + "/{id}", "bearer-jwt", "Ordens de serviço");
    }

    private ResponseEntity<OrdemDeServicoResponse> criar(String papel, OrdemDeServicoRequest request) {
        return restTemplate.exchange(PATH, HttpMethod.POST, new HttpEntity<>(request, headers(papel)),
                OrdemDeServicoResponse.class);
    }

    private static OrdemDeServicoRequest request() {
        return new OrdemDeServicoRequest("529.982.247-25", "Maria", "abc-1d23", "Fiat", "Uno", 2012);
    }

    private static HttpHeaders headers(String papel) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestSupport.token(papel));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
