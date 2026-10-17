package br.com.grupo117.oficina.catalogoestoque.application;

import br.com.grupo117.oficina.catalogoestoque.domain.Servico;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Cadastra um servico novo no catalogo. O preco invalido e rejeitado pelo dominio.
 */
public final class CadastrarServico {

    private final ServicoRepositorio servicos;

    public CadastrarServico(ServicoRepositorio servicos) {
        this.servicos = Objects.requireNonNull(servicos, "Repositorio de servicos e obrigatorio");
    }

    public Servico cadastrar(String nome, String descricao, BigDecimal preco) {
        return servicos.salvar(Servico.cadastrar(nome, descricao, preco));
    }
}
