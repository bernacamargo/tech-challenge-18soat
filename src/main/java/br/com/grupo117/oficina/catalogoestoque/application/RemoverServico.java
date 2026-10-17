package br.com.grupo117.oficina.catalogoestoque.application;

import java.util.Objects;
import java.util.UUID;

/**
 * Remove o servico identificado pelo id.
 */
public final class RemoverServico {

    private final ServicoRepositorio servicos;

    public RemoverServico(ServicoRepositorio servicos) {
        this.servicos = Objects.requireNonNull(servicos, "Repositorio de servicos e obrigatorio");
    }

    public void remover(UUID id) {
        if (servicos.buscarPorId(id).isEmpty()) {
            throw new ServicoNaoEncontradoException(id);
        }
        servicos.remover(id);
    }
}
