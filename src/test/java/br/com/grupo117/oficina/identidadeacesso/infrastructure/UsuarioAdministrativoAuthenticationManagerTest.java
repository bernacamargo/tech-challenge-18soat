package br.com.grupo117.oficina.identidadeacesso.infrastructure;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.grupo117.oficina.identidadeacesso.application.usecase.AutenticarUsuarioAdministrativo;
import br.com.grupo117.oficina.identidadeacesso.domain.UsuarioAdministrativo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

/** Cadeia real manager -> caso de uso -> adapter do Keycloak -> JwtDecoder escrito a mao. */
class UsuarioAdministrativoAuthenticationManagerTest {

    private static final String TOKEN = "t1";

    private final DecoderContador decoder = new DecoderContador();
    private final UsuarioAdministrativoAuthenticationManager manager = new UsuarioAdministrativoAuthenticationManager(
            new AutenticarUsuarioAdministrativo(new KeycloakValidadorDeCredencial(decoder)));

    // C1
    @Test
    void tokenValidoAutenticaComAuthoritiesDoPerfil() {
        decoder.devolver(TOKEN, jwt("sub-1", "atendente", "ATENDENTE"));

        Authentication a = autenticar(TOKEN);

        assertTrue(a.isAuthenticated());
        assertEquals(Set.of("ROLE_ATENDENTE"), nomes(a));
        assertEquals("sub-1", a.getName());
        assertEquals("atendente", ((UsuarioAdministrativo) a.getPrincipal()).login());
    }

    // C2
    @Test
    void rolesTecnicosNaoViramAuthority() {
        decoder.devolver(TOKEN, jwt("sub-1", "atendente", "ATENDENTE", "default-roles-oficina", "offline_access"));

        Authentication a = autenticar(TOKEN);

        assertEquals(Set.of("ROLE_ATENDENTE"), nomes(a));
    }

    // C3
    @Test
    void variosPerfisViramVariasAuthorities() {
        decoder.devolver(TOKEN, jwt("sub-1", "admin", "ADMINISTRADOR", "MECANICO"));

        Authentication a = autenticar(TOKEN);

        assertEquals(Set.of("ROLE_ADMINISTRADOR", "ROLE_MECANICO"), nomes(a));
    }

    // C4
    @Test
    void semPerfilDaOficinaAutenticaSemAuthorities() {
        decoder.devolver(TOKEN, jwt("sub-2", "tecnico", "default-roles-oficina", "offline_access", "uma_authorization"));

        Authentication a = autenticar(TOKEN);

        assertTrue(a.isAuthenticated());
        assertEquals(Set.of(), nomes(a));
        assertEquals("sub-2", a.getName());
        assertNull(a.getPrincipal());
    }

    // C5
    @Test
    void credencialInvalidaViraInvalidBearerTokenComDescricaoFixa() {
        decoder.lancar(TOKEN, new BadJwtException("Jwt expired at 2026-10-25T00:00:00Z"));

        InvalidBearerTokenException e = assertThrows(InvalidBearerTokenException.class, () -> autenticar(TOKEN));

        assertEquals("Token invalido", e.getError().getDescription());
        assertEquals("invalid_token", e.getError().getErrorCode());
        assertFalse(e.getMessage().contains("expired"));
    }

    // C6
    @Test
    void semSubViraInvalidBearerToken() {
        decoder.devolver(TOKEN, base().claim("preferred_username", "atendente")
                .claim("realm_access", Map.of("roles", List.of("ATENDENTE"))).build());

        InvalidBearerTokenException e = assertThrows(InvalidBearerTokenException.class, () -> autenticar(TOKEN));

        assertEquals("Token invalido", e.getError().getDescription());
    }

