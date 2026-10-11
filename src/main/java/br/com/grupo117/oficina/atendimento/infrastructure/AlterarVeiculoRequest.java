package br.com.grupo117.oficina.atendimento.infrastructure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlterarVeiculoRequest(
        @NotBlank(message = "Marca do veiculo e obrigatoria")
        @Size(max = 80, message = "Marca do veiculo deve ter no maximo 80 caracteres")
        String marca,
        @NotBlank(message = "Modelo do veiculo e obrigatorio")
        @Size(max = 80, message = "Modelo do veiculo deve ter no maximo 80 caracteres")
        String modelo,
        @NotNull(message = "Ano do veiculo e obrigatorio")
        Integer ano
) {
}
