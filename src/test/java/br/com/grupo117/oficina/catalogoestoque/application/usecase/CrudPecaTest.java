package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.grupo117.oficina.catalogoestoque.domain.Dinheiro;
import br.com.grupo117.oficina.catalogoestoque.domain.DinheiroInvalidoException;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import br.com.grupo117.oficina.catalogoestoque.domain.TipoItemEstoque;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrudPecaTest {

    private static final BigDecimal PRECO = new BigDecimal("35.90");

    private static Peca filtroDeOleo() {
        return Peca.cadastrar("Filtro de oleo", null, TipoItemEstoque.PECA, Dinheiro.de(PRECO), 5);
    }

    @Test
    void cadastraPecaESalvaNoRepositorio() {
        PecaRepositorioEmMemoria repo = new PecaRepositorioEmMemoria();

        Peca peca = new CadastrarPeca(repo).cadastrar("Oleo 5W30", "Frasco 1L", TipoItemEstoque.INSUMO, PRECO, 10);

        assertThat(repo.buscarPorId(peca.id())).containsSame(peca);
        assertThat(peca.nome()).isEqualTo("Oleo 5W30");
        assertThat(peca.precoUnitario()).isEqualTo(Dinheiro.de("35.90"));
        assertThat(peca.quantidadeDisponivel()).isEqualTo(10);
    }

    @Test
    void naoSalvaCadastroInvalido() {
        PecaRepositorioEmMemoria repo = new PecaRepositorioEmMemoria();
        CadastrarPeca cadastrar = new CadastrarPeca(repo);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> cadastrar.cadastrar(" ", null, TipoItemEstoque.PECA, PRECO, 1));
        assertThatThrownBy(() -> cadastrar.cadastrar("Vela", null, TipoItemEstoque.PECA, null, 1))
                .isInstanceOf(DinheiroInvalidoException.class);
        assertThat(repo.salvamentos()).isZero();
    }

    @Test
    void listaEDetalhaPecasExistentes() {
        Peca filtro = filtroDeOleo();
        Peca vela = Peca.cadastrar("Vela", null, TipoItemEstoque.PECA, Dinheiro.de("20"), 4);
        PecaRepositorioEmMemoria repo = new PecaRepositorioEmMemoria().com(filtro, vela);

        assertThat(new ListarPecas(repo).listar()).containsExactly(filtro, vela);
        assertThat(new DetalharPeca(repo).detalhar(vela.id())).isSameAs(vela);
    }

    @Test
    void listaVaziaEPecaAusente() {
        PecaRepositorioEmMemoria repo = new PecaRepositorioEmMemoria();
        DetalharPeca detalhar = new DetalharPeca(repo);
        UUID id = UUID.randomUUID();

        assertThat(new ListarPecas(repo).listar()).isEmpty();
        assertThatThrownBy(() -> detalhar.detalhar(id))
                .isInstanceOf(PecaNaoEncontradaException.class)
                .hasMessage("Peca nao encontrada: " + id);
        assertThatNullPointerException().isThrownBy(() -> detalhar.detalhar(null));
    }

    @Test
    void alteraDadosCadastraisSemMexerNoEstoque() {
        Peca filtro = filtroDeOleo();
        PecaRepositorioEmMemoria repo = new PecaRepositorioEmMemoria().com(filtro);

        Peca alterada = new AlterarPeca(repo)
                .alterar(filtro.id(), "Filtro de ar", "Motor 1.6", TipoItemEstoque.PECA, new BigDecimal("42"));

        assertThat(alterada.nome()).isEqualTo("Filtro de ar");
        assertThat(alterada.descricao()).isEqualTo("Motor 1.6");
        assertThat(alterada.precoUnitario()).isEqualTo(Dinheiro.de("42.00"));
        assertThat(alterada.quantidadeDisponivel()).isEqualTo(5);
        assertThat(repo.salvamentos()).isEqualTo(1);
    }

    @Test
    void naoAlteraPecaAusenteOuComDadosInvalidos() {
        Peca filtro = filtroDeOleo();
        PecaRepositorioEmMemoria repo = new PecaRepositorioEmMemoria().com(filtro);
        AlterarPeca alterar = new AlterarPeca(repo);
        UUID ausente = UUID.randomUUID();
        UUID id = filtro.id();

        assertThatThrownBy(() -> alterar.alterar(ausente, "Filtro", null, TipoItemEstoque.PECA, PRECO))
                .isInstanceOf(PecaNaoEncontradaException.class);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> alterar.alterar(id, " ", null, TipoItemEstoque.PECA, PRECO));
        assertThatNullPointerException()
                .isThrownBy(() -> alterar.alterar(null, "Filtro", null, TipoItemEstoque.PECA, PRECO));
        assertThat(filtro.nome()).isEqualTo("Filtro de oleo");
        assertThat(repo.salvamentos()).isZero();
    }

    @Test
    void removePecaExistente() {
        Peca filtro = filtroDeOleo();
        PecaRepositorioEmMemoria repo = new PecaRepositorioEmMemoria().com(filtro);

        new RemoverPeca(repo).remover(filtro.id());

        assertThat(repo.buscarPorId(filtro.id())).isEmpty();
    }

    @Test
    void naoRemovePecaAusente() {
        Peca filtro = filtroDeOleo();
        PecaRepositorioEmMemoria repo = new PecaRepositorioEmMemoria().com(filtro);
        RemoverPeca remover = new RemoverPeca(repo);
        UUID ausente = UUID.randomUUID();

        assertThatThrownBy(() -> remover.remover(ausente)).isInstanceOf(PecaNaoEncontradaException.class);
        assertThatNullPointerException().isThrownBy(() -> remover.remover(null));
        assertThat(repo.listar()).containsExactly(filtro);
    }

    @Test
    void exigeRepositorio() {
        assertThatNullPointerException().isThrownBy(() -> new CadastrarPeca(null));
        assertThatNullPointerException().isThrownBy(() -> new ListarPecas(null));
        assertThatNullPointerException().isThrownBy(() -> new DetalharPeca(null));
        assertThatNullPointerException().isThrownBy(() -> new AlterarPeca(null));
        assertThatNullPointerException().isThrownBy(() -> new RemoverPeca(null));
    }
}
