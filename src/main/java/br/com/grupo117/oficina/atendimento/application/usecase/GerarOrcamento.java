package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.util.Objects;
import java.util.UUID;

public final class GerarOrcamento {
    private final OrdemDeServicoRepositorio ordens;

    public GerarOrcamento(OrdemDeServicoRepositorio ordens) {
        this.ordens = Objects.requireNonNull(ordens);
    }

    public OrdemDeServico gerar(UUID id) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        OrdemDeServico os = ordens.buscarPorId(id)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
        os.gerarOrcamento();
        return ordens.salvar(os);
    }
}
