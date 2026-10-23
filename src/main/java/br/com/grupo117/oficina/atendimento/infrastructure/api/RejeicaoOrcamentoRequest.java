package br.com.grupo117.oficina.atendimento.infrastructure.api;

import jakarta.validation.constraints.NotBlank;

public record RejeicaoOrcamentoRequest(@NotBlank String motivo) {}
