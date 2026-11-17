package br.com.grupo117.oficina;

import java.sql.Timestamp;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AberturaOrdemDeServicoMigrationIntegrationTest extends AbstractIntegrationTest {
    @Autowired private JdbcTemplate jdbc;

    @Test
    void migrationPreservaAberturaDesconhecidaEDefineDefaultParaNovasInsercoes() {
        String schema = "upgrade_abertura_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .target("10").load().migrate();
            jdbc.update("insert into " + schema + ".cliente (cpf_cnpj, nome) values ('52998224725', 'Maria')");
            jdbc.update("insert into " + schema + ".veiculo (placa, marca, modelo, ano, cpf_cnpj_cliente) values ('ABC1D23', 'Fiat', 'Uno', 2012, '52998224725')");
            UUID legada = UUID.randomUUID();
            inserirOs(schema, legada);

            var resultado = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .target("11").load().migrate();
            assertThat(resultado.migrationsExecuted).isEqualTo(1);
            assertThat(jdbc.queryForObject("select aberta_em from " + schema
                    + ".ordem_de_servico where id = ?", Timestamp.class, legada)).isNull();

            UUID nova = UUID.randomUUID();
            inserirOs(schema, nova);
            assertThat(jdbc.queryForObject("select aberta_em from " + schema
                    + ".ordem_de_servico where id = ?", Timestamp.class, nova)).isNotNull();
        } finally {
            jdbc.execute("drop schema if exists " + schema + " cascade");
        }
    }

    private void inserirOs(String schema, UUID id) {
        jdbc.update("insert into " + schema + ".ordem_de_servico (id, cpf_cnpj_cliente, placa_veiculo, status) values (?, '52998224725', 'ABC1D23', 'RECEBIDA')", id);
    }
}
