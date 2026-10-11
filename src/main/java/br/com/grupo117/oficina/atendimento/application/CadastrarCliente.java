package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import java.util.Objects;

/**
 * Cadastra um cliente novo. Documento repetido nao gera outro cadastro.
 */
public final class CadastrarCliente {

    private final ClienteRepositorio clientes;

    public CadastrarCliente(ClienteRepositorio clientes) {
        this.clientes = Objects.requireNonNull(clientes, "Repositorio de clientes e obrigatorio");
    }

    public Cliente cadastrar(String nome, String documento) {
        CpfCnpj cpfCnpj = CpfCnpj.de(documento);
        if (clientes.buscarPorCpfCnpj(cpfCnpj).isPresent()) {
            throw new ClienteJaCadastradoException(cpfCnpj);
        }
        return clientes.salvar(new Cliente(nome, cpfCnpj));
    }
}
