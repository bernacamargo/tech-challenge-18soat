package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Placa;

/**
 * Nenhum veiculo com a placa informada esta cadastrado.
 */
public final class VeiculoNaoEncontradoException extends RuntimeException {

    public VeiculoNaoEncontradoException(Placa placa) {
        super("Veiculo nao encontrado: " + placa.valor());
    }
}
