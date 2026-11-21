package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.NotificacaoPort;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import java.util.Objects;
import java.util.UUID;

public final class IniciarDiagnostico {

    private final OrdemDeServicoRepositorio ordens;
    private final NotificacaoPort notificacoes;

    public IniciarDiagnostico(OrdemDeServicoRepositorio ordens) {
        this(ordens, os -> { });
    }

    public IniciarDiagnostico(OrdemDeServicoRepositorio ordens, NotificacaoPort notificacoes) {
        this.ordens = Objects.requireNonNull(ordens);
        this.notificacoes = Objects.requireNonNull(notificacoes);
    }

    public OrdemDeServico iniciar(UUID id) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        OrdemDeServico os = ordens.buscarPorId(id)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        OrdemDeServico salva = ordens.salvar(os);
        notificacoes.agendar(salva);
        return salva;
    }
}
