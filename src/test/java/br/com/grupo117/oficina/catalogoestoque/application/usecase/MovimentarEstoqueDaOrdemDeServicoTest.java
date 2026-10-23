package br.com.grupo117.oficina.catalogoestoque.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.grupo117.oficina.catalogoestoque.domain.Dinheiro;
import br.com.grupo117.oficina.catalogoestoque.domain.EstoqueBaixado;
import br.com.grupo117.oficina.catalogoestoque.domain.EstoqueInsuficienteException;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import br.com.grupo117.oficina.catalogoestoque.domain.PecaReservada;
import br.com.grupo117.oficina.catalogoestoque.domain.ReservaDeEstoque;
import br.com.grupo117.oficina.catalogoestoque.domain.ReservaCancelada;
import br.com.grupo117.oficina.catalogoestoque.domain.StatusReserva;
import br.com.grupo117.oficina.catalogoestoque.domain.TipoItemEstoque;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MovimentarEstoqueDaOrdemDeServicoTest {

    private static final UUID OS_ID = UUID.randomUUID();

    private final Peca filtro = pecaCom("Filtro de oleo", 5);
    private final Peca vela = pecaCom("Vela", 8);
    private final PecaRepositorioEmMemoria pecas = new PecaRepositorioEmMemoria().com(filtro, vela);
    private final ReservaDeEstoqueRepositorioEmMemoria reservas = new ReservaDeEstoqueRepositorioEmMemoria();
    private final PublicadorDeEventosEmMemoria publicador = new PublicadorDeEventosEmMemoria();
    private final MovimentarEstoqueDaOrdemDeServico estoque =
            new MovimentarEstoqueDaOrdemDeServico(pecas, reservas, publicador);

    private static Peca pecaCom(String nome, int quantidade) {
        Peca peca = Peca.cadastrar(nome, null, TipoItemEstoque.PECA, Dinheiro.de("10"), quantidade);
        peca.limparEventos();
        return peca;
    }

    private ReservaDeEstoque reserva(UUID id) {
        return reservas.buscarPorId(id).orElseThrow();
    }

    @Test
    void reservaPecaParaAOrdemDeServico() {
        UUID reservaId = estoque.reservar(OS_ID, filtro.id(), 3);

        assertThat(reserva(reservaId).ativa()).isTrue();
        assertThat(reserva(reservaId).ordemDeServicoId()).isEqualTo(OS_ID);
        assertThat(filtro.quantidadeDisponivel()).isEqualTo(5);
        assertThat(filtro.quantidadeLivre()).isEqualTo(2);
        assertThat(pecas.salvamentos()).isEqualTo(1);
        assertThat(publicador.publicados()).singleElement()
                .isInstanceOfSatisfying(PecaReservada.class, evento -> {
                    assertThat(evento.reservaId()).isEqualTo(reservaId);
                    assertThat(evento.quantidade()).isEqualTo(3);
                });
        assertThat(filtro.eventos()).isEmpty();
    }

    @Test
    void rejeitaReservaAcimaDoLivreSemSalvar() {
        UUID pecaId = filtro.id();

        assertThatThrownBy(() -> estoque.reservar(OS_ID, pecaId, 6))
                .isInstanceOf(EstoqueInsuficienteException.class);

        assertThat(filtro.quantidadeReservada()).isZero();
        assertThat(pecas.salvamentos()).isZero();
        assertThat(reservas.quantidade()).isZero();
        assertThat(publicador.publicados()).isEmpty();
    }

    @Test
    void rejeitaReservaDePecaInexistente() {
        UUID ausente = UUID.randomUUID();

        assertThatThrownBy(() -> estoque.reservar(OS_ID, ausente, 1))
                .isInstanceOf(PecaNaoEncontradaException.class);
        assertThat(reservas.quantidade()).isZero();
    }

    @Test
    void baixaConsumindoAsReservasDaOrdemDeServico() {
        UUID reservaFiltro = estoque.reservar(OS_ID, filtro.id(), 3);
        UUID reservaVela = estoque.reservar(OS_ID, vela.id(), 4);
        UUID outraOs = UUID.randomUUID();
        UUID reservaOutraOs = estoque.reservar(outraOs, vela.id(), 2);
        publicador.publicados().clear();

        int baixadas = estoque.baixarReservasDaOrdemDeServico(OS_ID);

        assertThat(baixadas).isEqualTo(2);
        assertThat(filtro.quantidadeDisponivel()).isEqualTo(2);
        assertThat(filtro.quantidadeReservada()).isZero();
        assertThat(vela.quantidadeDisponivel()).isEqualTo(4);
        assertThat(vela.quantidadeReservada()).isEqualTo(2);
        assertThat(reserva(reservaFiltro).status()).isEqualTo(StatusReserva.BAIXADA);
        assertThat(reserva(reservaVela).status()).isEqualTo(StatusReserva.BAIXADA);
        assertThat(reserva(reservaOutraOs).ativa()).isTrue();
        assertThat(publicador.publicados()).hasSize(2).allMatch(EstoqueBaixado.class::isInstance);
    }

    @Test
    void naoDescontaOEstoqueDuasVezes() {
        estoque.reservar(OS_ID, filtro.id(), 3);
        estoque.baixarReservasDaOrdemDeServico(OS_ID);
        publicador.publicados().clear();

        int baixadas = estoque.baixarReservasDaOrdemDeServico(OS_ID);

        assertThat(baixadas).isZero();
        assertThat(filtro.quantidadeDisponivel()).isEqualTo(2);
        assertThat(filtro.quantidadeReservada()).isZero();
        assertThat(publicador.publicados()).isEmpty();
    }

    @Test
    void ordemDeServicoSemReservaNaoAlteraOEstoque() {
        int baixadas = estoque.baixarReservasDaOrdemDeServico(UUID.randomUUID());

        assertThat(baixadas).isZero();
        assertThat(filtro.quantidadeDisponivel()).isEqualTo(5);
        assertThat(pecas.salvamentos()).isZero();
    }

    @Test
    void rejeitaBaixaDeReservaCujaPecaNaoExisteMais() {
        estoque.reservar(OS_ID, filtro.id(), 1);
        pecas.remover(filtro.id());

        assertThatThrownBy(() -> estoque.baixarReservasDaOrdemDeServico(OS_ID))
                .isInstanceOf(PecaNaoEncontradaException.class);
    }

    @Test
    void cancelarReservaDevolveQuantidadeAoLivre() {
        UUID reservaId = estoque.reservar(OS_ID, filtro.id(), 3);
        publicador.publicados().clear();

        int canceladas = estoque.cancelarReservasDaOrdemDeServico(OS_ID);

        assertThat(canceladas).isEqualTo(1);
        assertThat(filtro.quantidadeDisponivel()).isEqualTo(5);
        assertThat(filtro.quantidadeLivre()).isEqualTo(5);
        assertThat(reserva(reservaId).status()).isEqualTo(StatusReserva.CANCELADA);
        assertThat(publicador.publicados()).singleElement().isInstanceOf(ReservaCancelada.class);
        assertThat(estoque.cancelarReservasDaOrdemDeServico(OS_ID)).isZero();
    }

    @Test
    void exigeIdentificadoresEDependencias() {
        UUID pecaId = filtro.id();

        assertThatNullPointerException().isThrownBy(() -> estoque.reservar(null, pecaId, 1));
        assertThatNullPointerException().isThrownBy(() -> estoque.reservar(OS_ID, null, 1));
        assertThatNullPointerException().isThrownBy(() -> estoque.baixarReservasDaOrdemDeServico(null));
        assertThatNullPointerException().isThrownBy(() -> estoque.cancelarReservasDaOrdemDeServico(null));
        assertThatNullPointerException()
                .isThrownBy(() -> new MovimentarEstoqueDaOrdemDeServico(null, reservas, publicador));
        assertThatNullPointerException()
                .isThrownBy(() -> new MovimentarEstoqueDaOrdemDeServico(pecas, null, publicador));
        assertThatNullPointerException()
                .isThrownBy(() -> new MovimentarEstoqueDaOrdemDeServico(pecas, reservas, null));
    }
}
