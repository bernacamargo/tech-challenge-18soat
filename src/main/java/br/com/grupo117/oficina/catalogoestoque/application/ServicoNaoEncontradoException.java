package br.com.grupo117.oficina.catalogoestoque.application;

import java.util.UUID;

/**
 * Nenhum servico com o id informado esta cadastrado.
 */
public final class ServicoNaoEncontradoException extends RuntimeException {

    public ServicoNaoEncontradoException(UUID id) {
        super("Servico nao encontrado: " + id);
    }
}
