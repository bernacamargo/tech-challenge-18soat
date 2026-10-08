package br.com.grupo117.oficina;

import org.junit.jupiter.api.Test;

/**
 * Smoke test do scaffold: valida que o contexto sobe, o Flyway executa o baseline
 * e o Postgres real (Testcontainers) esta conectado.
 */
class HealthSmokeTest extends AbstractIntegrationTest {
    // testes herdados de AbstractIntegrationTest validam /api/health e o datasource
}
