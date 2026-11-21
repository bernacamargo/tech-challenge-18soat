package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.NotificacaoPort;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import java.util.Objects;
import java.util.UUID;

public final class RegistrarEntrega {

    private final OrdemDeServicoRepositorio ordens;
    private final NotificacaoPort notificacoes;

    public RegistrarEntrega(OrdemDeServicoRepositorio ordens) {
        this(ordens, os -> { });
    }

    public RegistrarEntrega(OrdemDeServicoRepositorio ordens, NotificacaoPort notificacoes) {
        this.ordens = Objects.requireNonNull(ordens);
        this.notificacoes = Objects.requireNonNull(notificacoes);
    }

    public OrdemDeServico registrar(UUID id) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        OrdemDeServico os = ordens.buscarPorId(id)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        os.transicionarPara(StatusOS.ENTREGUE);
        OrdemDeServico salva = ordens.salvar(os);
        notificacoes.agendar(salva);
        return salva;
    }
}
