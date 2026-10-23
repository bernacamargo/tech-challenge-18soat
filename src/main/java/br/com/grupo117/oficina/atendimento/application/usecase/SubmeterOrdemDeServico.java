package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.util.Objects;
import java.util.UUID;

/** Mecânico submete a OS; a política calcula o orçamento e envia para aprovação. */
public final class SubmeterOrdemDeServico {
    private final OrdemDeServicoRepositorio ordens;

    public SubmeterOrdemDeServico(OrdemDeServicoRepositorio ordens) {
        this.ordens = Objects.requireNonNull(ordens);
    }

    public OrdemDeServico submeter(UUID id) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        OrdemDeServico os = ordens.buscarPorId(id)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        os.submeter();
        return ordens.salvar(os);
    }
}
