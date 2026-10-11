package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;

/**
 * Nenhum cliente com o CPF/CNPJ informado esta cadastrado.
 */
public final class ClienteNaoEncontradoException extends RuntimeException {

    public ClienteNaoEncontradoException(CpfCnpj cpfCnpj) {
        super("Cliente nao encontrado: " + cpfCnpj.digitos());
    }
}
