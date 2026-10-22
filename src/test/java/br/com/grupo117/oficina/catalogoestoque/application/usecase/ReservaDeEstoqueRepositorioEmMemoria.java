package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import br.com.grupo117.oficina.catalogoestoque.application.port.ReservaDeEstoqueRepositorio;
import br.com.grupo117.oficina.catalogoestoque.domain.ReservaDeEstoque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Fake do port de reservas para testar os casos de uso sem Spring/JPA. */
final class ReservaDeEstoqueRepositorioEmMemoria implements ReservaDeEstoqueRepositorio {

    private final Map<UUID, ReservaDeEstoque> reservas = new LinkedHashMap<>();

    Optional<ReservaDeEstoque> buscarPorId(UUID id) {
        return Optional.ofNullable(reservas.get(id));
    }

    int quantidade() {
        return reservas.size();
    }

    @Override
    public ReservaDeEstoque salvar(ReservaDeEstoque reserva) {
        reservas.put(reserva.id(), reserva);
        return reserva;
    }

    @Override
    public List<ReservaDeEstoque> buscarAtivasPorOrdemDeServico(UUID ordemDeServicoId) {
        return reservas.values().stream()
                .filter(reserva -> reserva.ordemDeServicoId().equals(ordemDeServicoId))
                .filter(ReservaDeEstoque::ativa)
                .toList();
    }
}
