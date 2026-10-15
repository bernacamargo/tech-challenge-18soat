package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import java.time.Instant;
import java.util.UUID;

public record OrdemDeServicoResponse(UUID id, String clienteId, String veiculoId,
                                      StatusOS status, Instant finalizadaEm) {
    public static OrdemDeServicoResponse de(OrdemDeServico os) {
        return new OrdemDeServicoResponse(os.id(), os.clienteId(), os.veiculoId(),
                os.status(), os.finalizadaEm().orElse(null));
    }
}
