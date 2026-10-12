package br.com.grupo117.oficina.atendimento.infrastructure.api;

import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
import java.math.BigDecimal;
import java.util.UUID;

public record ItemDaOSResponse(UUID id, UUID catalogoId, TipoItemOS tipo, String descricao,
                               int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
    public static ItemDaOSResponse de(ItemDaOS item) {
        return new ItemDaOSResponse(item.id(), item.catalogoId(), item.tipo(), item.descricao(),
                item.quantidade(), item.precoUnitario().valor(), item.subtotal().valor());
    }
}
