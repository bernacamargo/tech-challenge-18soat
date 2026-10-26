package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.application.port.NotificacaoPort;
import java.util.Objects;
import java.util.UUID;

/** Mecânico submete a OS; a política calcula o orçamento e envia para aprovação. */
public final class SubmeterOrdemDeServico {
    private final OrdemDeServicoRepositorio ordens;
    private final NotificacaoPort notificacoes;

    public SubmeterOrdemDeServico(OrdemDeServicoRepositorio ordens) { this(ordens, os -> { }); }

    public SubmeterOrdemDeServico(OrdemDeServicoRepositorio ordens, NotificacaoPort notificacoes) {
        this.notificacoes = Objects.requireNonNull(notificacoes);
        this.ordens = Objects.requireNonNull(ordens);
    }

    public OrdemDeServico submeter(UUID id) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        OrdemDeServico os = ordens.buscarPorId(id)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        os.submeter();
        OrdemDeServico salva = ordens.salvar(os);
        notificacoes.agendarOrcamento(salva);
        return salva;
    }
}
