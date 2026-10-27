package br.com.grupo117.oficina;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.grupo117.oficina.infra.security.JwksTestServer;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Valida o decoder de producao do Boot (propriedades do application.yml): so o jwk-set-uri vem do teste;
 * issuer, audience e algoritmo sao os do YAML de producao.
 */
class ValidacaoTokenJwtIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ApplicationContext contexto;

    static Stream<Arguments> tokensInvalidos() {
        return Stream.of(
                caso("malformado", () -> "abc"),
                caso("expirado-120s", JwtTestSupport::tokenExpirado),
                caso("nbf+120s", JwtTestSupport::tokenNaoAntesDe),
                caso("iss-interno-compose", () -> JwtTestSupport.tokenComIssuer("http://keycloak:8080/realms/oficina")),
                caso("iss-realm-master", () -> JwtTestSupport.tokenComIssuer("http://localhost:8081/realms/master")),
                caso("aud-account", () -> JwtTestSupport.tokenComAudience("account")),
                caso("aud-ausente", JwtTestSupport::tokenSemAudience),
                caso("outra-chave-mesmo-kid", JwtTestSupport::tokenAssinadoComOutraChave),
                caso("kid-desconhecido", JwtTestSupport::tokenComKidDesconhecido),
                caso("hs256-segredo-antigo", () -> JwtTestSupport.tokenHs256("ATENDENTE")),
                caso("alg-none", JwtTestSupport::tokenAlgNone));
    }

    private static Arguments caso(String nome, Supplier<String> token) {
        return Arguments.of(nome, token);
    }

    @Test
    void tokenRs256ValidoDeAtendenteAcessaClientes() {
        ResponseEntity<String> resposta = getClientes(JwtTestSupport.tokenRs256("ATENDENTE"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void semHeaderDa401ComDesafioBearer() {
        ResponseEntity<String> resposta = getClientes(null);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer");
        assertThat(resposta.getBody()).isNullOrEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tokensInvalidos")
    void tokenInvalidoDa401(String caso, Supplier<String> token) {
        ResponseEntity<String> resposta = getClientes(token.get());

        assertThat(resposta.getStatusCode()).as(caso).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).as(caso).isNullOrEmpty();
    }

    @Test
    void audienceEmListaComOficinaApiDa200() {
        ResponseEntity<String> resposta = getClientes(JwtTestSupport.tokenComAudience(List.of("account", "oficina-api")));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void audienceComoTextoUnicoDa200() {
        ResponseEntity<String> resposta = getClientes(JwtTestSupport.tokenComAudience("oficina-api"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unicoDecoderEhODaAutoconfiguracaoDoBoot() {
        assertThat(contexto.getBeanNamesForType(JwtDecoder.class)).containsExactly("jwtDecoderByJwkKeySetUri");
    }

    @Test
    void decoderLeOJwksPorJwkSetUri() {
        int antes = JwksTestServer.requisicoes();

        ResponseEntity<String> resposta = getClientes(JwtTestSupport.tokenComKidDesconhecido());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(JwksTestServer.requisicoes()).isGreaterThan(antes);
    }

    @Test
    void tokenSemExpiracaoDa401() {
        ResponseEntity<String> resposta = getClientes(JwtTestSupport.tokenSemExp());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).isNullOrEmpty();
    }

    @Test
    void idTokenDoKeycloakDa401() {
        ResponseEntity<String> resposta = getClientes(JwtTestSupport.tokenDeId());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).isNullOrEmpty();
    }

    @Test
    void tokenSemTypDa401() {
        ResponseEntity<String> resposta =
                getClientes(JwtTestSupport.tokenRs256(b -> b.claims(c -> c.remove("typ")), "ATENDENTE"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void typComCaixaDiferenteDa401() {
        ResponseEntity<String> resposta = getClientes(JwtTestSupport.tokenRs256(b -> b.claim("typ", "bearer"), "ATENDENTE"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenDeAcessoComTypBearerEExpDa200() {
        ResponseEntity<String> resposta = getClientes(JwtTestSupport.tokenRs256("ATENDENTE"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<String> getClientes(String tokenOuNull) {
        HttpHeaders headers = new HttpHeaders();
        if (tokenOuNull != null) {
            headers.setBearerAuth(tokenOuNull);
        }
        return restTemplate.exchange("/api/clientes", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
