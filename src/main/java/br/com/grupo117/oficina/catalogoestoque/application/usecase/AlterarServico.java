package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import br.com.grupo117.oficina.catalogoestoque.application.port.ServicoRepositorio;
import br.com.grupo117.oficina.catalogoestoque.domain.Servico;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Altera nome, descricao e preco de um servico ja cadastrado. O id permanece.
 */
public final class AlterarServico {

    private final ServicoRepositorio servicos;

    public AlterarServico(ServicoRepositorio servicos) {
        this.servicos = Objects.requireNonNull(servicos, "Repositorio de servicos e obrigatorio");
    }

    public Servico alterar(UUID id, String nome, String descricao, BigDecimal preco) {
        Servico atual = servicos.buscarPorId(id).orElseThrow(() -> new ServicoNaoEncontradoException(id));
        return servicos.salvar(atual.alterar(nome, descricao, preco));
    }
}
