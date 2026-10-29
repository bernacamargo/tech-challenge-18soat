package br.com.grupo117.oficina.catalogoestoque.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PecaTest {

    private static final UUID OS_ID = UUID.randomUUID();
    private static final Dinheiro PRECO = Dinheiro.de("35.90");

    private static Peca filtroDeOleoCom(int quantidade) {
        Peca peca = Peca.cadastrar("Filtro de oleo", "Filtro motor 1.0", TipoItemEstoque.PECA, PRECO, quantidade);
        peca.limparEventos();
        return peca;
    }

    private static EventoDeDominio unicoEvento(Peca peca) {
        List<EventoDeDominio> eventos = peca.eventos();
        assertEquals(1, eventos.size());
        return eventos.get(0);
    }

    @Test
    void cadastraPecaComDadosValidos() {
        Peca peca = Peca.cadastrar("  Oleo 5W30  ", "  Frasco de 1 litro ", TipoItemEstoque.INSUMO, PRECO, 10);

        assertEquals("Oleo 5W30", peca.nome());
        assertEquals("Frasco de 1 litro", peca.descricao());
        assertEquals(TipoItemEstoque.INSUMO, peca.tipo());
        assertEquals(PRECO, peca.precoUnitario());
        assertEquals(10, peca.quantidadeDisponivel());
        assertEquals(0, peca.quantidadeReservada());
        assertEquals(10, peca.quantidadeLivre());
        assertTrue(peca.toString().contains("Oleo 5W30"));

        PecaAdicionada evento = assertInstanceOf(PecaAdicionada.class, unicoEvento(peca));
        assertEquals(peca.id(), evento.pecaId());
        assertEquals("Oleo 5W30", evento.nome());
    }

    @Test
    void aceitaPecaSemDescricaoEComEstoqueZerado() {
        Peca peca = Peca.cadastrar("Pastilha de freio", "  ", TipoItemEstoque.PECA, PRECO, 0);

        assertNull(peca.descricao());
        assertEquals(0, peca.quantidadeLivre());
    }

    @Test
    void rejeitaDadosInvalidosNoCadastro() {
        TipoItemEstoque peca = TipoItemEstoque.PECA;

        assertThrows(IllegalArgumentException.class, () -> Peca.cadastrar("  ", null, peca, PRECO, 1));
        assertThrows(IllegalArgumentException.class, () -> Peca.cadastrar(null, null, peca, PRECO, 1));
        assertThrows(NullPointerException.class, () -> Peca.cadastrar("Vela", null, null, PRECO, 1));
        assertThrows(QuantidadeInvalidaException.class, () -> Peca.cadastrar("Vela", null, peca, PRECO, -1));
    }

    @Test
    void rejeitaPrecoAusenteOuZerado() {
        TipoItemEstoque peca = TipoItemEstoque.PECA;

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> Peca.cadastrar("Vela", null, peca, Dinheiro.ZERO, 1)
        );

        assertEquals("Preco unitario da peca deve ser maior que zero", erro.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Peca.cadastrar("Vela", null, peca, null, 1));
    }

    @Test
    void reabasteceSomandoNoDisponivel() {
        Peca peca = filtroDeOleoCom(2);

        peca.reabastecer(5);

        assertEquals(7, peca.quantidadeDisponivel());
        EstoqueReabastecido evento = assertInstanceOf(EstoqueReabastecido.class, unicoEvento(peca));
        assertEquals(peca.id(), evento.pecaId());
        assertEquals(5, evento.quantidade());
    }

    @Test
    void rejeitaOverflowSemAlterarSaldoReservasOuEventos() {
        Peca peca = filtroDeOleoCom(7);
        peca.reservar(OS_ID, 2);
        peca.limparEventos();

        assertThrows(QuantidadeInvalidaException.class, () -> peca.reabastecer(Integer.MAX_VALUE));
        assertEquals(7, peca.quantidadeDisponivel());
        assertEquals(2, peca.quantidadeReservada());
        assertTrue(peca.eventos().isEmpty());
    }

    @Test
    void aceitaEntradaExatamenteNoLimiteInteiro() {
        Peca peca = filtroDeOleoCom(7);
        peca.reabastecer(Integer.MAX_VALUE - 7);
        assertEquals(Integer.MAX_VALUE, peca.quantidadeDisponivel());
        assertEquals(1, peca.eventos().size());
        assertThrows(QuantidadeInvalidaException.class, () -> peca.reabastecer(1));
        assertEquals(Integer.MAX_VALUE, peca.quantidadeDisponivel());
        assertEquals(1, peca.eventos().size());
    }

    @Test
    void rejeitaReabastecimentoComQuantidadeZeroOuNegativa() {
        Peca peca = filtroDeOleoCom(2);

        assertThrows(QuantidadeInvalidaException.class, () -> peca.reabastecer(0));
        assertThrows(QuantidadeInvalidaException.class, () -> peca.reabastecer(-1));
        assertEquals(2, peca.quantidadeDisponivel());
        assertTrue(peca.eventos().isEmpty());
    }

    @Test
    void reservaDentroDoDisponivel() {
        Peca peca = filtroDeOleoCom(5);

        ReservaDeEstoque reserva = peca.reservar(OS_ID, 3);

        assertEquals(5, peca.quantidadeDisponivel());
        assertEquals(3, peca.quantidadeReservada());
        assertEquals(2, peca.quantidadeLivre());
        assertTrue(reserva.ativa());
        assertEquals(peca.id(), reserva.pecaId());
        assertEquals(OS_ID, reserva.ordemDeServicoId());

        PecaReservada evento = assertInstanceOf(PecaReservada.class, unicoEvento(peca));
        assertEquals(reserva.id(), evento.reservaId());
        assertEquals(OS_ID, evento.ordemDeServicoId());
        assertEquals(3, evento.quantidade());
    }

    @Test
    void rejeitaReservaAcimaDoDisponivel() {
        Peca peca = filtroDeOleoCom(2);

        EstoqueInsuficienteException erro = assertThrows(
                EstoqueInsuficienteException.class,
                () -> peca.reservar(OS_ID, 3)
        );

        assertEquals("Estoque insuficiente: solicitado 3, livre 2", erro.getMessage());
        assertEquals(0, peca.quantidadeReservada());
        assertTrue(peca.eventos().isEmpty());
    }

    @Test
    void rejeitaReservaQueSomadaAOutraUltrapassaOLivre() {
        Peca peca = filtroDeOleoCom(5);
        peca.reservar(OS_ID, 3);
        UUID outraOs = UUID.randomUUID();

        assertThrows(EstoqueInsuficienteException.class, () -> peca.reservar(outraOs, 3));
        assertEquals(3, peca.quantidadeReservada());
    }

    @Test
    void aceitaReservaExatamenteIgualAoLivre() {
        Peca peca = filtroDeOleoCom(4);

        peca.reservar(OS_ID, 4);

        assertEquals(0, peca.quantidadeLivre());
    }

    @Test
    void rejeitaReservaComQuantidadeInvalidaOuSemOrdemDeServico() {
        Peca peca = filtroDeOleoCom(4);

        assertThrows(QuantidadeInvalidaException.class, () -> peca.reservar(OS_ID, 0));
        assertThrows(NullPointerException.class, () -> peca.reservar(null, 1));
        assertEquals(0, peca.quantidadeReservada());
    }

    @Test
    void baixaConsomeReservaSemDescontarDuasVezes() {
        Peca peca = filtroDeOleoCom(5);
        ReservaDeEstoque reserva = peca.reservar(OS_ID, 3);
        peca.limparEventos();

        peca.baixar(reserva);

        assertEquals(2, peca.quantidadeDisponivel());
        assertEquals(0, peca.quantidadeReservada());
        assertEquals(2, peca.quantidadeLivre());
        assertEquals(StatusReserva.BAIXADA, reserva.status());

        EstoqueBaixado evento = assertInstanceOf(EstoqueBaixado.class, unicoEvento(peca));
        assertEquals(reserva.id(), evento.reservaId());
        assertEquals(OS_ID, evento.ordemDeServicoId());
        assertEquals(3, evento.quantidade());
    }

    @Test
    void rejeitaBaixaDeReservaJaBaixada() {
        Peca peca = filtroDeOleoCom(5);
        ReservaDeEstoque reserva = peca.reservar(OS_ID, 3);
        peca.baixar(reserva);

        assertThrows(ReservaEncerradaException.class, () -> peca.baixar(reserva));
        assertEquals(2, peca.quantidadeDisponivel());
        assertEquals(0, peca.quantidadeReservada());
    }

    @Test
    void cancelaReservaDevolvendoQuantidadeAoLivre() {
        Peca peca = filtroDeOleoCom(5);
        ReservaDeEstoque reserva = peca.reservar(OS_ID, 3);
        peca.limparEventos();

        peca.cancelarReserva(reserva);

        assertEquals(5, peca.quantidadeDisponivel());
        assertEquals(0, peca.quantidadeReservada());
        assertEquals(StatusReserva.CANCELADA, reserva.status());
        ReservaCancelada evento = assertInstanceOf(ReservaCancelada.class, unicoEvento(peca));
        assertEquals(3, evento.quantidade());
        assertThrows(ReservaEncerradaException.class, () -> peca.cancelarReserva(reserva));
    }

    @Test
    void rejeitaBaixaECancelamentoDeReservaDeOutraPeca() {
        Peca peca = filtroDeOleoCom(5);
        Peca outra = filtroDeOleoCom(5);
        ReservaDeEstoque reservaDaOutra = outra.reservar(OS_ID, 1);

        assertThrows(IllegalArgumentException.class, () -> peca.baixar(reservaDaOutra));
        assertThrows(IllegalArgumentException.class, () -> peca.cancelarReserva(reservaDaOutra));
        assertThrows(NullPointerException.class, () -> peca.baixar(null));
        assertTrue(reservaDaOutra.ativa());
    }

    @Test
    void alteraDadosDaPeca() {
        Peca peca = filtroDeOleoCom(5);

        peca.alterarDados(" Filtro de ar ", null, TipoItemEstoque.INSUMO, Dinheiro.de("42.00"));

        assertEquals("Filtro de ar", peca.nome());
        assertNull(peca.descricao());
        assertEquals(TipoItemEstoque.INSUMO, peca.tipo());
        assertEquals(Dinheiro.de("42"), peca.precoUnitario());
        assertEquals(5, peca.quantidadeDisponivel());
    }

    @Test
    void rejeitaAlteracaoInvalidaSemMudarAPeca() {
        Peca peca = filtroDeOleoCom(5);
        TipoItemEstoque insumo = TipoItemEstoque.INSUMO;

        assertThrows(IllegalArgumentException.class, () -> peca.alterarDados(" ", null, insumo, PRECO));
        assertThrows(NullPointerException.class, () -> peca.alterarDados("Filtro de ar", null, null, PRECO));
        assertThrows(IllegalArgumentException.class, () -> peca.alterarDados("Filtro de ar", null, insumo, null));
        assertEquals("Filtro de oleo", peca.nome());
        assertEquals(TipoItemEstoque.PECA, peca.tipo());
        assertEquals(PRECO, peca.precoUnitario());
    }

    @Test
    void restauraPecaPersistidaSemEventos() {
        UUID id = UUID.randomUUID();

        Peca peca = Peca.restaurar(id, "Vela", "Vela de ignicao", TipoItemEstoque.PECA, PRECO, 8, 3);

        assertEquals(id, peca.id());
        assertEquals(8, peca.quantidadeDisponivel());
        assertEquals(3, peca.quantidadeReservada());
        assertEquals(5, peca.quantidadeLivre());
        assertTrue(peca.eventos().isEmpty());
    }

    @Test
    void rejeitaRestauracaoInconsistente() {
        UUID id = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> Peca.restaurar(id, "Vela", null, TipoItemEstoque.PECA, PRECO, 2, 3));
        assertThrows(QuantidadeInvalidaException.class,
                () -> Peca.restaurar(id, "Vela", null, TipoItemEstoque.PECA, PRECO, 2, -1));
        assertThrows(NullPointerException.class,
                () -> Peca.restaurar(null, "Vela", null, TipoItemEstoque.PECA, PRECO, 2, 0));
    }

    @Test
    void eventosRetornaCopiaImutavelELimpaEventos() {
        Peca peca = Peca.cadastrar("Vela", null, TipoItemEstoque.PECA, PRECO, 1);
        List<EventoDeDominio> eventos = peca.eventos();

        assertThrows(UnsupportedOperationException.class, eventos::clear);
        peca.limparEventos();
        assertTrue(peca.eventos().isEmpty());
        assertEquals(1, eventos.size());
    }

    @Test
    void identificaPecaPeloId() {
        UUID id = UUID.randomUUID();
        Peca peca = Peca.restaurar(id, "Vela", null, TipoItemEstoque.PECA, PRECO, 2, 0);
        Peca mesmoId = Peca.restaurar(id, "Vela NGK", null, TipoItemEstoque.PECA, PRECO, 9, 1);
        Peca outra = filtroDeOleoCom(2);

        Object mesmaInstancia = peca;
        Object outroTipo = new Object();
        assertEquals(mesmaInstancia, peca);
        assertEquals(peca, mesmoId);
        assertEquals(peca.hashCode(), mesmoId.hashCode());
        assertNotEquals(peca, outra);
        assertNotEquals(peca, null);
        assertNotEquals(outroTipo, peca);
    }
}
