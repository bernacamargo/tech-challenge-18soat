package br.com.grupo117.oficina.atendimento.infrastructure.persistence;

import br.com.grupo117.oficina.atendimento.application.port.ExclusaoOSPort;
import br.com.grupo117.oficina.atendimento.application.usecase.OrdemDeServicoNaoEncontradaException;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class ExclusaoOSAdapter implements ExclusaoOSPort {
    private final JdbcTemplate jdbc;

    public ExclusaoOSAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void excluir(UUID osId) {
        int alteradas = jdbc.update("""
                UPDATE ordem_de_servico SET excluida_em = CURRENT_TIMESTAMP
                WHERE id = ? AND excluida_em IS NULL
                """, osId);
        if (alteradas == 0) {
            throw new OrdemDeServicoNaoEncontradaException(osId);
        }
        jdbc.update("""
                UPDATE notificacao_orcamento SET cancelada_em = CURRENT_TIMESTAMP, ultimo_erro = NULL
                WHERE ordem_de_servico_id = ? AND enviada_em IS NULL AND cancelada_em IS NULL
                """, osId);
    }
}
