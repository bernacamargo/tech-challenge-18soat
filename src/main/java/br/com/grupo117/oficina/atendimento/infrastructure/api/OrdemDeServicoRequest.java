package br.com.grupo117.oficina.atendimento.infrastructure.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrdemDeServicoRequest(@NotBlank String documento, @NotBlank String nome,
        @NotBlank String placa, @NotBlank String marca, @NotBlank String modelo, @NotNull Integer ano,
        UUID servicoId, UUID pecaId, Integer quantidade,
        @Email @Size(max = 254)
        @Pattern(regexp = "[^\\s@<>]+@[^\\s@<>.]+(?:\\.[^\\s@<>.]+)+")
        @Schema(description = "Contato para envio do orçamento; preenche apenas cadastro novo ou sem e-mail")
        String email) {

    public OrdemDeServicoRequest(String documento, String nome, String placa, String marca, String modelo, Integer ano,
                                  UUID servicoId, UUID pecaId, Integer quantidade) {
        this(documento, nome, placa, marca, modelo, ano, servicoId, pecaId, quantidade, null);
    }

    public OrdemDeServicoRequest(String documento, String nome, String placa, String marca, String modelo, Integer ano) {
        this(documento, nome, placa, marca, modelo, ano, null, null, null, null);
    }
}
