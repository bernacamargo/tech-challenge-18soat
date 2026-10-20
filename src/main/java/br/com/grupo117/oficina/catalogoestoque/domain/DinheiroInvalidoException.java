package br.com.grupo117.oficina.catalogoestoque.domain;

/**
 * Valor monetario rejeitado por estar ausente, mal formatado ou negativo.
 */
public final class DinheiroInvalidoException extends RuntimeException {

    public DinheiroInvalidoException(String mensagem) {
        super(mensagem);
    }
}
