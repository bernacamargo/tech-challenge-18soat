package br.com.grupo117.oficina.catalogoestoque.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PecaJpaRepository extends JpaRepository<PecaEntity, UUID> {}
