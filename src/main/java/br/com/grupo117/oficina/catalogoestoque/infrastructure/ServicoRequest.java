package br.com.grupo117.oficina.catalogoestoque.infrastructure;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ServicoRequest(
        @NotBlank(message = "Nome do servico e obrigatorio")
        @Size(max = 120, message = "Nome do servico deve ter no maximo 120 caracteres")
        String nome,
        @Size(max = 500, message = "Descricao do servico deve ter no maximo 500 caracteres")
        String descricao,
        @NotNull(message = "Preco do servico e obrigatorio")
        @DecimalMin(value = "0.01", message = "Preco deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "Preco deve ter no maximo duas casas decimais")
        BigDecimal preco
) {
}
