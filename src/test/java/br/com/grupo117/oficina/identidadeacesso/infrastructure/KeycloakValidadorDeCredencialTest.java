package br.com.grupo117.oficina.identidadeacesso.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.grupo117.oficina.identidadeacesso.domain.Credencial;
import br.com.grupo117.oficina.identidadeacesso.domain.CredencialInvalidaException;
import br.com.grupo117.oficina.identidadeacesso.domain.DadosDaCredencial;
import br.com.grupo117.oficina.infra.security.JwksTestServer;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class KeycloakValidadorDeCredencialTest {

    private static final String TOKEN = "token-t1";

    // C1
    @Test
    void mapeiaSubPreferredUsernameERealmRolesSemFiltrar() {
        DecoderFake fake = DecoderFake.devolvendo(jwtPadrao());

        DadosDaCredencial dados = new KeycloakValidadorDeCredencial(fake).validar(new Credencial(TOKEN));

        assertEquals(new DadosDaCredencial("sub-1", "atendente", Set.of("ATENDENTE", "default-roles-oficina")), dados);
    }

    // C2
    @Test
    void entregaAoDecoderOValorDaCredencialUmaVez() {
        DecoderFake fake = DecoderFake.devolvendo(jwtPadrao());

        new KeycloakValidadorDeCredencial(fake).validar(new Credencial(TOKEN));

        assertEquals(1, fake.chamadas);
        assertEquals("token-t1", fake.ultimoToken);
    }

    // C3
    @Test
    void realmAccessAusenteViraVazio() {
        Jwt jwt = base().subject("sub-1").claim("preferred_username", "atendente").build();

        DadosDaCredencial dados = validarCom(jwt);

        assertEquals(Set.of(), dados.perfisInformados());
    }

    // C4
    @Test
    void realmAccessQueNaoEhObjetoViraVazio() {
        Jwt jwt = base().subject("sub-1").claim("realm_access", "ATENDENTE").build();

        DadosDaCredencial dados = validarCom(jwt);

        assertEquals(Set.of(), dados.perfisInformados());
    }

    static Stream<Map<String, Object>> realmAccessSemColecaoDeRoles() {
        return Stream.of(Map.of("roles", "ATENDENTE"), Map.of("outra", List.of("ATENDENTE")));
    }

    // C5
    @ParameterizedTest
    @MethodSource("realmAccessSemColecaoDeRoles")
    void rolesAusenteOuQueNaoEhColecaoViraVazio(Map<String, Object> realmAccess) {
        Jwt jwt = base().subject("sub-1").claim("realm_access", realmAccess).build();

        DadosDaCredencial dados = validarCom(jwt);

        assertEquals(Set.of(), dados.perfisInformados());
    }

    // C6
    @Test
    void rolesComItensNaoStringSaoIgnorados() {
        List<Object> roles = Arrays.asList("ATENDENTE", 42, null, Map.of("x", 1), "atendente");
        Jwt jwt = base().subject("sub-1").claim("realm_access", Map.of("roles", roles)).build();

        DadosDaCredencial dados = validarCom(jwt);

        assertEquals(Set.of("ATENDENTE", "atendente"), dados.perfisInformados());
    }

    // C7
    @Test
    void semPreferredUsernameDevolveLoginNuloSemUsarOSub() {
        Jwt jwt = base().subject("sub-1").claim("realm_access", Map.of("roles", List.of("ATENDENTE"))).build();

        DadosDaCredencial dados = validarCom(jwt);

        assertNull(dados.login());
        assertEquals("sub-1", dados.sujeito());
    }

    // C8
    @Test
    void semSubDevolveSujeitoNulo() {
        Jwt jwt = base().claim("preferred_username", "atendente").build();

        DadosDaCredencial dados = validarCom(jwt);

        assertNull(dados.sujeito());
    }

    // C9
    @Test
    void badJwtExceptionViraCredencialInvalida() {
        BadJwtException b = new BadJwtException("Malformed token");
        KeycloakValidadorDeCredencial validador = new KeycloakValidadorDeCredencial(DecoderFake.lancando(b));

        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> validador.validar(new Credencial(TOKEN)));

        assertEquals("Credencial invalida", e.getMessage());
        assertSame(b, e.getCause());
    }

    // C10
    @Test
    void jwtValidationExceptionViraCredencialInvalida() {
        JwtValidationException v = new JwtValidationException("Jwt expired at 2026-10-25T00:00:00Z",
                List.of(new OAuth2Error("invalid_token")));
        KeycloakValidadorDeCredencial validador = new KeycloakValidadorDeCredencial(DecoderFake.lancando(v));

        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> validador.validar(new Credencial(TOKEN)));

        assertEquals("Credencial invalida", e.getMessage());
    }

    // C11
    @Test
    void tokenExpiradoNoDecoderRealViraCredencialInvalida() {
        KeycloakValidadorDeCredencial validador = new KeycloakValidadorDeCredencial(decoderReal());

        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> validador.validar(new Credencial(JwtTestSupport.tokenExpirado())));

        assertInstanceOf(JwtValidationException.class, e.getCause());
    }

    // C12
    @Test
    void tokenValidoNoDecoderRealDevolveDados() {
        KeycloakValidadorDeCredencial validador = new KeycloakValidadorDeCredencial(decoderReal());

        DadosDaCredencial dados = validador.validar(new Credencial(JwtTestSupport.tokenRs256("ATENDENTE")));

        assertEquals(new DadosDaCredencial(JwtTestSupport.SUJEITO, JwtTestSupport.LOGIN, Set.of("ATENDENTE")), dados);
    }

    // C13
    @Test
    void falhaDeJwksNaoViraCredencialInvalida() {
        JwtException j = new JwtException("JWKS indisponivel");
        KeycloakValidadorDeCredencial validador = new KeycloakValidadorDeCredencial(DecoderFake.lancando(j));

        JwtException lancada = assertThrows(JwtException.class, () -> validador.validar(new Credencial(TOKEN)));

        assertSame(j, lancada);
        assertFalse(CredencialInvalidaException.class.isInstance(lancada));
    }

    // C14
    @Test
    void jwksInacessivelNoDecoderRealPropagaJwtException() throws IOException {
        int porta;
        try (ServerSocket socket = new ServerSocket(0)) {
            porta = socket.getLocalPort();
        }
        JwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri("http://127.0.0.1:" + porta + "/certs").build();
        KeycloakValidadorDeCredencial validador = new KeycloakValidadorDeCredencial(decoder);
        Credencial credencial = new Credencial(JwtTestSupport.tokenRs256("ATENDENTE"));

        JwtException e = assertThrows(JwtException.class, () -> validador.validar(credencial));

        assertFalse(e instanceof BadJwtException);
        assertFalse(CredencialInvalidaException.class.isInstance(e));
    }

    // C15
    @Test
    void mensagemNaoContemTokenNemMensagemDoNimbus() {
        BadJwtException b = new BadJwtException("falha com token-secreto-123");
        KeycloakValidadorDeCredencial validador = new KeycloakValidadorDeCredencial(DecoderFake.lancando(b));

        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> validador.validar(new Credencial("token-secreto-123")));

        assertEquals("Credencial invalida", e.getMessage());
        assertFalse(e.getMessage().contains("token-secreto-123"));
        assertFalse(e.getMessage().contains("falha"));
    }

    // C16
    @Test
    void credencialNulaFalhaSemChamarODecoder() {
        DecoderFake fake = DecoderFake.devolvendo(jwtPadrao());
        KeycloakValidadorDeCredencial validador = new KeycloakValidadorDeCredencial(fake);

        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class, () -> validador.validar(null));

        assertEquals("Credencial ausente", e.getMessage());
        assertEquals(0, fake.chamadas);
    }

    // C17
    @Test
    void exigeDecoder() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new KeycloakValidadorDeCredencial(null));

        assertEquals("Decoder de JWT e obrigatorio", e.getMessage());
    }

    private static Jwt.Builder base() {
        return Jwt.withTokenValue(TOKEN).header("alg", "RS256");
    }

    private static Jwt jwtPadrao() {
        return base()
                .subject("sub-1")
                .claim("preferred_username", "atendente")
                .claim("realm_access", Map.of("roles", List.of("ATENDENTE", "default-roles-oficina")))
                .build();
    }

    private static DadosDaCredencial validarCom(Jwt jwt) {
        return new KeycloakValidadorDeCredencial(DecoderFake.devolvendo(jwt)).validar(new Credencial(TOKEN));
    }

    private static JwtDecoder decoderReal() {
        return NimbusJwtDecoder.withJwkSetUri(JwksTestServer.uri()).build();
    }

    /** Decoder escrito a mao: devolve o Jwt configurado ou lanca a excecao configurada; conta chamadas. */
    private static final class DecoderFake implements JwtDecoder {

        private final Jwt jwt;
        private final JwtException falha;
        int chamadas;
        String ultimoToken;

        private DecoderFake(Jwt jwt, JwtException falha) {
            this.jwt = jwt;
            this.falha = falha;
        }

        static DecoderFake devolvendo(Jwt jwt) {
            return new DecoderFake(jwt, null);
        }

        static DecoderFake lancando(JwtException falha) {
            return new DecoderFake(null, falha);
        }

        @Override
        public Jwt decode(String token) {
            chamadas++;
            ultimoToken = token;
            if (falha != null) {
                throw falha;
            }
            return jwt;
        }
    }
}
