package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RejeitarOrcamento {
    private final OrdemDeServicoRepositorio ordens;
    private final Clock clock;

    public RejeitarOrcamento(OrdemDeServicoRepositorio ordens, Clock clock) {
        this.ordens = Objects.requireNonNull(ordens);
        this.clock = Objects.requireNonNull(clock);
    }

    public OrdemDeServico rejeitar(UUID id, String motivo) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        OrdemDeServico os = ordens.buscarPorId(id)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        os.rejeitarOrcamento(Instant.now(clock), motivo);
        return ordens.salvar(os);
    }
}
