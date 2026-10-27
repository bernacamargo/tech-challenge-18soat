package br.com.grupo117.oficina;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Guardas transversais de seguranca (D7, D12b): o token e o header Authorization nao vao para o
 * log nem com o Security em TRACE; 401/403 nao ecoam a credencial nem dizem qual perfil falta;
 * toda rota de controller da oficina sem token responde 401, salvo as publicas do SecurityConfig.
 * O TRACE e ligado por metodo e restaurado depois (sem @TestPropertySource: o Logback e global
 * na JVM e o contexto em cache seria reutilizado pelas outras classes de IT).
 */
@ExtendWith(OutputCaptureExtension.class)
class SegurancaTransversalIntegrationTest extends AbstractIntegrationTest {

    static final List<String> LOGGERS_TRACE = List.of(
            "org.springframework.security", "br.com.grupo117.oficina.identidadeacesso");

    // "METODO padrao": espelho dos permitAll de SecurityConfig.java:36-40 com controller da oficina
    static final Set<String> ROTAS_PUBLICAS = Set.of(
            "GET /api/health",
            "GET /api/acompanhamento/ordens-de-servico/{id}",
            "POST /api/acompanhamento/ordens-de-servico/{id}/aprovacao",
            "POST /api/acompanhamento/ordens-de-servico/{id}/rejeicao");

    private static final List<String> TERMOS_DE_PERFIL = List.of(
            "ATENDENTE", "MECANICO", "ADMINISTRADOR", "ROLE_", "hasRole", "hasAnyRole", "default-roles", "usuario-teste");

    private final LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    private final Map<String, LogLevel> niveisAnteriores = new HashMap<>();

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mapeamentos;

    @BeforeEach
    void ligaTrace() {
        for (String nome : LOGGERS_TRACE) {
            LoggerConfiguration configuracao = logging.getLoggerConfiguration(nome);
            niveisAnteriores.put(nome, configuracao == null ? null : configuracao.getConfiguredLevel());
            logging.setLogLevel(nome, LogLevel.TRACE);
        }
    }

    @AfterEach
    void restauraNiveis() {
        for (String nome : LOGGERS_TRACE) {
            logging.setLogLevel(nome, niveisAnteriores.get(nome));
        }
    }

    // S1
    @Test
    void tokenNaoApareceNoLog(CapturedOutput output) {
        List<String> tokens = List.of(
                JwtTestSupport.token("ATENDENTE"),
                JwtTestSupport.tokenAssinadoComOutraChave(),
                JwtTestSupport.tokenExpirado(),
                JwtTestSupport.tokenSemPerfilDaOficina(),
                "a.b.c");
        List<HttpStatus> esperados = List.of(
                HttpStatus.OK, HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);

        for (int i = 0; i < tokens.size(); i++) {
            assertThat(get("/api/clientes", tokens.get(i)).getStatusCode()).as("requisicao %d", i).isEqualTo(esperados.get(i));
        }

        String saida = output.getAll();
        assertThat(saida).contains("Securing GET /api/clientes");
        for (int i = 0; i < tokens.size(); i++) {
            String[] segmentos = tokens.get(i).split("\\.");
            assertThat(saida).as("token %d", i).doesNotContain(tokens.get(i));
            for (int s = 1; s <= 2 && s < segmentos.length; s++) {
                if (segmentos[s].length() >= 20) {
                    assertThat(saida).as("token %d segmento %d", i, s).doesNotContain(segmentos[s]);
                }
            }
        }
        assertThat(saida).doesNotContainIgnoringCase("authorization: bearer");
        assertThat(saida).doesNotContainPattern("eyJ[A-Za-z0-9_-]{10,}\\.eyJ");
    }

