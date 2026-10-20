package br.com.grupo117.oficina.atendimento.infrastructure.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrdemDeServicoRequest(@NotBlank String documento, @NotBlank String nome,
        @NotBlank String placa, @NotBlank String marca, @NotBlank String modelo, @NotNull Integer ano) {}
