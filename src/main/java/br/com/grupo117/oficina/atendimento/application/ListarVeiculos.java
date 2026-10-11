package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Veiculo;
import java.util.List;
import java.util.Objects;

/**
 * Lista os veiculos cadastrados.
 */
public final class ListarVeiculos {

    private final VeiculoRepositorio veiculos;

    public ListarVeiculos(VeiculoRepositorio veiculos) {
        this.veiculos = Objects.requireNonNull(veiculos, "Repositorio de veiculos e obrigatorio");
    }

    public List<Veiculo> listar() {
        return veiculos.listar();
    }
}
