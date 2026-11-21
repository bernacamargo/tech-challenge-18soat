package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.NotificacaoPort;
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
    private final NotificacaoPort notificacoes;

    public ConcluirServicos(OrdemDeServicoRepositorio ordens, Clock clock) {
        this(ordens, clock, os -> { });
    }

    public ConcluirServicos(OrdemDeServicoRepositorio ordens, Clock clock, NotificacaoPort notificacoes) {
        this.ordens = Objects.requireNonNull(ordens);
        this.clock = Objects.requireNonNull(clock);
        this.notificacoes = Objects.requireNonNull(notificacoes);
    }

    public OrdemDeServico concluir(UUID id) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        OrdemDeServico os = ordens.buscarPorId(id)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        os.transicionarPara(StatusOS.FINALIZADA, Instant.now(clock));
        OrdemDeServico salva = ordens.salvar(os);
        notificacoes.agendar(salva);
        return salva;
    }
}
