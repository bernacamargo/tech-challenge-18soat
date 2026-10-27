package br.com.grupo117.oficina;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Documentacao de seguranca no /v3/api-docs (T23, D9, D14): o esquema bearer-jwt ensina a obter o
 * token no Keycloak sem senha e sem scope=openid, e toda operacao protegida documenta 401 e 403.
 */
class OpenApiSegurancaIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_TOKEN = "http://localhost:8081/realms/oficina/protocol/openid-connect/token";
    private static final String ACOMPANHAMENTO = "/api/acompanhamento/ordens-de-servico/{id}";

    @Autowired
    ApplicationContext applicationContext;

    private JsonNode docs() throws Exception {
        ResponseEntity<String> resposta = restTemplate.getForEntity("/v3/api-docs", String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return new ObjectMapper().readTree(resposta.getBody());
    }

    private String descricaoBearer() throws Exception {
        return docs().path("components").path("securitySchemes").path("bearer-jwt").path("description").asText();
    }

    private static Set<String> senhasDoRealm() throws Exception {
        JsonNode realm = new ObjectMapper().readTree(Path.of("keycloak/realm-oficina.json").toFile());
        Set<String> senhas = new HashSet<>();
        for (JsonNode usuario : realm.path("users")) {
            for (JsonNode credencial : usuario.path("credentials")) {
                String valor = credencial.path("value").asText();
                if (!valor.isBlank()) {
                    senhas.add(valor);
                }
            }
        }
        return senhas;
    }

    private static boolean semSecurity(JsonNode operacao) {
        JsonNode security = operacao.path("security");
        return security.isMissingNode() || security.isEmpty();
    }

    @Test
    void apiDocsEhPublico() {
        ResponseEntity<String> resposta = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void esquemaBearerJwtEhHttpBearerJwt() throws Exception {
        JsonNode esquema = docs().path("components").path("securitySchemes").path("bearer-jwt");

        assertThat(esquema.path("type").asText()).isEqualTo("http");
        assertThat(esquema.path("scheme").asText()).isEqualTo("bearer");
        assertThat(esquema.path("bearerFormat").asText()).isEqualTo("JWT");
    }

    @Test
    void unicoEsquemaDeSegurancaEhBearerJwt() throws Exception {
        List<String> nomes = new ArrayList<>();
        docs().path("components").path("securitySchemes").fieldNames().forEachRemaining(nomes::add);

        assertThat(nomes).containsExactly("bearer-jwt");
    }

    @Test
    void descricaoEnsinaAObterOTokenNoKeycloak() throws Exception {
        String descricao = descricaoBearer();

        assertThat(descricao).contains(URL_TOKEN, "grant_type=password", "client_id=oficina-api",
                "atendente", "mecanico", "administrador", "Authorize",
                "Credenciais de demonstração (somente local)");
    }

    @Test
    void descricaoNaoTemSenhaNemScopeOpenid() throws Exception {
        String descricao = descricaoBearer();
        Set<String> senhas = senhasDoRealm();

        assertThat(descricao).doesNotContain("scope=openid", "demo123", "eyJ");
        assertThat(senhas).as("senhas lidas do realm").isNotEmpty();
        for (String senha : senhas) {
            assertThat(descricao.contains(senha)).as("descricao contem senha do realm").isFalse();
        }
    }

    @Test
    void operacaoProtegidaDocumenta401e403() throws Exception {
        JsonNode respostas = docs().path("paths").path("/api/clientes").path("get").path("responses");

        assertThat(respostas.path("401").path("description").asText()).isEqualTo("Autenticação necessária");
        assertThat(respostas.path("403").path("description").asText()).isEqualTo("Perfil sem permissão");
    }

    @Test
    void todaOperacaoProtegidaDocumenta401e403() throws Exception {
        int protegidas = 0;
        Iterator<Map.Entry<String, JsonNode>> caminhos = docs().path("paths").fields();
        while (caminhos.hasNext()) {
            Map.Entry<String, JsonNode> caminho = caminhos.next();
            Iterator<Map.Entry<String, JsonNode>> operacoes = caminho.getValue().fields();
            while (operacoes.hasNext()) {
                Map.Entry<String, JsonNode> operacao = operacoes.next();
                if (semSecurity(operacao.getValue())) {
                    continue;
                }
                protegidas++;
                String rotulo = operacao.getKey() + " " + caminho.getKey();
                assertThat(operacao.getValue().path("responses").has("401")).as(rotulo + " documenta 401").isTrue();
                assertThat(operacao.getValue().path("responses").has("403")).as(rotulo + " documenta 403").isTrue();
            }
        }

        assertThat(protegidas).isGreaterThanOrEqualTo(10);
    }

    @Test
    void respostaDeSegurancaJaDeclaradaNaoEhSobrescrita() {
        OpenApiCustomizer customizer = applicationContext.getBean("respostasDeSeguranca", OpenApiCustomizer.class);
        Operation operacao = new Operation()
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .responses(new ApiResponses()
                        .addApiResponse("401", new ApiResponse().description("declarado-401"))
                        .addApiResponse("403", new ApiResponse().description("declarado-403")));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/x", new PathItem().get(operacao)));

        customizer.customise(openApi);

        ApiResponses respostas = openApi.getPaths().get("/x").getGet().getResponses();
        assertThat(respostas.get("401").getDescription()).isEqualTo("declarado-401");
        assertThat(respostas.get("403").getDescription()).isEqualTo("declarado-403");
    }

    @Test
    void rotaPublicaDeAcompanhamentoSemCadeadoNemRespostasDeSeguranca() throws Exception {
        JsonNode paths = docs().path("paths");
        List<JsonNode> operacoes = List.of(
                paths.path(ACOMPANHAMENTO).path("get"),
                paths.path(ACOMPANHAMENTO + "/aprovacao").path("post"),
                paths.path(ACOMPANHAMENTO + "/rejeicao").path("post"));

        for (JsonNode operacao : operacoes) {
            assertThat(operacao.isMissingNode()).as("operacao publica presente no doc").isFalse();
            assertThat(semSecurity(operacao)).as("operacao publica sem security").isTrue();
            assertThat(operacao.path("responses").has("401")).as("operacao publica sem 401").isFalse();
            assertThat(operacao.path("responses").has("403")).as("operacao publica sem 403").isFalse();
        }
    }

    @Test
    void healthSemCadeadoNemRespostasDeSeguranca() throws Exception {
        JsonNode operacao = docs().path("paths").path("/api/health").path("get");

        assertThat(operacao.isMissingNode()).as("health presente no doc").isFalse();
        assertThat(semSecurity(operacao)).as("health sem security").isTrue();
        assertThat(operacao.path("responses").has("401")).as("health sem 401").isFalse();
        assertThat(operacao.path("responses").has("403")).as("health sem 403").isFalse();
    }

    @Test
    void swaggerUiNaoPersisteAutorizacao() throws Exception {
        ResponseEntity<String> resposta = restTemplate.getForEntity("/v3/api-docs/swagger-config", String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode config = new ObjectMapper().readTree(resposta.getBody());
        assertThat(config.path("persistAuthorization").asBoolean(false)).isFalse();
    }
}
