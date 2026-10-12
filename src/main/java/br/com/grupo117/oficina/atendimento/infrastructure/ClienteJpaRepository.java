package br.com.grupo117.oficina.atendimento.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, String> {
}
