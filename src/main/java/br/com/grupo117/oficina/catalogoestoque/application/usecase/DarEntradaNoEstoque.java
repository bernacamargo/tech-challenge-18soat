package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import br.com.grupo117.oficina.catalogoestoque.application.port.PublicadorDeEventos;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import java.util.Objects;
import java.util.UUID;

/**
 * Da entrada de uma quantidade no estoque de uma peca existente e publica
 * EstoqueReabastecido depois de salvar.
 */
public final class DarEntradaNoEstoque {

    private final PecaRepositorio pecas;
    private final PublicadorDeEventos eventos;

    public DarEntradaNoEstoque(PecaRepositorio pecas, PublicadorDeEventos eventos) {
        this.pecas = Objects.requireNonNull(pecas);
        this.eventos = Objects.requireNonNull(eventos);
    }

    public Peca darEntrada(UUID pecaId, int quantidade) {
        Objects.requireNonNull(pecaId, "Identificador da peca e obrigatorio");
        Peca peca = pecas.buscarPorId(pecaId).orElseThrow(() -> new PecaNaoEncontradaException(pecaId));
        peca.reabastecer(quantidade);
        Peca salva = pecas.salvar(peca);
        eventos.publicar(peca.eventos());
        peca.limparEventos();
        return salva;
    }
}
