package br.com.grupo117.oficina.atendimento.infrastructure.integration;

import br.com.grupo117.oficina.atendimento.application.port.BaixaEstoquePort;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.application.port.ReservaEstoquePort;
import br.com.grupo117.oficina.atendimento.application.usecase.OrdemDeServicoNaoEncontradaException;
import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
import br.com.grupo117.oficina.atendimento.infrastructure.persistence.OperacaoOSLock;
import br.com.grupo117.oficina.atendimento.infrastructure.persistence.ReservaItemOSRepository;
import br.com.grupo117.oficina.atendimento.infrastructure.persistence.ReservaItemOSRepository.Reserva;
import br.com.grupo117.oficina.atendimento.infrastructure.persistence.ReservaItemOSRepository.Vinculo;
import br.com.grupo117.oficina.catalogoestoque.application.port.EstoqueDaOrdemDeServico;
import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class EstoqueOSAdapter implements ReservaEstoquePort, BaixaEstoquePort {
    private final EstoqueDaOrdemDeServico estoque;
    private final PecaRepositorio pecas;
    private final OrdemDeServicoRepositorio ordens;
    private final ReservaItemOSRepository vinculos;
    private final OperacaoOSLock locks;

    public EstoqueOSAdapter(EstoqueDaOrdemDeServico estoque, PecaRepositorio pecas,
                            OrdemDeServicoRepositorio ordens, ReservaItemOSRepository vinculos, OperacaoOSLock locks) {
        this.estoque = estoque;
        this.pecas = pecas;
        this.ordens = ordens;
        this.vinculos = vinculos;
        this.locks = locks;
    }

    @Override
    public boolean reservar(UUID osId, UUID itemId, UUID pecaId, int quantidade) {
        if (quantidade <= 0) throw new IllegalArgumentException("Quantidade deve ser positiva");
        locks.bloquearOrdem(osId);
        var existente = vinculos.buscarPorItem(itemId);
        if (existente.isPresent()) {
            Vinculo v = existente.get();
            if (!v.osId().equals(osId) || !v.pecaId().equals(pecaId) || v.quantidade() != quantidade) {
                throw inconsistente();
            }
            boolean confirmada = vinculos.buscarReservas(osId).stream().anyMatch(r ->
                    r.id().equals(v.reservaId()) && r.pecaId().equals(pecaId) && r.quantidade() == quantidade
                            && (r.status().equals("ATIVA") || r.status().equals("BAIXADA")));
            if (!confirmada) throw inconsistente();
            return true;
        }
        locks.bloquearPeca(pecaId);
        var peca = pecas.buscarPorId(pecaId).orElseThrow(EstoqueOSAdapter::inconsistente);
        // O lock torna esta verificação atômica com a reserva subsequente.
        if (peca.quantidadeLivre() < quantidade) return false;
        UUID reservaId = estoque.reservar(osId, pecaId, quantidade);
        vinculos.salvar(new Vinculo(itemId, osId, pecaId, quantidade, reservaId));
        return true;
    }

    @Override
    public void baixarReservas(UUID osId) {
        locks.bloquearOrdem(osId);
        var os = ordens.buscarPorId(osId).orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(osId));
        List<ItemDaOS> itens = os.itens().stream().filter(i -> i.tipo() == TipoItemOS.PECA).toList();
        itens.stream().map(ItemDaOS::catalogoId).distinct().sorted(java.util.Comparator.comparing(UUID::toString))
                .forEach(locks::bloquearPeca);
        List<Vinculo> links = vinculos.buscarPorOrdem(osId);
        List<Reserva> reservas = vinculos.buscarReservas(osId);
        validarCorrespondencia(itens, links, reservas);
        if (reservas.isEmpty() || reservas.stream().allMatch(r -> r.status().equals("BAIXADA"))) return;
        if (!reservas.stream().allMatch(r -> r.status().equals("ATIVA"))) throw inconsistente();
        int baixadas = estoque.baixarReservasDaOrdemDeServico(osId);
        if (baixadas != reservas.size()) throw inconsistente();
        List<Reserva> depois = vinculos.buscarReservas(osId);
        if (depois.size() != reservas.size() || !depois.stream().allMatch(r -> r.status().equals("BAIXADA"))) {
            throw inconsistente();
        }
    }

    private void validarCorrespondencia(List<ItemDaOS> itens, List<Vinculo> links, List<Reserva> reservas) {
        if (itens.size() != links.size() || links.size() != reservas.size()) throw inconsistente();
        Map<UUID, Vinculo> porItem = links.stream().collect(Collectors.toMap(Vinculo::itemId, Function.identity()));
        Map<UUID, Reserva> porReserva = reservas.stream().collect(Collectors.toMap(Reserva::id, Function.identity()));
        for (ItemDaOS item : itens) {
            Vinculo v = porItem.get(item.id());
            if (v == null || !v.pecaId().equals(item.catalogoId()) || v.quantidade() != item.quantidade()) throw inconsistente();
            Reserva r = porReserva.get(v.reservaId());
            if (r == null || !r.pecaId().equals(v.pecaId()) || r.quantidade() != v.quantidade()) throw inconsistente();
        }
    }

    private static IllegalStateException inconsistente() {
        return new IllegalStateException("Reservas de estoque não correspondem aos itens da OS");
    }
}
