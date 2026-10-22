package br.com.grupo117.oficina.catalogoestoque.infrastructure.persistence;

import br.com.grupo117.oficina.catalogoestoque.domain.StatusReserva;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaDeEstoqueJpaRepository extends JpaRepository<ReservaDeEstoqueEntity, UUID> {

    List<ReservaDeEstoqueEntity> findByOrdemDeServicoIdAndStatusOrderById(UUID ordemDeServicoId, StatusReserva status);
}
