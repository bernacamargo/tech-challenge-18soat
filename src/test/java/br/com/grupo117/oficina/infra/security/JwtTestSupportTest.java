package br.com.grupo117.oficina.infra.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class JwtTestSupportTest {

    private static final JwtDecoder DECODER = NimbusJwtDecoder.withJwkSetUri(JwksTestServer.uri()).build();

    @Test
    void jwksPublicaApenasChavePublica() throws Exception {
        String json = JwtTestSupport.jwksJson();
        JWKSet conjunto = JWKSet.parse(json);

        for (String membro : List.of("\"d\"", "\"p\"", "\"q\"", "\"dp\"", "\"dq\"", "\"qi\"")) {
            assertFalse(json.contains(membro), membro);
        }
        assertEquals(1, conjunto.getKeys().size());
        JWK chave = conjunto.getKeys().get(0);
        assertFalse(chave.isPrivate());
        assertEquals("teste-oficina", chave.getKeyID());
    }

    @Test
    void tokenPadraoTemClaimsEsperadas() throws Exception {
        JWTClaimsSet claims = SignedJWT.parse(JwtTestSupport.tokenRs256("ATENDENTE")).getJWTClaimsSet();

        assertEquals(JwtTestSupport.ISSUER, claims.getIssuer());
        assertEquals(List.of("oficina-api"), claims.getAudience());
        assertEquals("usuario-teste", claims.getSubject());
        assertEquals("usuario-teste", claims.getStringClaim("preferred_username"));
        assertEquals("Bearer", claims.getStringClaim("typ"));
        assertEquals("oficina-api", claims.getStringClaim("azp"));
        assertEquals(List.of("ATENDENTE"), roles(claims));
        assertEquals(300, (claims.getExpirationTime().getTime() - claims.getIssueTime().getTime()) / 1000);
    }

    @Test
    void tokenPadraoAssinaComRs256EKid() throws Exception {
        SignedJWT jwt = SignedJWT.parse(JwtTestSupport.tokenRs256("ATENDENTE"));

        assertEquals(JWSAlgorithm.RS256, jwt.getHeader().getAlgorithm());
        assertEquals("teste-oficina", jwt.getHeader().getKeyID());
    }

    @Test
    void tokenPadraoDecodificaPeloJwksServido() {
        Jwt jwt = DECODER.decode(JwtTestSupport.tokenRs256("ATENDENTE"));

        assertEquals("usuario-teste", jwt.getSubject());
        assertEquals(List.of("oficina-api"), jwt.getClaimAsStringList("aud"));
        assertTrue(JwksTestServer.requisicoes() >= 1);
    }

    @Test
    void ajusteSobrescreveSoOClaimAlvo() throws Exception {
        JWTClaimsSet claims = SignedJWT.parse(JwtTestSupport.tokenRs256(b -> b.subject("outro"), "MECANICO"))
                .getJWTClaimsSet();

        assertEquals("outro", claims.getSubject());
        assertEquals(JwtTestSupport.ISSUER, claims.getIssuer());
        assertEquals(List.of("oficina-api"), claims.getAudience());
        assertEquals("usuario-teste", claims.getStringClaim("preferred_username"));
        assertEquals(List.of("MECANICO"), roles(claims));
    }

    @Test
    void tokenDeOutraChaveComMesmoKidNaoVerifica() throws Exception {
        String token = JwtTestSupport.tokenAssinadoComOutraChave();

        assertThrows(BadJwtException.class, () -> DECODER.decode(token));
        assertEquals("teste-oficina", SignedJWT.parse(token).getHeader().getKeyID());
    }

    @Test
    void tokenComKidDesconhecidoNaoVerifica() throws Exception {
        String token = JwtTestSupport.tokenComKidDesconhecido();

        assertThrows(BadJwtException.class, () -> DECODER.decode(token));
        assertEquals("kid-desconhecido", SignedJWT.parse(token).getHeader().getKeyID());
    }

    @Test
    void tokenHs256NaoVerificaComJwks() throws Exception {
        String token = JwtTestSupport.tokenHs256("ATENDENTE");

        assertThrows(BadJwtException.class, () -> DECODER.decode(token));
        SignedJWT jwt = SignedJWT.parse(token);
        assertEquals(JWSAlgorithm.HS256, jwt.getHeader().getAlgorithm());
        assertEquals(List.of("oficina-api"), jwt.getJWTClaimsSet().getAudience());
    }

    @Test
    void tokenAlgNoneNaoVerifica() throws Exception {
        String token = JwtTestSupport.tokenAlgNone();

        PlainJWT jwt = assertInstanceOf(PlainJWT.class, JWTParser.parse(token));
        assertEquals(Algorithm.NONE, jwt.getHeader().getAlgorithm());
        assertTrue(token.endsWith("."));
        assertThrows(BadJwtException.class, () -> DECODER.decode(token));
    }

    @Test
    void tokenExpiradoFalhaNaValidacaoDeTempo() throws Exception {
        String token = JwtTestSupport.tokenExpirado();

        assertThrows(JwtValidationException.class, () -> DECODER.decode(token));
        JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
        Instant exp = claims.getExpirationTime().toInstant();
        assertTrue(exp.isBefore(Instant.now().minusSeconds(100)));
        assertTrue(claims.getIssueTime().toInstant().isBefore(exp));
    }

    @Test
    void tokenNaoAntesDeFalhaNaValidacaoDeTempo() throws Exception {
        String token = JwtTestSupport.tokenNaoAntesDe();

        assertThrows(JwtValidationException.class, () -> DECODER.decode(token));
        assertTrue(SignedJWT.parse(token).getJWTClaimsSet().getNotBeforeTime().toInstant()
                .isAfter(Instant.now().plusSeconds(100)));
    }

    @Test
    void variantesDeIssuerEAudienceMudamSoOCampoAlvo() throws Exception {
        JWTClaimsSet comIssuer = SignedJWT.parse(JwtTestSupport.tokenComIssuer("http://keycloak:8080/realms/oficina"))
                .getJWTClaimsSet();
        assertEquals("http://keycloak:8080/realms/oficina", comIssuer.getIssuer());
        assertEquals(List.of("oficina-api"), comIssuer.getAudience());
        assertEquals("usuario-teste", comIssuer.getSubject());

        SignedJWT semAudience = SignedJWT.parse(JwtTestSupport.tokenSemAudience());
        assertTrue(semAudience.getJWTClaimsSet().getAudience().isEmpty());
        assertFalse(semAudience.getPayload().toJSONObject().containsKey("aud"));
    }

    @Test
    void audienceUnicaViraStringELista2ViraArray() throws Exception {
        Object unica = SignedJWT.parse(JwtTestSupport.tokenComAudience("oficina-api"))
                .getPayload().toJSONObject().get("aud");
        Object duas = SignedJWT.parse(JwtTestSupport.tokenComAudience(List.of("account", "oficina-api")))
                .getPayload().toJSONObject().get("aud");

        assertInstanceOf(String.class, unica);
        assertEquals(List.of("account", "oficina-api"), assertInstanceOf(List.class, duas));
    }

    @Test
    void tokenSemExpNaoTemClaimExp() throws Exception {
        JWTClaimsSet claims = SignedJWT.parse(JwtTestSupport.tokenSemExp()).getJWTClaimsSet();

        assertNull(claims.getExpirationTime());
        assertNotNull(claims.getIssueTime());
    }

    @Test
    void tokenDeIdTemTypId() throws Exception {
        JWTClaimsSet claims = SignedJWT.parse(JwtTestSupport.tokenDeId()).getJWTClaimsSet();

        assertEquals("ID", claims.getStringClaim("typ"));
        assertEquals(List.of("oficina-api"), claims.getAudience());
        assertEquals(JwtTestSupport.ISSUER, claims.getIssuer());
    }

    @Test
    void tokensSemSubOuSemLoginRemovemSoAClaim() throws Exception {
        JWTClaimsSet semSub = SignedJWT.parse(JwtTestSupport.tokenSemSub()).getJWTClaimsSet();
        assertNull(semSub.getSubject());
        assertEquals("usuario-teste", semSub.getStringClaim("preferred_username"));

        JWTClaimsSet semLogin = SignedJWT.parse(JwtTestSupport.tokenSemPreferredUsername()).getJWTClaimsSet();
        assertNull(semLogin.getClaim("preferred_username"));
        assertEquals("usuario-teste", semLogin.getSubject());
    }

    @Test
    void tokenSemPerfilDaOficinaSoTemRolesTecnicos() throws Exception {
        JWTClaimsSet claims = SignedJWT.parse(JwtTestSupport.tokenSemPerfilDaOficina()).getJWTClaimsSet();

        assertEquals(JwtTestSupport.ROLES_TECNICOS, roles(claims));
    }

    @Test
    void jwksServidoEhJsonPublicoESoAceitaGet() throws Exception {
        HttpClient cliente = HttpClient.newHttpClient();
        URI uri = URI.create(JwksTestServer.uri());

        HttpResponse<String> get = cliente.send(HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, get.statusCode());
        assertTrue(get.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertEquals(JwtTestSupport.jwksJson(), get.body());
        assertFalse(get.body().contains("\"d\""));

        HttpResponse<String> post = cliente.send(
                HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(405, post.statusCode());
    }

    @Test
    void servidorEscutaSoEmLoopbackEmPortaEfemera() throws Exception {
        URI uri = URI.create(JwksTestServer.uri());

        assertTrue(InetAddress.getByName(uri.getHost()).isLoopbackAddress());
        assertTrue(uri.getPort() > 0);
        assertEquals(JwksTestServer.CAMINHO, uri.getPath());
        assertTrue(JwksTestServer.enderecoDeEscuta().getAddress().isLoopbackAddress());
        assertEquals(uri.getPort(), JwksTestServer.enderecoDeEscuta().getPort());
    }

    @SuppressWarnings("unchecked")
    private static List<String> roles(JWTClaimsSet claims) throws Exception {
        Map<String, Object> realmAccess = claims.getJSONObjectClaim("realm_access");
        return (List<String>) realmAccess.get("roles");
    }
}
