package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.CatalogoOSPort;
import br.com.grupo117.oficina.atendimento.application.port.ItemCatalogo;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.application.port.ReservaEstoquePort;
import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
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
        ItemCatalogo dados = ItensDoCatalogo.exigirItem(catalogo.buscarPeca(catalogoId), catalogoId);
        if (!catalogoId.equals(dados.id())) {
            throw new IllegalArgumentException("Catálogo retornou identificador divergente");
        }
        ItemDaOS item = ItensDoCatalogo.criar(dados, catalogoId, TipoItemOS.PECA, quantidade);
        ItensDoCatalogo.reservarPeca(estoque, osId, item);
        os.incluirItem(item);
        return ordens.salvar(os);
    }
}
