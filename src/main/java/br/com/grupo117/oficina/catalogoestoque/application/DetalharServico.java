package br.com.grupo117.oficina.catalogoestoque.application;

import br.com.grupo117.oficina.catalogoestoque.domain.Servico;
import java.util.Objects;
import java.util.UUID;

/**
 * Devolve o servico identificado pelo id.
 */
public final class DetalharServico {

    private final ServicoRepositorio servicos;

    public DetalharServico(ServicoRepositorio servicos) {
        this.servicos = Objects.requireNonNull(servicos, "Repositorio de servicos e obrigatorio");
    }

    public Servico detalhar(UUID id) {
        return servicos.buscarPorId(id).orElseThrow(() -> new ServicoNaoEncontradoException(id));
    }
}
