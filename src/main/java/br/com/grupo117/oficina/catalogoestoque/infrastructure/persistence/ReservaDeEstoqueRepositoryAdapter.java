package br.com.grupo117.oficina.catalogoestoque.infrastructure.persistence;

import br.com.grupo117.oficina.catalogoestoque.application.port.ReservaDeEstoqueRepositorio;
import br.com.grupo117.oficina.catalogoestoque.domain.ReservaDeEstoque;
import br.com.grupo117.oficina.catalogoestoque.domain.StatusReserva;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class ReservaDeEstoqueRepositoryAdapter implements ReservaDeEstoqueRepositorio {
    private final ReservaDeEstoqueJpaRepository jpa;

    public ReservaDeEstoqueRepositoryAdapter(ReservaDeEstoqueJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public ReservaDeEstoque salvar(ReservaDeEstoque reserva) {
        return jpa.saveAndFlush(ReservaDeEstoqueEntity.de(reserva)).paraDominio();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaDeEstoque> buscarAtivasPorOrdemDeServico(UUID ordemDeServicoId) {
        return jpa.findByOrdemDeServicoIdAndStatusOrderById(ordemDeServicoId, StatusReserva.ATIVA).stream()
                .map(ReservaDeEstoqueEntity::paraDominio).toList();
    }
}
