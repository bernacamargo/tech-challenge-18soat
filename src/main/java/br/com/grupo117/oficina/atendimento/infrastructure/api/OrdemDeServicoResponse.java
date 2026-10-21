package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import java.util.List;

public record OrdemDeServicoResponse(UUID id, String clienteId, String veiculoId,
                                      StatusOS status, Instant finalizadaEm,
                                      List<ItemDaOSResponse> itens, OrcamentoResponse orcamento,
                                      @Schema(description = "Início da execução em UTC; nulo antes da aprovação ou em registros legados sem início conhecido", nullable = true)
                                      Instant iniciadaEm,
                                      @Schema(description = "Data e motivo da rejeição; nulo quando não houve rejeição", nullable = true)
                                      RejeicaoOrcamentoResponse rejeicaoOrcamento) {
    public static OrdemDeServicoResponse de(OrdemDeServico os) {
        return new OrdemDeServicoResponse(os.id(), os.clienteId(), os.veiculoId(),
                os.status(), os.finalizadaEm().orElse(null),
                os.itens().stream().map(ItemDaOSResponse::de).toList(),
                os.orcamento().map(valor -> new OrcamentoResponse(valor.total().valor())).orElse(null),
                os.iniciadaEm().orElse(null),
                os.rejeicaoOrcamento().map(RejeicaoOrcamentoResponse::de).orElse(null));
    }
}
