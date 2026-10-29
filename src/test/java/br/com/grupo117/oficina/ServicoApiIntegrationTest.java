package br.com.grupo117.oficina;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.grupo117.oficina.catalogoestoque.infrastructure.api.ServicoResponse;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(strings = {"MECANICO", "ATENDENTE"})
    void equipeConsultaCatalogoSemPermissaoDeEscrita(String papel) {
        var criado = restTemplate.exchange("/api/servicos", HttpMethod.POST,
                new HttpEntity<>(Map.of("nome", "Revisao", "preco", 80), headersComPapel("ADMINISTRADOR")),
                ServicoResponse.class);
        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var id = criado.getBody().id();
        var headers = headersComPapel(papel);
        assertThat(restTemplate.exchange("/api/servicos", HttpMethod.GET,
                new HttpEntity<>(headers), ServicoResponse[].class).getBody())
                .extracting(ServicoResponse::id).contains(id);
        assertThat(restTemplate.exchange("/api/servicos/" + id, HttpMethod.GET,
                new HttpEntity<>(headers), ServicoResponse.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        var dados = new HttpEntity<>(Map.of("nome", "Alterado", "preco", 90), headers);
        assertThat(restTemplate.exchange("/api/servicos", HttpMethod.POST, dados, String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(restTemplate.exchange("/api/servicos/" + id, HttpMethod.PUT, dados, String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(restTemplate.exchange("/api/servicos/" + id, HttpMethod.DELETE,
                new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private static HttpHeaders headersComPapel(String papel) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestSupport.token(papel));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
