package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;

/**
 * Ja existe cliente com o CPF/CNPJ informado.
 */
public final class ClienteJaCadastradoException extends RuntimeException {

    public ClienteJaCadastradoException(CpfCnpj cpfCnpj) {
        super("Cliente ja cadastrado: " + cpfCnpj.digitos());
    }
}
