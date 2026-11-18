package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.ItemCatalogo;
import br.com.grupo117.oficina.atendimento.application.port.ReservaEstoquePort;
import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import java.util.Optional;
import java.util.UUID;

final class ItensDoCatalogo {

    private ItensDoCatalogo() {
    }

    static ItemCatalogo exigirItem(Optional<ItemCatalogo> resultado, UUID catalogoId) {
        return resultado.orElseThrow(
                () -> new IllegalArgumentException("Item não encontrado no catálogo: " + catalogoId));
    }

    static ItemDaOS criar(ItemCatalogo dados, UUID catalogoId, TipoItemOS tipo, int quantidade) {
        return new ItemDaOS(UUID.randomUUID(), catalogoId, tipo, dados.descricao(), quantidade,
                new Dinheiro(dados.preco()));
    }

    static void reservarPeca(ReservaEstoquePort estoque, UUID osId, ItemDaOS item) {
        UUID pecaId = item.catalogoId();
        if (!estoque.reservar(osId, item.id(), pecaId, item.quantidade())) {
            throw new IllegalStateException("Estoque insuficiente para a peça: " + pecaId);
        }
    }
}
