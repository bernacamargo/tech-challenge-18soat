package br.com.grupo117.oficina;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

class EmailClienteMigrationIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void atualizaV8PreservandoClienteVeiculoEOrcamentoExistentes() {
        String schema = "upgrade_email_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("8").load().migrate();
            jdbc.update("INSERT INTO " + schema + ".cliente(cpf_cnpj, nome) VALUES ('52998224725', 'Maria')");
            jdbc.update("INSERT INTO " + schema + ".veiculo(placa, marca, modelo, ano, cpf_cnpj_cliente) VALUES ('ABC1D23', 'Fiat', 'Uno', 2012, '52998224725')");
            UUID osId = UUID.randomUUID();
            jdbc.update("INSERT INTO " + schema + ".ordem_de_servico(id, cpf_cnpj_cliente, placa_veiculo, status, orcamento_total) VALUES (?, '52998224725', 'ABC1D23', 'AGUARDANDO_APROVACAO', 156.25)", osId);
            var resultado = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("9").load().migrate();
            assertThat(resultado.migrationsExecuted).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT nome FROM " + schema + ".cliente", String.class)).isEqualTo("Maria");
            assertThat(jdbc.queryForObject("SELECT email FROM " + schema + ".cliente", String.class)).isNull();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + schema + ".veiculo", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT orcamento_total FROM " + schema + ".ordem_de_servico", java.math.BigDecimal.class)).isEqualByComparingTo("156.25");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + schema + ".notificacao_orcamento", Integer.class)).isZero();
        } finally { jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE"); }
    }
}
