package br.com.grupo117.oficina.atendimento.infrastructure.persistence;

import java.util.UUID;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdemDeServicoJpaRepository extends JpaRepository<OrdemDeServicoEntity, UUID> {
    boolean existsByIdAndExcluidaEmIsNotNull(UUID id);

    Optional<OrdemDeServicoEntity> findByIdAndExcluidaEmIsNull(UUID id);

    List<OrdemDeServicoEntity> findAllByExcluidaEmIsNull(Sort sort);
}
