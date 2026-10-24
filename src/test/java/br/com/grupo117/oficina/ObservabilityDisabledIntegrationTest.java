package br.com.grupo117.oficina;

import org.junit.jupiter.api.Test;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureObservability
class ObservabilityDisabledIntegrationTest extends AbstractIntegrationTest {
    @Test
    void naoExpoePrometheusSemPerfilDeObservabilidade() {
        var headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestSupport.token("ADMINISTRADOR"));
        assertThat(restTemplate.exchange("/actuator/prometheus", HttpMethod.GET,
                new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
