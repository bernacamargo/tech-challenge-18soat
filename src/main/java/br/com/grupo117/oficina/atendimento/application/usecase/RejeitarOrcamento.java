package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.DevolucaoEstoquePort;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.valueobject.RejeicaoOrcamento;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RejeitarOrcamento {
    private final OrdemDeServicoRepositorio ordens;
    private final DevolucaoEstoquePort estoque;
    private final Clock clock;

    public RejeitarOrcamento(OrdemDeServicoRepositorio ordens, DevolucaoEstoquePort estoque, Clock clock) {
        this.ordens = Objects.requireNonNull(ordens);
        this.estoque = Objects.requireNonNull(estoque);
        this.clock = Objects.requireNonNull(clock);
    }

    public OrdemDeServico rejeitar(UUID id, String motivo) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        OrdemDeServico os = ordens.buscarPorId(id)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        Instant momento = Instant.now(clock);
        RejeicaoOrcamento rejeicao = new RejeicaoOrcamento(momento, motivo);
        os.validarAprovacaoOrcamento();
        estoque.devolverReservas(id);
        os.rejeitarOrcamento(rejeicao.momento(), rejeicao.motivo());
        return ordens.salvar(os);
    }
}
