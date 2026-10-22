package br.com.grupo117.oficina.catalogoestoque.domain;

/**
 * Reserva rejeitada porque a quantidade solicitada excede a quantidade livre da peca.
 */
public final class EstoqueInsuficienteException extends RuntimeException {

    public EstoqueInsuficienteException(String mensagem) {
        super(mensagem);
    }
}
