package br.com.grupo117.oficina.atendimento.domain;

/**
 * Placa rejeitada por nao seguir o formato antigo (LLLNNNN) nem o Mercosul (LLLNLNN).
 */
public final class PlacaInvalidaException extends RuntimeException {

    public PlacaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
