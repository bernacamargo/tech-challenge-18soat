package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import java.util.Objects;
import java.util.UUID;

public final class IniciarDiagnostico {

    private final OrdemDeServicoRepositorio ordens;

    public IniciarDiagnostico(OrdemDeServicoRepositorio ordens) {
        this.ordens = Objects.requireNonNull(ordens);
    }

    public OrdemDeServico iniciar(UUID id) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        OrdemDeServico os = ordens.buscarPorId(id)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        return ordens.salvar(os);
    }
}
