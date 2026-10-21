package br.com.grupo117.oficina.atendimento.domain;

import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import java.util.Objects;
import java.util.UUID;

/** Item identificado e imutável: guarda o preço vigente no momento da inclusão. */
public record ItemDaOS(UUID id, UUID catalogoId, TipoItemOS tipo, String descricao,
                       int quantidade, Dinheiro precoUnitario) {
    public ItemDaOS {
        Objects.requireNonNull(id, "Identificador do item é obrigatório");
        Objects.requireNonNull(catalogoId, "Identificador do catálogo é obrigatório");
        Objects.requireNonNull(tipo, "Tipo do item é obrigatório");
        Objects.requireNonNull(precoUnitario, "Preço unitário é obrigatório");
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição é obrigatória");
        }
        descricao = descricao.trim();
        if (quantidade <= 0 || precoUnitario.valor().signum() <= 0) {
            throw new IllegalArgumentException("Quantidade e preço unitário devem ser positivos");
        }
    }

    public Dinheiro subtotal() { return precoUnitario.multiplicar(quantidade); }
}
