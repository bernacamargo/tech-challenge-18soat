package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.Email;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import java.util.Objects;

/**
 * Altera nome e contato do cliente já cadastrado. O CPF/CNPJ continua sendo a identidade.
 */
public final class AlterarCliente {

    private final ClienteRepositorio clientes;

    public AlterarCliente(ClienteRepositorio clientes) {
        this.clientes = Objects.requireNonNull(clientes, "Repositorio de clientes e obrigatorio");
    }

    public Cliente alterar(String documento, String nome) { return alterar(documento, nome, null); }

    public Cliente alterar(String documento, String nome, String email) {
        Email contato = email == null ? null : new Email(email);
        CpfCnpj cpfCnpj = CpfCnpj.de(documento);
        Cliente existente = clientes.buscarPorCpfCnpj(cpfCnpj)
                .orElseThrow(() -> new ClienteNaoEncontradoException(cpfCnpj));
        return clientes.salvar(new Cliente(nome, cpfCnpj,
                contato == null ? existente.email().orElse(null) : contato));
    }
}
