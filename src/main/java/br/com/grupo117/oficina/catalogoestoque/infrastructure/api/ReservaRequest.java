package br.com.grupo117.oficina.catalogoestoque.infrastructure.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record ReservaRequest(@NotNull UUID ordemDeServicoId, @NotNull UUID pecaId,
        @NotNull @Positive Integer quantidade) {}
