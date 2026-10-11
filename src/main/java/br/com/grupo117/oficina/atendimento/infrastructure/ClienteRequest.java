package br.com.grupo117.oficina.atendimento.infrastructure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "Nome do cliente e obrigatorio")
        @Size(max = 150, message = "Nome do cliente deve ter no maximo 150 caracteres")
        String nome,
        @NotBlank(message = "CPF/CNPJ e obrigatorio")
        String cpfCnpj
) {
}
