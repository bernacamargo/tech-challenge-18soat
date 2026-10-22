package br.com.grupo117.oficina.catalogoestoque.infrastructure.api;

import br.com.grupo117.oficina.catalogoestoque.domain.Servico;
import java.math.BigDecimal;
import java.util.UUID;

public record ServicoResponse(UUID id, String nome, String descricao, BigDecimal preco) {

    public static ServicoResponse de(Servico servico) {
        return new ServicoResponse(servico.id(), servico.nome(), servico.descricao(), servico.preco().valor());
    }
}
