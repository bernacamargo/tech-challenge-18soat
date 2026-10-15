package br.com.grupo117.oficina.atendimento.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdemDeServicoJpaRepository extends JpaRepository<OrdemDeServicoEntity, UUID> {}
