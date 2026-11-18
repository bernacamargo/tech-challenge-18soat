package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.util.List;
import java.util.Comparator;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import java.util.Objects;

public final class ListarOrdensDeServico {
    private final OrdemDeServicoRepositorio ordens;

    public ListarOrdensDeServico(OrdemDeServicoRepositorio ordens) {
        this.ordens = Objects.requireNonNull(ordens);
    }

    public List<OrdemDeServico> listar() {
        return ordens.listar().stream()
                .filter(os -> os.status() != StatusOS.FINALIZADA && os.status() != StatusOS.ENTREGUE)
                .sorted(Comparator.comparingInt((OrdemDeServico os) -> prioridade(os.status()))
                        .thenComparing(os -> os.abertaEm().orElse(null),
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(os -> os.id().toString()))
                .toList();
    }

    private static int prioridade(StatusOS status) {
        return switch (status) {
            case EM_EXECUCAO -> 0;
            case AGUARDANDO_APROVACAO -> 1;
            case EM_DIAGNOSTICO -> 2;
            case RECEBIDA -> 3;
            case FINALIZADA, ENTREGUE -> 4;
        };
    }
}
