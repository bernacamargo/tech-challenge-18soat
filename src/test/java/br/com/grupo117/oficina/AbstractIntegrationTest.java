package br.com.grupo117.oficina;

import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste base de integracao: sobe o contexto Spring contra um PostgreSQL real
 * via Testcontainers. O container fica vivo pela JVM inteira. Se cada classe
 * de teste o derrubasse, o contexto Spring em cache seguiria apontando para
 * uma porta que ja nao escuta.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        postgres.start();
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    DataSource dataSource;

    @DynamicPropertySource
    static void registrarInfraDeTeste(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("oficina.security.jwt-secret", () -> JwtTestSupport.SECRET);
    }

    @BeforeEach
    void limparDados() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE cliente CASCADE");
        }
    }

    @Test
    void contextoSobeEApiResponde() {
        ResponseEntity<Map> health = restTemplate.getForEntity("/api/health", Map.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).containsEntry("status", "UP");
    }

    @Test
    void datasourceEhPostgresReal() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(connection.getMetaData().getDatabaseMajorVersion()).isGreaterThanOrEqualTo(17);
        }
    }
}
