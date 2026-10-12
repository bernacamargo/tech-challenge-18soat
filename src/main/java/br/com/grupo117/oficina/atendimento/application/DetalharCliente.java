package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import java.util.Objects;

/**
 * Devolve o cliente identificado pelo CPF/CNPJ.
 */
public final class DetalharCliente {

    private final ClienteRepositorio clientes;

    public DetalharCliente(ClienteRepositorio clientes) {
        this.clientes = Objects.requireNonNull(clientes, "Repositorio de clientes e obrigatorio");
    }

    public Cliente detalhar(String documento) {
        CpfCnpj cpfCnpj = CpfCnpj.de(documento);
        return clientes.buscarPorCpfCnpj(cpfCnpj)
                .orElseThrow(() -> new ClienteNaoEncontradoException(cpfCnpj));
    }
}
