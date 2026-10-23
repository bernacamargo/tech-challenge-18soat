package br.com.grupo117.oficina.atendimento.infrastructure.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record PecaDaOSRequest(@NotNull UUID pecaId, @NotNull @Positive Integer quantidade) {}
