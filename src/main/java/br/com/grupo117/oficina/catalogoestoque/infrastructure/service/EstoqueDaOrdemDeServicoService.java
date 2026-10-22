package br.com.grupo117.oficina.catalogoestoque.infrastructure.service;

import br.com.grupo117.oficina.catalogoestoque.application.port.EstoqueDaOrdemDeServico;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.MovimentarEstoqueDaOrdemDeServico;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fronteira transacional da reserva e da baixa: peca e reserva sao gravadas juntas ou
 * nenhuma e gravada. Primary para que quem injeta o port receba sempre esta versao.
 */
@Service
@Primary
@Transactional
public class EstoqueDaOrdemDeServicoService implements EstoqueDaOrdemDeServico {
    private final MovimentarEstoqueDaOrdemDeServico movimentar;

    public EstoqueDaOrdemDeServicoService(MovimentarEstoqueDaOrdemDeServico movimentar) {
        this.movimentar = movimentar;
    }

    @Override
    public UUID reservar(UUID ordemDeServicoId, UUID pecaId, int quantidade) {
        return movimentar.reservar(ordemDeServicoId, pecaId, quantidade);
    }

    @Override
    public int baixarReservasDaOrdemDeServico(UUID ordemDeServicoId) {
        return movimentar.baixarReservasDaOrdemDeServico(ordemDeServicoId);
    }
}
