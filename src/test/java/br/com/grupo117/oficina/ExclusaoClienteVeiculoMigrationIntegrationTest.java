package br.com.grupo117.oficina;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExclusaoClienteVeiculoMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void migrationAdicionaMarcaEPreservaCadastrosAtivos() {
        String schema = "upgrade_cadastro_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .target("12").load().migrate();
            jdbc.update("insert into " + schema + ".cliente (cpf_cnpj, nome) values ('52998224725', 'Maria')");
            jdbc.update("insert into " + schema
                    + ".veiculo (placa, marca, modelo, ano, cpf_cnpj_cliente) values ('ABC1D23', 'Fiat', 'Uno', 2012, '52998224725')");

            var resultado = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .load().migrate();

            assertThat(resultado.migrationsExecuted).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from " + schema
                    + ".cliente where cpf_cnpj = '52998224725' and nome = 'Maria' and excluida_em is null",
                    Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from " + schema
                    + ".veiculo where placa = 'ABC1D23' and excluida_em is null",
                    Integer.class)).isEqualTo(1);
        } finally {
            jdbc.execute("drop schema if exists " + schema + " cascade");
        }
    }
}
