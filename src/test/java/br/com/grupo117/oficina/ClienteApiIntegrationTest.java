package br.com.grupo117.oficina;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.grupo117.oficina.atendimento.infrastructure.ClienteResponse;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class ClienteApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void swaggerMostraORecursoDeClientes() {
        ResponseEntity<String> docs = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody()).contains("/api/clientes");
        assertThat(docs.getBody()).contains("Clientes");
    }

    @Test
    void criarEListarCliente() {
        HttpHeaders headers = headersComPapel("ATENDENTE");
        Map<String, String> corpo = Map.of("nome", "Maria Souza", "cpfCnpj", "529.982.247-25");

        ResponseEntity<ClienteResponse> criado = restTemplate.exchange(
                "/api/clientes",
                HttpMethod.POST,
                new HttpEntity<>(corpo, headers),
                ClienteResponse.class
        );

        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criado.getBody()).isNotNull();
        assertThat(criado.getBody().nome()).isEqualTo("Maria Souza");
        assertThat(criado.getBody().cpfCnpj()).isEqualTo("52998224725");

        ResponseEntity<ClienteResponse[]> lista = restTemplate.exchange(
                "/api/clientes",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ClienteResponse[].class
        );

        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).extracting(ClienteResponse::cpfCnpj).contains("52998224725");
    }

    @Test
    void semTokenNaoAcessaClientes() {
        ResponseEntity<String> resposta = restTemplate.getForEntity("/api/clientes", String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void mecanicoNaoGerenciaClientes() {
        ResponseEntity<String> resposta = restTemplate.exchange(
                "/api/clientes",
                HttpMethod.GET,
                new HttpEntity<>(headersComPapel("MECANICO")),
                String.class
        );

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private static HttpHeaders headersComPapel(String papel) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestSupport.token(papel));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
