package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.util.List;
import java.util.Objects;

public final class ListarOrdensDeServico {
    private final OrdemDeServicoRepositorio ordens;

    public ListarOrdensDeServico(OrdemDeServicoRepositorio ordens) {
        this.ordens = Objects.requireNonNull(ordens);
    }

    public List<OrdemDeServico> listar() {
        return ordens.listar();
    }
}
