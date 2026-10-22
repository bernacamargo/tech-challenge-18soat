package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import java.util.UUID;

public final class PecaNaoEncontradaException extends RuntimeException {

    public PecaNaoEncontradaException(UUID id) {
        super("Peca nao encontrada: " + id);
    }
}
