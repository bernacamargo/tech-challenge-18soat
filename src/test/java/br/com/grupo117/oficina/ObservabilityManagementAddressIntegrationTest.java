package br.com.grupo117.oficina;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Com o perfil observability, OBSERVABILITY_MANAGEMENT_ADDRESS (definida pelo compose) vira o endereco
 * da porta de management; sem a variavel, o default continua 127.0.0.1 (ADR 0016).
 */
@AutoConfigureObservability
@ActiveProfiles("observability")
@TestPropertySource(properties = {"management.server.port=0", "OBSERVABILITY_MANAGEMENT_ADDRESS=0.0.0.0"})
class ObservabilityManagementAddressIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ManagementServerProperties management;

    @LocalManagementPort
    int managementPort;

    @Test
    void enderecoDeManagementVemDaVariavelDoPerfilDeObservabilidade() {
        assertThat(management.getAddress()).isNotNull();
        assertThat(management.getAddress().getHostAddress()).isEqualTo("0.0.0.0");
        assertThat(restTemplate.getForEntity(
                "http://127.0.0.1:" + managementPort + "/actuator/prometheus", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
