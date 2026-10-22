package br.com.grupo117.oficina.catalogoestoque.infrastructure.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EntradaNoEstoqueRequest(@NotNull @Positive Integer quantidade) {}
