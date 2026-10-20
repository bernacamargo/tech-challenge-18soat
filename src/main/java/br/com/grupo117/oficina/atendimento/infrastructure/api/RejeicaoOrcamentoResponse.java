package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.domain.valueobject.RejeicaoOrcamento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record RejeicaoOrcamentoResponse(
        @Schema(description = "Momento da rejeição em UTC") Instant momento,
        @Schema(description = "Motivo registrado na rejeição") String motivo) {
    public static RejeicaoOrcamentoResponse de(RejeicaoOrcamento rejeicao) {
        return new RejeicaoOrcamentoResponse(rejeicao.momento(), rejeicao.motivo());
    }
}
