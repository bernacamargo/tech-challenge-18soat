package br.com.grupo117.oficina.identidadeacesso.domain;

/**
 * Credencial recusada: ausente, malformada, sem identificador ou sem login, ou
 * rejeitada pelo provedor de identidade. A mensagem nunca contem o valor da credencial.
 */
public final class CredencialInvalidaException extends RuntimeException {

    public CredencialInvalidaException(String mensagem) {
        super(mensagem);
    }

    /** Usado pelo adapter do provedor (PR3) para preservar a causa tecnica. */
    public CredencialInvalidaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
