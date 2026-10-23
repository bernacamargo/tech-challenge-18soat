package br.com.grupo117.oficina.atendimento.infrastructure.integration;

import br.com.grupo117.oficina.atendimento.application.port.CatalogoOSPort;
import br.com.grupo117.oficina.atendimento.application.port.ItemCatalogo;
import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import br.com.grupo117.oficina.catalogoestoque.application.port.ServicoRepositorio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CatalogoOSAdapter implements CatalogoOSPort {
    private final ServicoRepositorio servicos;
    private final PecaRepositorio pecas;

    public CatalogoOSAdapter(ServicoRepositorio servicos, PecaRepositorio pecas) {
        this.servicos = servicos;
        this.pecas = pecas;
    }

    @Override
    public Optional<ItemCatalogo> buscarServico(UUID id) {
        return servicos.buscarPorId(id).map(s -> new ItemCatalogo(s.id(), s.nome(), s.preco().valor()));
    }

    @Override
    public Optional<ItemCatalogo> buscarPeca(UUID id) {
        return pecas.buscarPorId(id).map(p -> new ItemCatalogo(p.id(), p.nome(), p.precoUnitario().valor()));
    }
}
