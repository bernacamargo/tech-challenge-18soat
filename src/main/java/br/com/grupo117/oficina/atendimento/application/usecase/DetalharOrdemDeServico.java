package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.util.Objects;
import java.util.UUID;

public final class DetalharOrdemDeServico {
    private final OrdemDeServicoRepositorio ordens;

    public DetalharOrdemDeServico(OrdemDeServicoRepositorio ordens) {
        this.ordens = Objects.requireNonNull(ordens);
    }

    public OrdemDeServico detalhar(UUID id) {
        Objects.requireNonNull(id, "Identificador da OS é obrigatório");
        return ordens.buscarPorId(id).orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(id));
    }
}
