package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.Email;
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

    public Cliente cadastrar(String nome, String documento) { return cadastrar(nome, documento, null); }

    public Cliente cadastrar(String nome, String documento, String email) {
        Email contato = email == null ? null : new Email(email);
        CpfCnpj cpfCnpj = CpfCnpj.de(documento);
        if (clientes.buscarPorCpfCnpj(cpfCnpj).isPresent()) {
            throw new ClienteJaCadastradoException(cpfCnpj);
        }
        return clientes.salvar(new Cliente(nome, cpfCnpj, contato));
    }
}
