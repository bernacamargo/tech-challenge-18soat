package br.com.grupo117.oficina.catalogoestoque.infrastructure.service;

import br.com.grupo117.oficina.catalogoestoque.application.usecase.AlterarPeca;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.CadastrarPeca;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.DarEntradaNoEstoque;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.DetalharPeca;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.ListarPecas;
import br.com.grupo117.oficina.catalogoestoque.application.usecase.RemoverPeca;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import br.com.grupo117.oficina.catalogoestoque.domain.TipoItemEstoque;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fronteira transacional do CRUD e da entrada no estoque; mantem os casos de uso sem Spring. */
@Service
@Transactional
public class PecaService {
    private final CadastrarPeca cadastrar;
    private final ListarPecas listar;
    private final DetalharPeca detalhar;
    private final AlterarPeca alterar;
    private final RemoverPeca remover;
    private final DarEntradaNoEstoque darEntrada;

    public PecaService(CadastrarPeca cadastrar, ListarPecas listar, DetalharPeca detalhar, AlterarPeca alterar,
                       RemoverPeca remover, DarEntradaNoEstoque darEntrada) {
        this.cadastrar = cadastrar;
        this.listar = listar;
        this.detalhar = detalhar;
        this.alterar = alterar;
        this.remover = remover;
        this.darEntrada = darEntrada;
    }

    public Peca cadastrar(String nome, String descricao, TipoItemEstoque tipo, BigDecimal preco, int quantidade) {
        return cadastrar.cadastrar(nome, descricao, tipo, preco, quantidade);
    }

    @Transactional(readOnly = true)
    public List<Peca> listar() { return listar.listar(); }

    @Transactional(readOnly = true)
    public Peca detalhar(UUID id) { return detalhar.detalhar(id); }

    public Peca alterar(UUID id, String nome, String descricao, TipoItemEstoque tipo, BigDecimal preco) {
        return alterar.alterar(id, nome, descricao, tipo, preco);
    }

    public void remover(UUID id) { remover.remover(id); }

    public Peca darEntrada(UUID id, int quantidade) { return darEntrada.darEntrada(id, quantidade); }
}
