package br.com.grupo117.oficina;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.grupo117.oficina.atendimento.infrastructure.ClienteResponse;
import br.com.grupo117.oficina.atendimento.infrastructure.VeiculoResponse;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class VeiculoApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void swaggerMostraORecursoDeVeiculos() {
        ResponseEntity<String> docs = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody()).contains("/api/veiculos");
        assertThat(docs.getBody()).contains("Veiculos");
    }

    @Test
    void criarEListarVeiculoLigadoAoCliente() {
        HttpHeaders headers = headersComPapel("ATENDENTE");
        cadastrarCliente(headers);

        Map<String, Object> corpo = Map.of(
                "cpfCnpjCliente", "529.982.247-25",
                "placa", "abc-1d23",
                "marca", "Fiat",
                "modelo", "Uno",
                "ano", 2012
        );

        ResponseEntity<VeiculoResponse> criado = restTemplate.exchange(
                "/api/veiculos",
                HttpMethod.POST,
                new HttpEntity<>(corpo, headers),
                VeiculoResponse.class
        );

        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criado.getBody()).isNotNull();
        assertThat(criado.getBody().placa()).isEqualTo("ABC1D23");
        assertThat(criado.getBody().marca()).isEqualTo("Fiat");
        assertThat(criado.getBody().modelo()).isEqualTo("Uno");
        assertThat(criado.getBody().ano()).isEqualTo(2012);
        assertThat(criado.getBody().cpfCnpjCliente()).isEqualTo("52998224725");

        ResponseEntity<VeiculoResponse[]> lista = restTemplate.exchange(
                "/api/veiculos",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                VeiculoResponse[].class
        );

        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).extracting(VeiculoResponse::placa).contains("ABC1D23");
        assertThat(lista.getBody()).extracting(VeiculoResponse::cpfCnpjCliente).contains("52998224725");
    }

    @Test
    void semTokenNaoAcessaVeiculos() {
        ResponseEntity<String> resposta = restTemplate.getForEntity("/api/veiculos", String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void cadastrarCliente(HttpHeaders headers) {
        Map<String, String> cliente = Map.of("nome", "Maria Souza", "cpfCnpj", "529.982.247-25");
        ResponseEntity<ClienteResponse> criado = restTemplate.exchange(
                "/api/clientes",
                HttpMethod.POST,
                new HttpEntity<>(cliente, headers),
                ClienteResponse.class
        );
        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private static HttpHeaders headersComPapel(String papel) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestSupport.token(papel));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
