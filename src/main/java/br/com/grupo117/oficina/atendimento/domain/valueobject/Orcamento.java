package br.com.grupo117.oficina.atendimento.domain.valueobject;

import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import java.math.BigDecimal;
import java.util.List;

/** Orçamento imutável, calculado exclusivamente a partir dos preços registrados nos itens. */
public record Orcamento(List<ItemDaOS> itens) {
    public Orcamento {
        itens = List.copyOf(itens);
        if (itens.isEmpty()) {
            throw new IllegalArgumentException("Orçamento deve possuir ao menos um item");
        }
    }

    public Dinheiro total() {
        BigDecimal soma = itens.stream().map(item -> item.subtotal().valor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Dinheiro(soma);
    }
}
