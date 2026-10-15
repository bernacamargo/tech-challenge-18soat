package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import java.time.Instant;

public record AcompanhamentoOrdemDeServicoResponse(StatusOS status, Instant finalizadaEm) {
    public static AcompanhamentoOrdemDeServicoResponse de(OrdemDeServico os) {
        return new AcompanhamentoOrdemDeServicoResponse(os.status(), os.finalizadaEm().orElse(null));
    }
}
