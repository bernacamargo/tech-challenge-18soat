package br.com.grupo117.oficina.atendimento.domain;

/**
 * Documento fiscal rejeitado pela regra de digitos verificadores do CPF ou do CNPJ.
 */
public final class CpfCnpjInvalidoException extends RuntimeException {

    public CpfCnpjInvalidoException(String mensagem) {
        super(mensagem);
    }
}
