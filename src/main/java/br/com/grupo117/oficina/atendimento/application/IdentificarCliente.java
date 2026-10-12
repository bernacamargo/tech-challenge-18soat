package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import java.util.Objects;

/**
 * Identifica o cliente pelo CPF/CNPJ. Se o documento ainda nao existe, cadastra
 * um cliente novo; se ja existe, devolve o cliente salvo, sem duplicar.
 */
public final class IdentificarCliente {

    private final ClienteRepositorio clientes;

    public IdentificarCliente(ClienteRepositorio clientes) {
        this.clientes = Objects.requireNonNull(clientes, "Repositorio de clientes e obrigatorio");
    }

    public Cliente identificar(String documento, String nome) {
        CpfCnpj cpfCnpj = CpfCnpj.de(documento);
        return clientes.buscarPorCpfCnpj(cpfCnpj)
                .orElseGet(() -> clientes.salvar(new Cliente(nome, cpfCnpj)));
    }
}
