package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Placa;
import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import java.util.Optional;

/**
 * Port de saida do repositorio de veiculos. A implementacao (JPA ou outra)
 * fica na infraestrutura; o caso de uso so depende desta interface.
 */
public interface VeiculoRepositorio {

    Optional<Veiculo> buscarPorPlaca(Placa placa);

    Veiculo salvar(Veiculo veiculo);
}
