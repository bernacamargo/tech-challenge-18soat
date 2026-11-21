package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.BaixaEstoquePort;
import br.com.grupo117.oficina.atendimento.application.port.NotificacaoPort;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AprovarOrcamento {
    private final OrdemDeServicoRepositorio ordens;
    private final BaixaEstoquePort estoque;
    private final Clock clock;
    private final NotificacaoPort notificacoes;

    public AprovarOrcamento(OrdemDeServicoRepositorio ordens, BaixaEstoquePort estoque, Clock clock) {
        this(ordens, estoque, clock, os -> { });
    }

    public AprovarOrcamento(OrdemDeServicoRepositorio ordens, BaixaEstoquePort estoque, Clock clock,
                            NotificacaoPort notificacoes) {
        this.ordens = Objects.requireNonNull(ordens);
        this.estoque = Objects.requireNonNull(estoque);
        this.clock = Objects.requireNonNull(clock);
        this.notificacoes = Objects.requireNonNull(notificacoes);
    }

    public OrdemDeServico aprovar(UUID id) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        OrdemDeServico os = ordens.buscarPorId(id)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        os.validarAprovacaoOrcamento();
        Instant momento = Instant.now(clock);
        estoque.baixarReservas(id);
        os.aprovarOrcamento(momento);
        OrdemDeServico salva = ordens.salvar(os);
        notificacoes.agendar(salva);
        return salva;
    }
}
