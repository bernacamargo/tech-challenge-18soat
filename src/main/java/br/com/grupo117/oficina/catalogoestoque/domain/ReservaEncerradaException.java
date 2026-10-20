package br.com.grupo117.oficina.catalogoestoque.domain;

/**
 * Operacao rejeitada porque a reserva ja foi baixada ou cancelada.
 */
public final class ReservaEncerradaException extends RuntimeException {

    public ReservaEncerradaException(String mensagem) {
        super(mensagem);
    }
}
