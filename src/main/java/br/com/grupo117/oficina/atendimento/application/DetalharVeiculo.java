package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Placa;
import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import java.util.Objects;

/**
 * Devolve o veiculo identificado pela placa.
 */
public final class DetalharVeiculo {

    private final VeiculoRepositorio veiculos;

    public DetalharVeiculo(VeiculoRepositorio veiculos) {
        this.veiculos = Objects.requireNonNull(veiculos, "Repositorio de veiculos e obrigatorio");
    }

    public Veiculo detalhar(String placa) {
        Placa placaNormalizada = Placa.de(placa);
        return veiculos.buscarPorPlaca(placaNormalizada)
                .orElseThrow(() -> new VeiculoNaoEncontradoException(placaNormalizada));
    }
}
