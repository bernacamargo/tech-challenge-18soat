package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import br.com.grupo117.oficina.catalogoestoque.domain.Dinheiro;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import br.com.grupo117.oficina.catalogoestoque.domain.TipoItemEstoque;
import java.math.BigDecimal;
import java.util.Objects;

/** Cadastra uma peca ou insumo no estoque com a quantidade inicial informada. */
public final class CadastrarPeca {

    private final PecaRepositorio pecas;

    public CadastrarPeca(PecaRepositorio pecas) {
        this.pecas = Objects.requireNonNull(pecas);
    }

    public Peca cadastrar(
            String nome,
            String descricao,
            TipoItemEstoque tipo,
            BigDecimal precoUnitario,
            int quantidadeInicial
    ) {
        Peca peca = Peca.cadastrar(nome, descricao, tipo, Dinheiro.de(precoUnitario), quantidadeInicial);
        return pecas.salvar(peca);
    }
}
