package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Placa;
import java.util.Objects;

/**
 * Exclui logicamente o veiculo identificado pela placa.
 */
public final class RemoverVeiculo {

    private final VeiculoRepositorio veiculos;
    private final VinculoDeCadastro vinculos;

    public RemoverVeiculo(VeiculoRepositorio veiculos) {
        this(veiculos, VinculoDeCadastro.nenhum());
    }

    public RemoverVeiculo(VeiculoRepositorio veiculos, VinculoDeCadastro vinculos) {
        this.veiculos = Objects.requireNonNull(veiculos, "Repositorio de veiculos e obrigatorio");
        this.vinculos = Objects.requireNonNull(vinculos, "Consulta de vinculos e obrigatoria");
    }

    public void remover(String placa) {
        Placa placaNormalizada = Placa.de(placa);
        if (veiculos.buscarPorPlaca(placaNormalizada).isEmpty()) {
            throw new VeiculoNaoEncontradoException(placaNormalizada);
        }
        if (vinculos.veiculoEmUso(placaNormalizada)) {
            throw new CadastroComVinculoException();
        }
        veiculos.remover(placaNormalizada);
    }
}
