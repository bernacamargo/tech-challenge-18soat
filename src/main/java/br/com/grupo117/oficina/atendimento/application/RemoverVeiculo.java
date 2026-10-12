package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Placa;
import java.util.Objects;

/**
 * Remove o veiculo identificado pela placa.
 */
public final class RemoverVeiculo {

    private final VeiculoRepositorio veiculos;

    public RemoverVeiculo(VeiculoRepositorio veiculos) {
        this.veiculos = Objects.requireNonNull(veiculos, "Repositorio de veiculos e obrigatorio");
    }

    public void remover(String placa) {
        Placa placaNormalizada = Placa.de(placa);
        if (veiculos.buscarPorPlaca(placaNormalizada).isEmpty()) {
            throw new VeiculoNaoEncontradoException(placaNormalizada);
        }
        veiculos.remover(placaNormalizada);
    }
}
