package br.com.grupo117.oficina.catalogoestoque.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Servico do catalogo da oficina, identificado por id e precificado em dinheiro.
 */
public final class Servico {

    private final UUID id;
    private final String nome;
    private final String descricao;
    private final Dinheiro preco;

    public Servico(UUID id, String nome, String descricao, Dinheiro preco) {
        this.id = Objects.requireNonNull(id, "Id do servico e obrigatorio");
        this.nome = textoObrigatorio(nome, "Nome do servico e obrigatorio");
        this.descricao = descricaoOpcional(descricao);
        this.preco = precoValido(preco);
    }

    public static Servico cadastrar(String nome, String descricao, BigDecimal preco) {
        return new Servico(UUID.randomUUID(), nome, descricao, Dinheiro.de(preco));
    }

    public Servico alterar(String nome, String descricao, BigDecimal preco) {
        return new Servico(id, nome, descricao, Dinheiro.de(preco));
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String descricao() {
        return descricao;
    }

    public Dinheiro preco() {
        return preco;
    }

    private static String textoObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim();
    }

    private static Dinheiro precoValido(Dinheiro preco) {
        if (preco == null || !preco.positivo()) {
            throw new DinheiroInvalidoException("Preco do servico deve ser maior que zero");
        }
        return preco;
    }

    private static String descricaoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Servico servico)) {
            return false;
        }
        return id.equals(servico.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Servico[id=" + id + ", nome=" + nome + ", preco=" + preco.valor() + "]";
    }
}
