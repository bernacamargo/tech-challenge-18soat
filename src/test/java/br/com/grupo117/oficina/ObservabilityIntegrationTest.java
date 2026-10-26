package br.com.grupo117.oficina;

import org.junit.jupiter.api.Test;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureObservability
@ActiveProfiles("observability")
@TestPropertySource(properties = "management.server.port=0")
class ObservabilityIntegrationTest extends AbstractIntegrationTest {
    @LocalManagementPort
    int managementPort;

    @Test
    void expoeMetricasSomenteNaPortaDeManagementSemLiberarAPIs() {
        restTemplate.getForEntity("/api/health", String.class);
        var metrics = restTemplate.getForEntity(
                "http://127.0.0.1:" + managementPort + "/actuator/prometheus", String.class);

        assertThat(metrics.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(metrics.getBody()).contains("jvm_memory_used_bytes", "hikaricp_connections_active",
                "http_server_requests_seconds_bucket", "application=\"oficina\"");
        var headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestSupport.token("ADMINISTRADOR"));
        assertThat(restTemplate.exchange("/actuator/prometheus", HttpMethod.GET,
                new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.getForEntity("/api/ordens-de-servico", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity(
                "http://127.0.0.1:" + managementPort + "/actuator/env", String.class).getStatusCode())
                .isNotEqualTo(HttpStatus.OK);
    }
}
