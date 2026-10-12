package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import java.util.Objects;

/**
 * Altera o nome do cliente ja cadastrado. O CPF/CNPJ continua sendo a identidade.
 */
public final class AlterarCliente {

    private final ClienteRepositorio clientes;

    public AlterarCliente(ClienteRepositorio clientes) {
        this.clientes = Objects.requireNonNull(clientes, "Repositorio de clientes e obrigatorio");
    }

    public Cliente alterar(String documento, String nome) {
        CpfCnpj cpfCnpj = CpfCnpj.de(documento);
        if (clientes.buscarPorCpfCnpj(cpfCnpj).isEmpty()) {
            throw new ClienteNaoEncontradoException(cpfCnpj);
        }
        return clientes.salvar(new Cliente(nome, cpfCnpj));
    }
}
