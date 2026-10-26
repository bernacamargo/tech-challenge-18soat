package br.com.grupo117.oficina.atendimento.infrastructure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record AlterarClienteRequest(
        @NotBlank(message = "Nome do cliente e obrigatorio")
        @Size(max = 150, message = "Nome do cliente deve ter no maximo 150 caracteres")
        String nome,
        @Email @Size(max = 254) @Pattern(regexp = "[^\\s@<>]+@[^\\s@<>.]+(?:\\.[^\\s@<>.]+)+")
        @Schema(description = "E-mail para envio do orçamento. Opcional para compatibilidade; sem contato o envio aguarda o cadastro.")
        String email
) {
    public AlterarClienteRequest(String nome) { this(nome, null); }
}
