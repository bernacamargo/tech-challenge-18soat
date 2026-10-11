package br.com.grupo117.oficina.atendimento.domain;

import java.util.Objects;

/**
 * Cliente da oficina, identificado pelo CPF ou CNPJ. O historico de veiculos e de
 * ordens de servico referencia esta identidade; nao fica embutido neste agregado.
 */
public final class Cliente {

    private final String nome;
    private final CpfCnpj cpfCnpj;

    public Cliente(String nome, CpfCnpj cpfCnpj) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do cliente e obrigatorio");
        }
        this.nome = nome.trim();
        this.cpfCnpj = Objects.requireNonNull(cpfCnpj, "CPF/CNPJ e obrigatorio");
    }

    public static Cliente cadastrar(String nome, String documento) {
        return new Cliente(nome, CpfCnpj.de(documento));
    }

    public String nome() {
        return nome;
    }

    public CpfCnpj cpfCnpj() {
        return cpfCnpj;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Cliente cliente)) {
            return false;
        }
        return cpfCnpj.equals(cliente.cpfCnpj);
    }

    @Override
    public int hashCode() {
        return cpfCnpj.hashCode();
    }

    @Override
    public String toString() {
        return "Cliente[nome=" + nome + ", cpfCnpj=" + cpfCnpj.digitos() + "]";
    }
}
