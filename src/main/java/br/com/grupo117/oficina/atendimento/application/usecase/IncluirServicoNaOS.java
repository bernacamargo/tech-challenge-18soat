package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.CatalogoOSPort;
import br.com.grupo117.oficina.atendimento.application.port.ItemCatalogo;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import java.util.Objects;
import java.util.UUID;

public final class IncluirServicoNaOS {
    private final OrdemDeServicoRepositorio ordens;
    private final CatalogoOSPort catalogo;

    public IncluirServicoNaOS(OrdemDeServicoRepositorio ordens, CatalogoOSPort catalogo) {
        this.ordens = Objects.requireNonNull(ordens);
        this.catalogo = Objects.requireNonNull(catalogo);
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
        ItemCatalogo dados = catalogo.buscarServico(catalogoId)
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado no catálogo: " + catalogoId));
        if (!catalogoId.equals(dados.id())) {
            throw new IllegalArgumentException("Catálogo retornou identificador divergente");
        }
        ItemDaOS item = new ItemDaOS(UUID.randomUUID(), catalogoId, TipoItemOS.SERVICO,
                dados.descricao(), quantidade, new Dinheiro(dados.preco()));
        os.incluirItem(item);
        return ordens.salvar(os);
    }
}
