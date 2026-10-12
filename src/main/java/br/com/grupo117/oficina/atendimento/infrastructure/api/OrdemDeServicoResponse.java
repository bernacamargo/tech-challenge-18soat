package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

public record OrdemDeServicoResponse(UUID id, String clienteId, String veiculoId,
                                      StatusOS status, Instant finalizadaEm,
                                      List<ItemDaOSResponse> itens, OrcamentoResponse orcamento) {
    public static OrdemDeServicoResponse de(OrdemDeServico os) {
        return new OrdemDeServicoResponse(os.id(), os.clienteId(), os.veiculoId(),
                os.status(), os.finalizadaEm().orElse(null),
                os.itens().stream().map(ItemDaOSResponse::de).toList(),
                os.orcamento().map(valor -> new OrcamentoResponse(valor.total().valor())).orElse(null));
    }
}
