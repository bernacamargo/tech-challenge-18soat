package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.CancelamentoReservasOSPort;
import br.com.grupo117.oficina.atendimento.application.port.ExclusaoOSPort;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import java.util.Objects;
import java.util.UUID;

public final class ExcluirOrdemDeServico {
    private final OrdemDeServicoRepositorio ordens;
    private final CancelamentoReservasOSPort estoque;
    private final ExclusaoOSPort exclusao;

    public ExcluirOrdemDeServico(OrdemDeServicoRepositorio ordens, CancelamentoReservasOSPort estoque,
                                ExclusaoOSPort exclusao) {
        this.ordens = Objects.requireNonNull(ordens);
        this.estoque = Objects.requireNonNull(estoque);
        this.exclusao = Objects.requireNonNull(exclusao);
    }

    public void excluir(UUID id) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        ordens.buscarPorId(id).orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        estoque.cancelarReservasAtivas(id);
        exclusao.excluir(id);
    }
}
