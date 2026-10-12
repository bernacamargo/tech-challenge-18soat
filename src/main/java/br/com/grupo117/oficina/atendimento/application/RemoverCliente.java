package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import java.util.Objects;

/**
 * Remove o cliente identificado pelo CPF/CNPJ.
 */
public final class RemoverCliente {

    private final ClienteRepositorio clientes;

    public RemoverCliente(ClienteRepositorio clientes) {
        this.clientes = Objects.requireNonNull(clientes, "Repositorio de clientes e obrigatorio");
    }

    public void remover(String documento) {
        CpfCnpj cpfCnpj = CpfCnpj.de(documento);
        if (clientes.buscarPorCpfCnpj(cpfCnpj).isEmpty()) {
            throw new ClienteNaoEncontradoException(cpfCnpj);
        }
        clientes.remover(cpfCnpj);
    }
}
