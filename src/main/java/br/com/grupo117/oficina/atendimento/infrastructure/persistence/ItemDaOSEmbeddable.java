package br.com.grupo117.oficina.atendimento.infrastructure.persistence;

import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public class ItemDaOSEmbeddable {
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "catalogo_id", nullable = false)
    private UUID catalogoId;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private TipoItemOS tipo;
    @Column(name = "descricao", nullable = false, length = 255)
    private String descricao;
    @Column(name = "quantidade", nullable = false)
    private int quantidade;
    @Column(name = "preco_unitario", nullable = false, precision = 19, scale = 2)
    private BigDecimal precoUnitario;

    protected ItemDaOSEmbeddable() {}

    public static ItemDaOSEmbeddable de(ItemDaOS item) {
        ItemDaOSEmbeddable valor = new ItemDaOSEmbeddable();
        valor.id = item.id();
        valor.catalogoId = item.catalogoId();
        valor.tipo = item.tipo();
        valor.descricao = item.descricao();
        valor.quantidade = item.quantidade();
        valor.precoUnitario = item.precoUnitario().valor();
        return valor;
    }

    public ItemDaOS paraDominio() {
        return new ItemDaOS(id, catalogoId, tipo, descricao, quantidade, new Dinheiro(precoUnitario));
    }
}
