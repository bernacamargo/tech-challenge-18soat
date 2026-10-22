package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import java.util.Objects;
import java.util.UUID;

public final class DetalharPeca {

    private final PecaRepositorio pecas;

    public DetalharPeca(PecaRepositorio pecas) {
        this.pecas = Objects.requireNonNull(pecas);
    }

    public Peca detalhar(UUID id) {
        Objects.requireNonNull(id, "Identificador da peca e obrigatorio");
        return pecas.buscarPorId(id).orElseThrow(() -> new PecaNaoEncontradaException(id));
    }
}
