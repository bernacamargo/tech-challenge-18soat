package br.com.grupo117.oficina.atendimento.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VeiculoJpaRepository extends JpaRepository<VeiculoEntity, String> {

    @Query("""
            select veiculo from VeiculoEntity veiculo
            join fetch veiculo.cliente cliente
            where veiculo.excluidaEm is null and cliente.excluidaEm is null
            """)
    List<VeiculoEntity> listarComCliente();

    @Query("""
            select veiculo from VeiculoEntity veiculo
            join fetch veiculo.cliente cliente
            where veiculo.placa = :placa
              and veiculo.excluidaEm is null
              and cliente.excluidaEm is null
            """)
    Optional<VeiculoEntity> buscarComCliente(String placa);
}
