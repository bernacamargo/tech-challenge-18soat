package br.com.grupo117.oficina;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O compose sobe a app sem o perfil observability por padrao (APP_PROFILES vazio). Nesse modo nao
 * existe porta de management separada, e o Spring recusa management.server.address ("Management-specific
 * server address cannot be configured as the management server is not listening on a separate port").
 * Por isso o compose nao pode definir MANAGEMENT_SERVER_ADDRESS: o endereco vai por uma variavel que so
 * o application-observability.yml le.
 */
class DockerComposeObservabilityTest {

    @Test
    void appNaoDefineEnderecoDeManagementQueQuebraASubidaSemObservabilidade() throws IOException {
        Map<String, Object> ambiente = ambienteDaApp();

        assertThat(ambiente).doesNotContainKey("MANAGEMENT_SERVER_ADDRESS");
        assertThat(ambiente).containsEntry("OBSERVABILITY_MANAGEMENT_ADDRESS", "0.0.0.0");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> ambienteDaApp() throws IOException {
        try (Reader reader = Files.newBufferedReader(Path.of("docker-compose.yml"))) {
            Map<String, Object> compose = new Yaml().load(reader);
            Map<String, Object> servicos = (Map<String, Object>) compose.get("services");
            Map<String, Object> app = (Map<String, Object>) servicos.get("app");
            return (Map<String, Object>) app.get("environment");
        }
    }
}
