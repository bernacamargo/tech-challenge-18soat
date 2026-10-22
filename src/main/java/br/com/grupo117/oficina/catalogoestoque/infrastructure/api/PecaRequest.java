package br.com.grupo117.oficina.catalogoestoque.infrastructure.api;

import br.com.grupo117.oficina.catalogoestoque.domain.TipoItemEstoque;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record PecaRequest(@NotBlank String nome, String descricao, @NotNull TipoItemEstoque tipo,
        @NotNull @Positive BigDecimal precoUnitario, @NotNull @PositiveOrZero Integer quantidadeInicial) {}
