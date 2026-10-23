package br.com.grupo117.oficina.atendimento.infrastructure.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrdemDeServicoRequest(@NotBlank String documento, @NotBlank String nome,
        @NotBlank String placa, @NotBlank String marca, @NotBlank String modelo, @NotNull Integer ano,
        UUID servicoId, UUID pecaId, Integer quantidade) {

    public OrdemDeServicoRequest(String documento, String nome, String placa, String marca, String modelo, Integer ano) {
        this(documento, nome, placa, marca, modelo, ano, null, null, null);
    }
}
