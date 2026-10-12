package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import java.util.Objects;
import java.util.UUID;

public final class RemoverPeca {

    private final PecaRepositorio pecas;

    public RemoverPeca(PecaRepositorio pecas) {
        this.pecas = Objects.requireNonNull(pecas);
    }

    public void remover(UUID id) {
        Objects.requireNonNull(id, "Identificador da peca e obrigatorio");
        if (pecas.buscarPorId(id).isEmpty()) {
            throw new PecaNaoEncontradaException(id);
        }
        pecas.remover(id);
    }
}
