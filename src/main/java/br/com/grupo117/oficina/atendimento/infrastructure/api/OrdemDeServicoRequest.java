package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.application.usecase.CriarOrdemDeServico.ItemInicial;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public record OrdemDeServicoRequest(@NotBlank String documento, @NotBlank @Size(max = 150) String nome,
        @NotBlank String placa, @NotBlank @Size(max = 80) String marca, @NotBlank @Size(max = 80) String modelo, @NotNull Integer ano,
        @Schema(description = "Formato de item único. Use sem as listas.") UUID servicoId,
        @Schema(description = "Formato de item único. Use sem as listas.") UUID pecaId,
        @Schema(description = "Quantidade no formato de item único.") Integer quantidade,
        @Email @Size(max = 254)
        @Pattern(regexp = "[^\\s@<>]++@[^\\s@<>.]++(?:\\.[^\\s@<>.]++)++")
        @Schema(description = "Contato para envio do orçamento; preenche apenas cadastro novo ou sem e-mail")
        String email,
        @Schema(description = "Serviços iniciais, na ordem informada.")
        List<@NotNull @Valid ServicoDaOSRequest> servicos,
        @Schema(description = "Peças iniciais, na ordem informada, com reserva de estoque.")
        List<@NotNull @Valid PecaDaOSRequest> pecas) {

    public OrdemDeServicoRequest(String documento, String nome, String placa, String marca, String modelo, Integer ano,
                                  UUID servicoId, UUID pecaId, Integer quantidade, String email) {
        this(documento, nome, placa, marca, modelo, ano, servicoId, pecaId, quantidade, email, null, null);
    }

    public OrdemDeServicoRequest(String documento, String nome, String placa, String marca, String modelo, Integer ano,
                                  UUID servicoId, UUID pecaId, Integer quantidade) {
        this(documento, nome, placa, marca, modelo, ano, servicoId, pecaId, quantidade, null);
    }

    public OrdemDeServicoRequest(String documento, String nome, String placa, String marca, String modelo, Integer ano) {
        this(documento, nome, placa, marca, modelo, ano, null, null, null, null);
    }

    boolean usaListasDeItens() {
        return servicos != null || pecas != null;
    }

    List<ItemInicial> itensIniciais() {
        if (servicoId != null || pecaId != null || quantidade != null) {
            throw new IllegalArgumentException("Envie listas de itens ou os campos de item único");
        }
        var itensServico = servicos == null ? Stream.<ServicoDaOSRequest>empty() : servicos.stream();
        var itensPeca = pecas == null ? Stream.<PecaDaOSRequest>empty() : pecas.stream();
        return Stream.concat(
                itensServico.map(item -> new ItemInicial(TipoItemOS.SERVICO, item.servicoId(), item.quantidade())),
                itensPeca.map(item -> new ItemInicial(TipoItemOS.PECA, item.pecaId(), item.quantidade()))
        ).toList();
    }
}
