package br.com.grupo117.oficina.atendimento.application;

/** O cadastro ainda é referenciado e não pode ser excluído. */
public final class CadastroComVinculoException extends RuntimeException {

    public CadastroComVinculoException() {
        super("Cadastro possui vinculos com outros registros e nao pode ser removido ou alterado");
    }
}
