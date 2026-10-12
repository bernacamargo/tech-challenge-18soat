package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import br.com.grupo117.oficina.catalogoestoque.domain.Dinheiro;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import br.com.grupo117.oficina.catalogoestoque.domain.TipoItemEstoque;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Altera os dados cadastrais da peca. As quantidades em estoque so mudam por
 * entrada, reserva e baixa, nunca por esta alteracao.
 */
public final class AlterarPeca {

    private final PecaRepositorio pecas;

    public AlterarPeca(PecaRepositorio pecas) {
        this.pecas = Objects.requireNonNull(pecas);
    }

    public Peca alterar(UUID id, String nome, String descricao, TipoItemEstoque tipo, BigDecimal precoUnitario) {
        Objects.requireNonNull(id, "Identificador da peca e obrigatorio");
        Peca peca = pecas.buscarPorId(id).orElseThrow(() -> new PecaNaoEncontradaException(id));
        peca.alterarDados(nome, descricao, tipo, Dinheiro.de(precoUnitario));
        return pecas.salvar(peca);
    }
}
