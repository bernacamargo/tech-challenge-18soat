package br.com.grupo117.oficina.catalogoestoque.domain;

/**
 * Valor monetario rejeitado por ser nulo, zero, negativo ou ilegivel.
 */
public final class DinheiroInvalidoException extends RuntimeException {

    public DinheiroInvalidoException(String mensagem) {
        super(mensagem);
    }
}
