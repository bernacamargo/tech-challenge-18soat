package br.com.grupo117.oficina.catalogoestoque.infrastructure.api;

import br.com.grupo117.oficina.catalogoestoque.domain.TipoItemEstoque;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Dados cadastrais da peca; a quantidade so muda por entrada, reserva e baixa. */
public record AlterarPecaRequest(@NotBlank @Size(max = 150) String nome, @Size(max = 500) String descricao, @NotNull TipoItemEstoque tipo,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal precoUnitario) {}
