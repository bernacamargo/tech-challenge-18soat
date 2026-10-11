package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Placa;

/**
 * Ja existe veiculo com a placa informada.
 */
public final class VeiculoJaCadastradoException extends RuntimeException {

    public VeiculoJaCadastradoException(Placa placa) {
        super("Veiculo ja cadastrado: " + placa.valor());
    }
}
