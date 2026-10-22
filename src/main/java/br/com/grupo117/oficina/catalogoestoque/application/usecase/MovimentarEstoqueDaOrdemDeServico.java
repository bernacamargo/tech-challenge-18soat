package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import br.com.grupo117.oficina.catalogoestoque.application.port.EstoqueDaOrdemDeServico;
import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import br.com.grupo117.oficina.catalogoestoque.application.port.PublicadorDeEventos;
import br.com.grupo117.oficina.catalogoestoque.application.port.ReservaDeEstoqueRepositorio;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import br.com.grupo117.oficina.catalogoestoque.domain.ReservaDeEstoque;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Reserva pecas para uma OS e, ao iniciar a execucao, efetiva a baixa consumindo as
 * reservas: a peca sai do estoque na baixa, nao no orcamento. Envolve mais de um
 * agregado, entao a infraestrutura deve executar cada operacao em uma transacao.
 */
public final class MovimentarEstoqueDaOrdemDeServico implements EstoqueDaOrdemDeServico {

    private final PecaRepositorio pecas;
    private final ReservaDeEstoqueRepositorio reservas;
    private final PublicadorDeEventos eventos;

    public MovimentarEstoqueDaOrdemDeServico(
            PecaRepositorio pecas,
            ReservaDeEstoqueRepositorio reservas,
            PublicadorDeEventos eventos
    ) {
        this.pecas = Objects.requireNonNull(pecas);
        this.reservas = Objects.requireNonNull(reservas);
        this.eventos = Objects.requireNonNull(eventos);
    }

    @Override
    public UUID reservar(UUID ordemDeServicoId, UUID pecaId, int quantidade) {
        Objects.requireNonNull(ordemDeServicoId, "Identificador da OS e obrigatorio");
        Peca peca = buscarPeca(pecaId);
        ReservaDeEstoque reserva = peca.reservar(ordemDeServicoId, quantidade);
        pecas.salvar(peca);
        reservas.salvar(reserva);
        publicarEventos(peca);
        return reserva.id();
    }

    @Override
    public int baixarReservasDaOrdemDeServico(UUID ordemDeServicoId) {
        Objects.requireNonNull(ordemDeServicoId, "Identificador da OS e obrigatorio");
        List<ReservaDeEstoque> ativas = reservas.buscarAtivasPorOrdemDeServico(ordemDeServicoId);
        for (ReservaDeEstoque reserva : ativas) {
            Peca peca = buscarPeca(reserva.pecaId());
            peca.baixar(reserva);
            pecas.salvar(peca);
            reservas.salvar(reserva);
            publicarEventos(peca);
        }
        return ativas.size();
    }

    private Peca buscarPeca(UUID pecaId) {
        Objects.requireNonNull(pecaId, "Identificador da peca e obrigatorio");
        return pecas.buscarPorId(pecaId).orElseThrow(() -> new PecaNaoEncontradaException(pecaId));
    }

    private void publicarEventos(Peca peca) {
        eventos.publicar(peca.eventos());
        peca.limparEventos();
    }
}
