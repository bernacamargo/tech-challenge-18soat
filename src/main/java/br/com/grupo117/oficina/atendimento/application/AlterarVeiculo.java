package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Placa;
import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import java.util.Objects;

/**
 * Altera marca, modelo e ano do veiculo. A placa e o cliente dono permanecem.
 */
public final class AlterarVeiculo {

    private final VeiculoRepositorio veiculos;

    public AlterarVeiculo(VeiculoRepositorio veiculos) {
        this.veiculos = Objects.requireNonNull(veiculos, "Repositorio de veiculos e obrigatorio");
    }

    public Veiculo alterar(String placa, String marca, String modelo, int ano) {
        Placa placaNormalizada = Placa.de(placa);
        Veiculo existente = veiculos.buscarPorPlaca(placaNormalizada)
                .orElseThrow(() -> new VeiculoNaoEncontradoException(placaNormalizada));
        return veiculos.salvar(new Veiculo(existente.cliente(), placaNormalizada, marca, modelo, ano));
    }
}
