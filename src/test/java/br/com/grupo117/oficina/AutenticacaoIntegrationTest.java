package br.com.grupo117.oficina;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Autenticacao pelo caso de uso AutenticarUsuarioAdministrativo (D4), com tokens RS256 reais do
 * JwtTestSupport: credencial invalida da 401; sem perfil da oficina fica autenticado sem
 * authorities (403 nas rotas administrativas, rotas publicas seguem acessiveis).
 */
class AutenticacaoIntegrationTest extends AbstractIntegrationTest {

    private static final String ACOMPANHAMENTO = "/api/acompanhamento/ordens-de-servico/";

    // C15
    @Test
    void atendenteComRoleTecnicoAcessaClientes() {
        ResponseEntity<String> resposta = get("/api/clientes", JwtTestSupport.tokenRs256("ATENDENTE", "default-roles-oficina"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // C16
    @Test
    void somenteRolesTecnicosDa403EmRotasAdministrativas() {
        String token = JwtTestSupport.tokenSemPerfilDaOficina();

        assertThat(get("/api/clientes", token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/pecas", token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // C17
    @Test
    void somenteRolesTecnicosAcessaHealthEApiDocs() {
        String token = JwtTestSupport.tokenSemPerfilDaOficina();

        assertThat(get("/api/health", token).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/v3/api-docs", token).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // C18
    @Test
    void somenteRolesTecnicosAcessaAcompanhamentoPublico() {
        ResponseEntity<String> resposta = get(ACOMPANHAMENTO + UUID.randomUUID(), JwtTestSupport.tokenSemPerfilDaOficina());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // C19
    @Test
    void perfilEmMinusculasDa403() {
        ResponseEntity<String> resposta = get("/api/clientes", JwtTestSupport.tokenRs256("atendente"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // C20
    @Test
    void tokenSemSubDa401() {
        ResponseEntity<String> resposta = get("/api/clientes", JwtTestSupport.tokenSemSub());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // C21
    @Test
    void tokenSemPreferredUsernameDa401() {
        ResponseEntity<String> resposta = get("/api/clientes", JwtTestSupport.tokenSemPreferredUsername());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // C22
    @Test
    void bearerInvalidoEmRotaPublicaDa401() {
        ResponseEntity<String> resposta = get(ACOMPANHAMENTO + UUID.randomUUID(), "a.b.c");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // C23
    @Test
    void aprovacaoPublicaSemBearerNaoExigeAutenticacao() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> resposta = restTemplate.exchange(ACOMPANHAMENTO + UUID.randomUUID() + "/aprovacao",
                HttpMethod.POST, new HttpEntity<>(null, headers), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<String> get(String caminho, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(caminho, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
