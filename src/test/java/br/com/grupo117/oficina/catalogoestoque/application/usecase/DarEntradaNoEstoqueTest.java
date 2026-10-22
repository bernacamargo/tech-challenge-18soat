package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.grupo117.oficina.catalogoestoque.domain.Dinheiro;
import br.com.grupo117.oficina.catalogoestoque.domain.EstoqueReabastecido;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import br.com.grupo117.oficina.catalogoestoque.domain.QuantidadeInvalidaException;
import br.com.grupo117.oficina.catalogoestoque.domain.TipoItemEstoque;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DarEntradaNoEstoqueTest {

    private final PublicadorDeEventosEmMemoria publicador = new PublicadorDeEventosEmMemoria();

    private static Peca filtroDeOleoCom(int quantidade) {
        Peca peca = Peca.cadastrar("Filtro de oleo", null, TipoItemEstoque.PECA, Dinheiro.de("35.90"), quantidade);
        peca.limparEventos();
        return peca;
    }

    @Test
    void aumentaQuantidadeDisponivelNoValorInformado() {
        Peca filtro = filtroDeOleoCom(2);
        PecaRepositorioEmMemoria repo = new PecaRepositorioEmMemoria().com(filtro);

        Peca atualizada = new DarEntradaNoEstoque(repo, publicador).darEntrada(filtro.id(), 5);

        assertThat(atualizada.quantidadeDisponivel()).isEqualTo(7);
        assertThat(repo.buscarPorId(filtro.id())).get()
                .extracting(Peca::quantidadeDisponivel).isEqualTo(7);
        assertThat(repo.salvamentos()).isEqualTo(1);
    }

    @Test
    void publicaEstoqueReabastecidoUmaUnicaVez() {
        Peca filtro = filtroDeOleoCom(2);
        PecaRepositorioEmMemoria repo = new PecaRepositorioEmMemoria().com(filtro);
        DarEntradaNoEstoque darEntrada = new DarEntradaNoEstoque(repo, publicador);

        darEntrada.darEntrada(filtro.id(), 5);

        assertThat(publicador.publicados()).singleElement()
                .isInstanceOfSatisfying(EstoqueReabastecido.class, evento -> {
                    assertThat(evento.pecaId()).isEqualTo(filtro.id());
                    assertThat(evento.quantidade()).isEqualTo(5);
                });
        assertThat(filtro.eventos()).isEmpty();

        darEntrada.darEntrada(filtro.id(), 1);

        assertThat(publicador.publicados()).hasSize(2);
        assertThat(filtro.quantidadeDisponivel()).isEqualTo(8);
    }

    @Test
    void rejeitaQuantidadeInvalidaSemSalvarNemPublicar() {
        Peca filtro = filtroDeOleoCom(2);
        PecaRepositorioEmMemoria repo = new PecaRepositorioEmMemoria().com(filtro);
        DarEntradaNoEstoque darEntrada = new DarEntradaNoEstoque(repo, publicador);
        UUID id = filtro.id();

        assertThatThrownBy(() -> darEntrada.darEntrada(id, 0)).isInstanceOf(QuantidadeInvalidaException.class);
        assertThatThrownBy(() -> darEntrada.darEntrada(id, -3)).isInstanceOf(QuantidadeInvalidaException.class);

        assertThat(filtro.quantidadeDisponivel()).isEqualTo(2);
        assertThat(repo.salvamentos()).isZero();
        assertThat(publicador.publicados()).isEmpty();
    }

    @Test
    void rejeitaPecaAusenteOuSemIdentificador() {
        DarEntradaNoEstoque darEntrada = new DarEntradaNoEstoque(new PecaRepositorioEmMemoria(), publicador);
        UUID ausente = UUID.randomUUID();

        assertThatThrownBy(() -> darEntrada.darEntrada(ausente, 1)).isInstanceOf(PecaNaoEncontradaException.class);
        assertThatNullPointerException().isThrownBy(() -> darEntrada.darEntrada(null, 1));
        assertThat(publicador.publicados()).isEmpty();
    }

    @Test
    void exigeRepositorioEPublicador() {
        PecaRepositorioEmMemoria repo = new PecaRepositorioEmMemoria();

        assertThatNullPointerException().isThrownBy(() -> new DarEntradaNoEstoque(null, publicador));
        assertThatNullPointerException().isThrownBy(() -> new DarEntradaNoEstoque(repo, null));
    }
}
