package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import java.util.Objects;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;

public final class ConcluirServicos {

    private final OrdemDeServicoRepositorio ordens;
    private final Clock clock;

    public ConcluirServicos(OrdemDeServicoRepositorio ordens, Clock clock) {
        this.ordens = Objects.requireNonNull(ordens);
        this.clock = Objects.requireNonNull(clock);
    }

    public OrdemDeServico concluir(UUID id) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        OrdemDeServico os = ordens.buscarPorId(id)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        os.transicionarPara(StatusOS.FINALIZADA, Instant.now(clock));
        return ordens.salvar(os);
    }
}
