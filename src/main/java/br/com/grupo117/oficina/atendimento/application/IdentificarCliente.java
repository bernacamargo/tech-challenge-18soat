package br.com.grupo117.oficina.atendimento.application;

import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.Email;
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

    public Cliente identificar(String documento, String nome) { return identificar(documento, nome, null); }

    public Cliente identificar(String documento, String nome, String email) {
        Email contato = email == null ? null : new Email(email);
        CpfCnpj cpfCnpj = CpfCnpj.de(documento);
        Cliente existente = clientes.buscarPorCpfCnpj(cpfCnpj).orElse(null);
        if (existente == null) return clientes.salvar(new Cliente(nome, cpfCnpj, contato));
        // Identificar uma OS não altera silenciosamente um contato já cadastrado.
        if (contato != null && existente.email().isEmpty()) {
            return clientes.salvar(new Cliente(existente.nome(), cpfCnpj, contato));
        }
        return existente;
    }
}