    // C7
    @Test
    void semPreferredUsernameViraInvalidBearerToken() {
        decoder.devolver(TOKEN, base().subject("sub-1")
                .claim("realm_access", Map.of("roles", List.of("ATENDENTE"))).build());

        InvalidBearerTokenException e = assertThrows(InvalidBearerTokenException.class, () -> autenticar(TOKEN));

        assertEquals("Token invalido", e.getError().getDescription());
    }

    // C8
    @Test
    void falhaTecnicaDoProvedorViraAuthenticationServiceException() {
        JwtException j = new JwtException("JWKS indisponivel");
        decoder.lancar(TOKEN, j);

        AuthenticationServiceException e = assertThrows(AuthenticationServiceException.class, () -> autenticar(TOKEN));

        assertEquals("Falha ao validar token", e.getMessage());
        assertSame(j, e.getCause());
        assertFalse(InvalidBearerTokenException.class.isInstance(e));
    }

    // C9
    @Test
    void erroInesperadoNaoViraCredencialInvalida() {
        IllegalStateException i = new IllegalStateException("x");
        decoder.lancar(TOKEN, i);

        assertSame(i, assertThrows(IllegalStateException.class, () -> autenticar(TOKEN)));
    }

    // C10
    @Test
    void tipoDeAutenticacaoNaoSuportadoFalha() {
        InvalidBearerTokenException e = assertThrows(InvalidBearerTokenException.class,
                () -> manager.authenticate(new UsernamePasswordAuthenticationToken("u", "p")));

        assertEquals("Token invalido", e.getError().getDescription());
        assertEquals(0, decoder.chamadas);
    }

    // C11
    @Test
    void naoGuardaNemExpoeOToken() {
        decoder.devolver(TOKEN, jwt("sub-1", "login-c11", "ATENDENTE"));

        Authentication a = autenticar(TOKEN);

        assertNull(a.getCredentials());
        assertFalse(a.toString().contains("t1"));
        assertFalse(a.toString().contains("sub-1"));
        assertFalse(a.toString().contains("login-c11"));
    }

    // C12
    @Test
    void decodificaUmaVezPorAutenticacao() {
        decoder.devolver(TOKEN, jwt("sub-1", "atendente", "ATENDENTE"));

        autenticar(TOKEN);

        assertEquals(1, decoder.chamadas);
    }

    // C13
    @Test
    void exigeCasoDeUso() {
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> new UsuarioAdministrativoAuthenticationManager(null));

        assertEquals("Caso de uso de autenticacao e obrigatorio", e.getMessage());
    }

    // C14
    @Test
    void fabricasExigemArgumentos() {
        assertThrows(NullPointerException.class, () -> UsuarioAdministrativoAuthentication.semPerfilDaOficina(null));
        assertThrows(NullPointerException.class, () -> UsuarioAdministrativoAuthentication.de(null));
    }

    private Authentication autenticar(String token) {
        return manager.authenticate(new BearerTokenAuthenticationToken(token));
    }

    private static Set<String> nomes(Authentication a) {
        return a.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(toSet());
    }

    private static Jwt.Builder base() {
        return Jwt.withTokenValue(TOKEN).header("alg", "RS256");
    }

    private static Jwt jwt(String sub, String login, String... roles) {
        return base().subject(sub).claim("preferred_username", login)
                .claim("realm_access", Map.of("roles", List.of(roles))).build();
    }

    /** Fake sem logica: procura o token no mapa (Jwt ou excecao a lancar) e conta as chamadas. */
    private static final class DecoderContador implements JwtDecoder {

        private final Map<String, Object> respostas = new HashMap<>();
        int chamadas;

        void devolver(String token, Jwt jwt) {
            respostas.put(token, jwt);
        }

        void lancar(String token, RuntimeException excecao) {
            respostas.put(token, excecao);
        }

        @Override
        public Jwt decode(String token) {
            chamadas++;
            Object resposta = respostas.get(token);
            if (resposta instanceof RuntimeException excecao) {
                throw excecao;
            }
            return (Jwt) resposta;
        }
    }
}
