package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.CatalogoOSPort;
import br.com.grupo117.oficina.atendimento.application.port.ItemCatalogo;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.application.port.ReservaEstoquePort;
import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import java.util.Objects;
import java.util.UUID;

public final class IncluirPecaNaOS {
    private final OrdemDeServicoRepositorio ordens;
    private final CatalogoOSPort catalogo;
    private final ReservaEstoquePort estoque;

    public IncluirPecaNaOS(OrdemDeServicoRepositorio ordens, CatalogoOSPort catalogo, ReservaEstoquePort estoque) {
        this.ordens = Objects.requireNonNull(ordens);
        this.catalogo = Objects.requireNonNull(catalogo);
        this.estoque = Objects.requireNonNull(estoque);
    }

    public OrdemDeServico incluir(UUID osId, UUID catalogoId, int quantidade) {
        Objects.requireNonNull(osId, "Identificador da OS é obrigatório");
        Objects.requireNonNull(catalogoId, "Identificador do catálogo é obrigatório");
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        OrdemDeServico os = ordens.buscarPorId(osId)
                .orElseThrow(() -> new OrdemDeServicoNaoEncontradaException(osId));
        os.validarInclusaoDeItem();
        ItemCatalogo dados = catalogo.buscarPeca(catalogoId)
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado no catálogo: " + catalogoId));
        if (!catalogoId.equals(dados.id())) {
            throw new IllegalArgumentException("Catálogo retornou identificador divergente");
        }
        ItemDaOS item = new ItemDaOS(UUID.randomUUID(), catalogoId, TipoItemOS.PECA,
                dados.descricao(), quantidade, new Dinheiro(dados.preco()));
        if (!estoque.reservar(osId, item.id(), catalogoId, quantidade)) {
            throw new IllegalStateException("Estoque insuficiente para a peça: " + catalogoId);
        }
        os.incluirItem(item);
        return ordens.salvar(os);
    }
}
