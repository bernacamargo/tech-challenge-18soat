package br.com.grupo117.oficina.catalogoestoque.domain;

/**
 * Quantidade de estoque rejeitada por ser zero ou negativa.
 */
public final class QuantidadeInvalidaException extends RuntimeException {

    public QuantidadeInvalidaException(String mensagem) {
        super(mensagem);
    }
}
