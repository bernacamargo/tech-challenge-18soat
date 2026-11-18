package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import br.com.grupo117.oficina.atendimento.infrastructure.api.OrdemDeServicoResponse;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ListagemOrdemDeServicoIntegrationTest extends AbstractIntegrationTest {
    private static final String PATH = "/api/ordens-de-servico";
    private static final Instant ABERTURA = Instant.parse("2026-11-01T12:00:00Z");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private OrdemDeServicoRepositorio ordens;

    @Test
    void listaPorStatusAntiguidadeELegadoPreservandoHistorico() {
        cadastrarClienteEVeiculo();
        List<OrdemDeServico> esperadas = new ArrayList<>();
        long id = 1;
        for (StatusOS status : List.of(StatusOS.EM_EXECUCAO, StatusOS.AGUARDANDO_APROVACAO,
                StatusOS.EM_DIAGNOSTICO, StatusOS.RECEBIDA)) {
            for (Instant abertura : java.util.Arrays.asList(null, ABERTURA, ABERTURA, ABERTURA.plusSeconds(60))) {
                esperadas.add(restaurar(id++, status, abertura));
            }
        }
        OrdemDeServico finalizada = restaurar(id++, StatusOS.FINALIZADA, ABERTURA);
        OrdemDeServico entregue = restaurar(id, StatusOS.ENTREGUE, ABERTURA);
        List<OrdemDeServico> inversas = new ArrayList<>(esperadas);
        Collections.reverse(inversas);
        inversas.forEach(ordens::salvar);
        ordens.salvar(finalizada);
        ordens.salvar(entregue);

        for (String perfil : List.of("ATENDENTE", "MECANICO", "ADMINISTRADOR")) {
            var resposta = restTemplate.exchange(PATH, HttpMethod.GET,
                    new HttpEntity<>(headers(perfil)), OrdemDeServicoResponse[].class);
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resposta.getBody()).extracting(OrdemDeServicoResponse::id)
                    .containsExactlyElementsOf(esperadas.stream().map(OrdemDeServico::id).toList());
            assertThat(resposta.getBody()[0].abertaEm()).isNull();
            assertThat(resposta.getBody()[1].abertaEm()).isEqualTo(ABERTURA);
        }

        assertThat(ordens.listar()).hasSize(18);
        for (OrdemDeServico os : List.of(finalizada, entregue)) {
            var detalhe = restTemplate.exchange(PATH + "/" + os.id(), HttpMethod.GET,
                    new HttpEntity<>(headers("ATENDENTE")), OrdemDeServicoResponse.class);
            assertThat(detalhe.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(detalhe.getBody().status()).isEqualTo(os.status());
            var publico = restTemplate.getForObject("/api/acompanhamento/ordens-de-servico/" + os.id(), JsonNode.class);
            assertThat(publico.path("status").asText()).isEqualTo(os.status().name());
            assertThat(publico.has("abertaEm")).isFalse();
        }
    }

    @Test
    void aberturaPersisteUmaVezEPreservaNaAtualizacao() {
        cadastrarClienteEVeiculo();
        Instant nanos = Instant.parse("2026-11-01T12:00:00.123456789Z");
        OrdemDeServico os = ordens.salvar(new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23", nanos));
        Instant micros = Instant.parse("2026-11-01T12:00:00.123456Z");
        assertThat(os.abertaEm()).contains(micros);
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        ordens.salvar(os);
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().abertaEm()).contains(micros);
    }

    @Test
    void listaVaziaQuandoSomenteHaConcluidas() {
        cadastrarClienteEVeiculo();
        ordens.salvar(restaurar(1, StatusOS.FINALIZADA, ABERTURA));
        ordens.salvar(restaurar(2, StatusOS.ENTREGUE, null));
        var resposta = restTemplate.exchange(PATH, HttpMethod.GET,
                new HttpEntity<>(headers("ATENDENTE")), OrdemDeServicoResponse[].class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isEmpty();
        assertThat(ordens.listar()).hasSize(2);
    }

    private void cadastrarClienteEVeiculo() {
        jdbc.update("insert into cliente (cpf_cnpj, nome) values ('52998224725', 'Maria')");
        jdbc.update("insert into veiculo (placa, marca, modelo, ano, cpf_cnpj_cliente) values ('ABC1D23', 'Fiat', 'Uno', 2012, '52998224725')");
    }

    private static OrdemDeServico restaurar(long id, StatusOS status, Instant abertura) {
        Instant fim = status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE ? ABERTURA.plusSeconds(3600) : null;
        return OrdemDeServico.restaurar(new UUID(0, id), "52998224725", "ABC1D23", status, fim,
                List.of(), null, null, null, abertura);
    }

    private static HttpHeaders headers(String perfil) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestSupport.token(perfil));
        return headers;
    }
}
