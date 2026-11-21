package br.com.grupo117.oficina.atendimento.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, String> {

    Optional<ClienteEntity> findByCpfCnpjAndExcluidaEmIsNull(String cpfCnpj);

    List<ClienteEntity> findAllByExcluidaEmIsNull();
}
