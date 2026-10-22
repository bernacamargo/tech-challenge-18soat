package br.com.grupo117.oficina.catalogoestoque.infrastructure.api;

import br.com.grupo117.oficina.catalogoestoque.domain.TipoItemEstoque;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Dados cadastrais da peca; a quantidade so muda por entrada, reserva e baixa. */
public record AlterarPecaRequest(@NotBlank String nome, String descricao, @NotNull TipoItemEstoque tipo,
        @NotNull @Positive BigDecimal precoUnitario) {}
