package br.com.grupo117.oficina.catalogoestoque.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoJpaRepository extends JpaRepository<ServicoEntity, UUID> {
}