    // S2
    @Test
    void respostaDe401NaoEcoaCredencial() {
        for (String token : List.of(JwtTestSupport.tokenAssinadoComOutraChave(), JwtTestSupport.tokenExpirado())) {
            ResponseEntity<String> resposta = get("/api/clientes", token);
            String[] segmentos = token.split("\\.");

            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            String headers = resposta.getHeaders().toString();
            String corpo = resposta.getBody() == null ? "" : resposta.getBody();
            assertThat(corpo).isEmpty();
            for (String texto : List.of(headers, corpo)) {
                assertThat(texto).doesNotContain(token).doesNotContain(segmentos[1]).doesNotContain(segmentos[2]);
            }
            assertThat(resposta.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer");
        }
    }

    // S3
    @Test
    void respostaDe403NaoDetalhaPerfilExigido() {
        List<ResponseEntity<String>> respostas = List.of(
                get("/api/clientes", JwtTestSupport.token("MECANICO")),
                restTemplate.exchange("/api/pecas/" + UUID.randomUUID(), HttpMethod.DELETE,
                        new HttpEntity<>(bearer(JwtTestSupport.token("ATENDENTE"))), String.class),
                get("/api/ordens-de-servico", JwtTestSupport.tokenSemPerfilDaOficina()));
        List<String> casos = List.of("MECANICO GET /api/clientes", "ATENDENTE DELETE /api/pecas/{id}",
                "sem perfil GET /api/ordens-de-servico");

        for (int i = 0; i < respostas.size(); i++) {
            ResponseEntity<String> resposta = respostas.get(i);
            assertThat(resposta.getStatusCode()).as(casos.get(i)).isEqualTo(HttpStatus.FORBIDDEN);
            List<String> textos = new ArrayList<>();
            resposta.getHeaders().forEach((nome, valores) -> textos.addAll(valores));
            textos.add(resposta.getBody() == null ? "" : resposta.getBody());
            for (String texto : textos) {
                for (String termo : TERMOS_DE_PERFIL) {
                    assertThat(texto).as("%s: %s", casos.get(i), termo).doesNotContain(termo);
                }
            }
        }
    }

    // S4
    @Test
    void rotasNaoPublicasExigemToken() {
        Set<String> rotas = new TreeSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entrada : mapeamentos.getHandlerMethods().entrySet()) {
            if (!entrada.getValue().getBeanType().getPackageName().startsWith("br.com.grupo117.oficina")) {
                continue;
            }
            RequestMappingInfo info = entrada.getKey();
            Set<RequestMethod> metodos = info.getMethodsCondition().getMethods();
            for (String padrao : info.getPathPatternsCondition().getPatternValues()) {
                if (metodos.isEmpty()) {
                    rotas.add("GET " + padrao);
                }
                for (RequestMethod metodo : metodos) {
                    rotas.add(metodo.name() + " " + padrao);
                }
            }
        }

        assertThat(rotas).hasSizeGreaterThanOrEqualTo(30);
        assertThat(rotas).containsAll(ROTAS_PUBLICAS);

        Set<String> rotasQueNaoDeram401 = new TreeSet<>();
        for (String rota : rotas) {
            String metodo = rota.substring(0, rota.indexOf(' '));
            String padrao = rota.substring(rota.indexOf(' ') + 1);
            String url = padrao.replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
            HttpStatusCode status = restTemplate.exchange(url, HttpMethod.valueOf(metodo), HttpEntity.EMPTY, String.class)
                    .getStatusCode();

            if (ROTAS_PUBLICAS.contains(rota)) {
                assertThat(status).as("%s -> %s", rota, status).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
            } else {
                assertThat(status).as("%s -> %s", rota, status).isEqualTo(HttpStatus.UNAUTHORIZED);
            }
            if (!HttpStatus.UNAUTHORIZED.equals(status)) {
                rotasQueNaoDeram401.add(rota);
            }
        }
        assertThat(rotasQueNaoDeram401).isEqualTo(ROTAS_PUBLICAS);
    }

    private ResponseEntity<String> get(String caminho, String token) {
        return restTemplate.exchange(caminho, HttpMethod.GET, new HttpEntity<>(bearer(token)), String.class);
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
