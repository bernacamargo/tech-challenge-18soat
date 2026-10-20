package br.com.grupo117.oficina.atendimento.domain.valueobject;

import java.time.Instant;
import java.util.Objects;

public record RejeicaoOrcamento(Instant momento, String motivo) {
    public RejeicaoOrcamento {
        Objects.requireNonNull(momento, "Momento da rejeição é obrigatório");
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Motivo da rejeição é obrigatório");
        }
        motivo = motivo.trim();
    }
}
