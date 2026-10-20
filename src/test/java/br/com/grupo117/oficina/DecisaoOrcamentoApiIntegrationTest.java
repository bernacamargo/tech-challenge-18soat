package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import br.com.grupo117.oficina.atendimento.infrastructure.api.OrdemDeServicoRequest;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.*;

class DecisaoOrcamentoApiIntegrationTest extends AbstractIntegrationTest {
    private static final String PATH = "/api/ordens-de-servico";
    private static final Instant MOMENTO = Instant.parse("2026-10-20T12:00:00.123456Z");
    @Autowired private OrdemDeServicoService service;
    @Autowired private OrdemDeServicoRepositorio ordens;

    @ParameterizedTest
    @ValueSource(strings = {"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    void detalheAdministrativoMostraInicioPersistido(String papel) {
        OrdemDeServico os = orcada();
        os.aprovarOrcamento(MOMENTO);
        ordens.salvar(os);
        var resposta = consultar(PATH + "/" + os.id(), papel);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode corpo = resposta.getBody();
        assertThat(corpo.path("status").asText()).isEqualTo("EM_EXECUCAO");
        assertThat(corpo.path("iniciadaEm").asText()).isEqualTo(MOMENTO.toString());
        assertThat(corpo.get("rejeicaoOrcamento").isNull()).isTrue();
        assertThat(corpo.path("itens").size()).isEqualTo(1);
        assertThat(corpo.path("orcamento").path("total").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(consultar(PATH, papel).getBody().get(0)).isEqualTo(corpo);
        verificarConsultaPublica(os.id(), "EM_EXECUCAO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATENDENTE", "MECANICO", "ADMINISTRADOR"})
    void detalheMostraDataEMotivoDaRejeicaoSemExporNaConsultaPublica(String papel) {
        OrdemDeServico os = orcada();
        os.rejeitarOrcamento(MOMENTO, "  Preço acima do esperado  ");
        ordens.salvar(os);
        var resposta = consultar(PATH + "/" + os.id(), papel);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode corpo = resposta.getBody();
        assertThat(corpo.path("status").asText()).isEqualTo("AGUARDANDO_APROVACAO");
        assertThat(corpo.get("iniciadaEm").isNull()).isTrue();
        assertThat(corpo.path("rejeicaoOrcamento").path("momento").asText()).isEqualTo(MOMENTO.toString());
        assertThat(corpo.path("rejeicaoOrcamento").path("motivo").asText()).isEqualTo("Preço acima do esperado");
        assertThat(consultar(PATH, papel).getBody().get(0)).isEqualTo(corpo);
        verificarConsultaPublica(os.id(), "AGUARDANDO_APROVACAO");
    }

    @Test
    void criacaoRetornaNovosCamposNulos() {
        var request = new OrdemDeServicoRequest("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
        var resposta = restTemplate.exchange(PATH, HttpMethod.POST,
                new HttpEntity<>(request, headers("ATENDENTE")), JsonNode.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody().get("iniciadaEm").isNull()).isTrue();
        assertThat(resposta.getBody().get("rejeicaoOrcamento").isNull()).isTrue();
    }

    @Test
    void osLegadaEntregueNaoRecebeInicioFicticioNaResposta() {
        OrdemDeServico criada = service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
        ordens.salvar(OrdemDeServico.restaurar(criada.id(), criada.clienteId(), criada.veiculoId(),
                StatusOS.ENTREGUE, MOMENTO));
        JsonNode corpo = consultar(PATH + "/" + criada.id(), "ADMINISTRADOR").getBody();
        assertThat(corpo.path("status").asText()).isEqualTo("ENTREGUE");
        assertThat(corpo.get("iniciadaEm").isNull()).isTrue();
        assertThat(corpo.get("rejeicaoOrcamento").isNull()).isTrue();
        assertThat(corpo.path("finalizadaEm").asText()).isEqualTo(MOMENTO.toString());
    }

    @Test
    void preservaInicioNaRespostaAposConclusaoEEntrega() {
        OrdemDeServico os = orcada();
        os.aprovarOrcamento(MOMENTO);
        os.transicionarPara(StatusOS.FINALIZADA, MOMENTO.plusSeconds(3600));
        os.transicionarPara(StatusOS.ENTREGUE, MOMENTO.plusSeconds(7200));
        ordens.salvar(os);
        JsonNode corpo = consultar(PATH + "/" + os.id(), "ADMINISTRADOR").getBody();
        assertThat(corpo.path("iniciadaEm").asText()).isEqualTo(MOMENTO.toString());
        assertThat(corpo.path("finalizadaEm").asText()).isEqualTo(MOMENTO.plusSeconds(3600).toString());
        verificarConsultaPublica(os.id(), "ENTREGUE");
    }

    @Test
    void dadosDaRejeicaoExigemAutenticacaoEPapelAdministrativoExistente() {
        OrdemDeServico os = orcada();
        os.rejeitarOrcamento(MOMENTO, "Custo");
        ordens.salvar(os);
        for (String caminho : List.of(PATH, PATH + "/" + os.id())) {
            assertThat(consultar(caminho, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(consultar(caminho, "CLIENTE").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void swaggerDocumentaCamposAdministrativosSemAmpliarSchemaPublico() {
        JsonNode docs = restTemplate.getForObject("/v3/api-docs", JsonNode.class);
        JsonNode schemas = docs.path("components").path("schemas");
        JsonNode os = schemas.path("OrdemDeServicoResponse").path("properties");
        assertThat(os.path("iniciadaEm").path("format").asText()).isEqualTo("date-time");
        assertThat(os.path("iniciadaEm").path("description").asText()).contains("legados");
        assertThat(os.has("rejeicaoOrcamento")).isTrue();
        JsonNode rejeicao = schemas.path("RejeicaoOrcamentoResponse").path("properties");
        assertThat(rejeicao.path("momento").path("format").asText()).isEqualTo("date-time");
        assertThat(rejeicao.has("motivo")).isTrue();
        assertThat(campos(schemas.path("AcompanhamentoOrdemDeServicoResponse").path("properties")))
                .containsExactlyInAnyOrder("status", "finalizadaEm");
    }

    private void verificarConsultaPublica(UUID id, String status) {
        JsonNode publico = restTemplate.getForObject("/api/acompanhamento/ordens-de-servico/" + id, JsonNode.class);
        assertThat(campos(publico)).containsExactlyInAnyOrder("status", "finalizadaEm");
        assertThat(publico.path("status").asText()).isEqualTo(status);
    }

    private List<String> campos(JsonNode objeto) {
        List<String> campos = new ArrayList<>();
        objeto.fieldNames().forEachRemaining(campos::add);
        return campos;
    }

    private OrdemDeServico orcada() {
        OrdemDeServico os = service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
        os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Diagnóstico", 1, new Dinheiro(new BigDecimal("50.00"))));
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        os.gerarOrcamento();
        return ordens.salvar(os);
    }

    private ResponseEntity<JsonNode> consultar(String caminho, String papel) {
        return restTemplate.exchange(caminho, HttpMethod.GET, new HttpEntity<>(headers(papel)), JsonNode.class);
    }

    private HttpHeaders headers(String papel) {
        HttpHeaders headers = new HttpHeaders();
        if (papel != null) headers.setBearerAuth(JwtTestSupport.token(papel));
        return headers;
    }
}
