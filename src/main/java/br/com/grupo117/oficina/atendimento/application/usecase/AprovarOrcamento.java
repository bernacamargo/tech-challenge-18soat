package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.BaixaEstoquePort;
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

    public AprovarOrcamento(OrdemDeServicoRepositorio ordens, BaixaEstoquePort estoque, Clock clock) {
        this.ordens = Objects.requireNonNull(ordens);
        this.estoque = Objects.requireNonNull(estoque);
        this.clock = Objects.requireNonNull(clock);
    }

    public OrdemDeServico aprovar(UUID id) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        OrdemDeServico os = ordens.buscarPorId(id)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        os.validarAprovacaoOrcamento();
        Instant momento = Instant.now(clock);
        estoque.baixarReservas(id);
        os.aprovarOrcamento(momento);
        return ordens.salvar(os);
    }
}
