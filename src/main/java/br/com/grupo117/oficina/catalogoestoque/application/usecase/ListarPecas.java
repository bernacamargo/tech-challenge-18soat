package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import java.util.List;
import java.util.Objects;

public final class ListarPecas {

    private final PecaRepositorio pecas;

    public ListarPecas(PecaRepositorio pecas) {
        this.pecas = Objects.requireNonNull(pecas);
    }

    public List<Peca> listar() {
        return pecas.listar();
    }
}
