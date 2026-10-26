package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.application.port.NotificacaoPort;
import java.util.Objects;
import java.util.UUID;

public final class GerarOrcamento {
    private final OrdemDeServicoRepositorio ordens;
    private final NotificacaoPort notificacoes;

    public GerarOrcamento(OrdemDeServicoRepositorio ordens) { this(ordens, os -> { }); }

    public GerarOrcamento(OrdemDeServicoRepositorio ordens, NotificacaoPort notificacoes) {
        this.notificacoes = Objects.requireNonNull(notificacoes);
        this.ordens = Objects.requireNonNull(ordens);
    }

    public OrdemDeServico gerar(UUID id) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        OrdemDeServico os = ordens.buscarPorId(id)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        os.gerarOrcamento();
        OrdemDeServico salva = ordens.salvar(os);
        notificacoes.agendarOrcamento(salva);
        return salva;
    }
}
