package br.com.grupo117.oficina;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

class DecisaoOrcamentoMigrationIntegrationTest extends AbstractIntegrationTest {
    @Autowired private JdbcTemplate jdbc;

    @Test
    void atualizaBancoNaV5SemInventarInicioOuRejeicaoDeOsExistentes() {
        String schema = "upgrade_decisao_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .target("5").load().migrate();
            jdbc.update("insert into " + schema + ".cliente (cpf_cnpj, nome) values ('52998224725', 'Maria')");
            jdbc.update("insert into " + schema + ".veiculo (placa, marca, modelo, ano, cpf_cnpj_cliente) values ('ABC1D23', 'Fiat', 'Uno', 2012, '52998224725')");
            UUID entregue = UUID.randomUUID();
            UUID aguardando = UUID.randomUUID();
            Instant fim = Instant.parse("2026-10-12T17:00:00Z");
            jdbc.update("insert into " + schema + ".ordem_de_servico (id, cpf_cnpj_cliente, placa_veiculo, status, finalizada_em) values (?, '52998224725', 'ABC1D23', 'ENTREGUE', ?)", entregue, Timestamp.from(fim));
            jdbc.update("insert into " + schema + ".ordem_de_servico (id, cpf_cnpj_cliente, placa_veiculo, status, orcamento_total) values (?, '52998224725', 'ABC1D23', 'AGUARDANDO_APROVACAO', 50.00)", aguardando);
            jdbc.update("insert into " + schema + ".item_da_os (id, ordem_de_servico_id, posicao, catalogo_id, tipo, descricao, quantidade, preco_unitario) values (?, ?, 0, ?, 'SERVICO', 'Diagnóstico', 1, 50.00)", UUID.randomUUID(), aguardando, UUID.randomUUID());

            var resultado = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .load().migrate();
            assertThat(resultado.migrationsExecuted).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from " + schema + ".ordem_de_servico", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("select finalizada_em from " + schema + ".ordem_de_servico where id = ?", Timestamp.class, entregue).toInstant()).isEqualTo(fim);
            assertThat(jdbc.queryForObject("select orcamento_total from " + schema + ".ordem_de_servico where id = ?", java.math.BigDecimal.class, aguardando)).isEqualByComparingTo("50.00");
            assertThat(jdbc.queryForObject("select count(*) from " + schema + ".item_da_os", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from " + schema + ".ordem_de_servico where iniciada_em is null and orcamento_rejeitado_em is null and motivo_rejeicao is null", Integer.class)).isEqualTo(2);
        } finally {
            jdbc.execute("drop schema if exists " + schema + " cascade");
        }
    }
}
