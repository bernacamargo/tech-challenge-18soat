package br.com.grupo117.oficina.catalogoestoque.application;

import br.com.grupo117.oficina.catalogoestoque.domain.Servico;
import java.util.List;
import java.util.Objects;

/**
 * Lista os servicos cadastrados no catalogo.
 */
public final class ListarServicos {

    private final ServicoRepositorio servicos;

    public ListarServicos(ServicoRepositorio servicos) {
        this.servicos = Objects.requireNonNull(servicos, "Repositorio de servicos e obrigatorio");
    }

    public List<Servico> listar() {
        return servicos.listar();
    }
}
