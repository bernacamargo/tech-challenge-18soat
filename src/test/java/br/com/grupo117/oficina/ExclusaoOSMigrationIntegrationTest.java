package br.com.grupo117.oficina;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ExclusaoOSMigrationIntegrationTest extends AbstractIntegrationTest {
    @Autowired private JdbcTemplate jdbc;

    @Test
    void migrationMantemOsExistentesAtivas() {
        String schema = "upgrade_exclusao_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .target("11").load().migrate();
            jdbc.update("insert into " + schema + ".cliente (cpf_cnpj, nome) values ('52998224725', 'Maria')");
            jdbc.update("insert into " + schema + ".veiculo (placa, marca, modelo, ano, cpf_cnpj_cliente) values ('ABC1D23', 'Fiat', 'Uno', 2012, '52998224725')");
            UUID id = UUID.randomUUID();
            jdbc.update("insert into " + schema + ".ordem_de_servico (id, cpf_cnpj_cliente, placa_veiculo, status) values (?, '52998224725', 'ABC1D23', 'RECEBIDA')", id);

            var resultado = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .target("12").load().migrate();

            assertThat(resultado.migrationsExecuted).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from " + schema
                    + ".ordem_de_servico where id = ? and excluida_em is null and status = 'RECEBIDA'",
                    Integer.class, id)).isEqualTo(1);
        } finally {
            jdbc.execute("drop schema if exists " + schema + " cascade");
        }
    }
}
