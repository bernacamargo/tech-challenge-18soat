package br.com.grupo117.oficina;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.grupo117.oficina.catalogoestoque.infrastructure.api.ServicoResponse;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class ServicoApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void swaggerMostraORecursoDeServicos() {
        ResponseEntity<String> docs = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody()).contains("/api/servicos");
        assertThat(docs.getBody()).contains("Servicos");
    }

    @Test
    void criarEListarServico() {
        HttpHeaders headers = headersComPapel("ADMINISTRADOR");
        Map<String, Object> corpo = Map.of(
                "nome", "Troca de oleo",
                "descricao", "Inclui filtro",
                "preco", new BigDecimal("150.00")
        );

        ResponseEntity<ServicoResponse> criado = restTemplate.exchange(
                "/api/servicos",
                HttpMethod.POST,
                new HttpEntity<>(corpo, headers),
                ServicoResponse.class
        );

        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criado.getHeaders().getLocation()).isNotNull();
        assertThat(criado.getBody()).isNotNull();
        assertThat(criado.getBody().nome()).isEqualTo("Troca de oleo");
        assertThat(criado.getBody().descricao()).isEqualTo("Inclui filtro");
        assertThat(criado.getBody().preco()).isEqualByComparingTo("150.00");

        ResponseEntity<ServicoResponse[]> lista = restTemplate.exchange(
                "/api/servicos",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ServicoResponse[].class
        );

        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).extracting(ServicoResponse::id).contains(criado.getBody().id());
    }

    @Test
    void semTokenNaoAcessaServicos() {
        ResponseEntity<String> resposta = restTemplate.getForEntity("/api/servicos", String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void mecanicoNaoGerenciaServicos() {
        ResponseEntity<String> resposta = restTemplate.exchange(
                "/api/servicos",
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
