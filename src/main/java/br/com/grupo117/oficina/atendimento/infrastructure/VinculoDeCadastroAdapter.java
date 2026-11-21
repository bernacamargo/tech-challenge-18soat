package br.com.grupo117.oficina.atendimento.infrastructure;

import br.com.grupo117.oficina.atendimento.application.VinculoDeCadastro;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.Placa;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Vínculo que ainda impede a exclusão: veículo visível do cliente, ou qualquer OS.
 */
@Component
public class VinculoDeCadastroAdapter implements VinculoDeCadastro {

    private final JdbcTemplate jdbc;

    public VinculoDeCadastroAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean clienteEmUso(CpfCnpj cpfCnpj) {
        Boolean emUso = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM veiculo
                    WHERE cpf_cnpj_cliente = ? AND excluida_em IS NULL
                ) OR EXISTS (
                    SELECT 1 FROM ordem_de_servico
                    WHERE cpf_cnpj_cliente = ?
                )
                """, Boolean.class, cpfCnpj.digitos(), cpfCnpj.digitos());
        return Boolean.TRUE.equals(emUso);
    }

    @Override
    public boolean veiculoEmUso(Placa placa) {
        Boolean emUso = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM ordem_de_servico
                    WHERE placa_veiculo = ?
                )
                """, Boolean.class, placa.valor());
        return Boolean.TRUE.equals(emUso);
    }
}
