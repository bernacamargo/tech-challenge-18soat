package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.assertThat;

class AcompanhamentoOrdemDeServicoIntegrationTest extends AbstractIntegrationTest {
    private static final String PATH = "/api/acompanhamento/ordens-de-servico/";
    @Autowired
    private OrdemDeServicoService service;
    @Autowired
    private OrdemDeServicoRepositorio ordens;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void consultaSemTokenRetornaSomenteAndamento() {
        OrdemDeServico os = criar();
        ResponseEntity<JsonNode> resposta = restTemplate.getForEntity(PATH + os.id(), JsonNode.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode corpo = resposta.getBody();
        assertThat(corpo).isNotNull();
        List<String> campos = new ArrayList<>();
        corpo.fieldNames().forEachRemaining(campos::add);
        assertThat(campos).containsExactlyInAnyOrder("status", "finalizadaEm");
        assertThat(corpo.get("status").asText()).isEqualTo("RECEBIDA");
        assertThat(corpo.get("finalizadaEm").isNull()).isTrue();
    }

    @Test
    void consultaMostraEstadoAtualPersistido() {
        OrdemDeServico criada = criar();
        Instant momento = Instant.parse("2026-10-12T18:00:00Z");
        ordens.salvar(OrdemDeServico.restaurar(criada.id(), criada.clienteId(), criada.veiculoId(),
                StatusOS.ENTREGUE, momento));

        ResponseEntity<JsonNode> resposta = restTemplate.getForEntity(PATH + criada.id(), JsonNode.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().get("status").asText()).isEqualTo("ENTREGUE");
        assertThat(resposta.getBody().get("finalizadaEm").asText()).isEqualTo(momento.toString());
    }

    @Test
    void identificadorAusenteOuInvalidoNaoRetornaOs() {
        assertThat(restTemplate.getForEntity(PATH + UUID.randomUUID(), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.getForEntity(PATH + "invalido", String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rotaPublicaNaoLiberaAdministracaoNemEscritaGenerica() {
        OrdemDeServico os = criar();
        assertThat(restTemplate.postForEntity(PATH + os.id(), null, String.class).getStatusCode())
                .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        assertThat(restTemplate.getForEntity("/api/ordens-de-servico/" + os.id(), String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/api/ordens-de-servico", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void swaggerMostraConsultaSemAutenticacao() throws Exception {
        String docs = restTemplate.getForObject("/v3/api-docs", String.class);
        JsonNode operacao = mapper.readTree(docs).path("paths").path(PATH + "{id}").path("get");
        assertThat(operacao.isMissingNode()).isFalse();
        assertThat(operacao.path("security").size()).isZero();
    }

    private OrdemDeServico criar() {
        return service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
    }
}
