package br.com.grupo117.oficina.catalogoestoque.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservaDeEstoqueTest {

    private static final UUID PECA_ID = UUID.randomUUID();
    private static final UUID OS_ID = UUID.randomUUID();

    private static ReservaDeEstoque novaReserva() {
        return ReservaDeEstoque.criar(PECA_ID, OS_ID, QuantidadeReservada.de(2));
    }

    @Test
    void nasceAtivaVinculadaAPecaEAOrdemDeServico() {
        ReservaDeEstoque reserva = novaReserva();

        assertTrue(reserva.ativa());
        assertEquals(StatusReserva.ATIVA, reserva.status());
        assertEquals(PECA_ID, reserva.pecaId());
        assertEquals(OS_ID, reserva.ordemDeServicoId());
        assertEquals(2, reserva.quantidade().valor());
        assertTrue(reserva.toString().contains("ATIVA"));
    }

    @Test
    void marcaBaixadaUmaUnicaVez() {
        ReservaDeEstoque reserva = novaReserva();

        reserva.marcarBaixada();

        assertFalse(reserva.ativa());
        assertEquals(StatusReserva.BAIXADA, reserva.status());
        assertThrows(ReservaEncerradaException.class, reserva::marcarBaixada);
    }

    @Test
    void naoCancelaReservaJaBaixada() {
        ReservaDeEstoque reserva = novaReserva();
        reserva.marcarBaixada();

        ReservaEncerradaException erro = assertThrows(ReservaEncerradaException.class, reserva::marcarCancelada);

        assertTrue(erro.getMessage().contains("BAIXADA"));
    }

    @Test
    void naoBaixaReservaCancelada() {
        ReservaDeEstoque reserva = novaReserva();
        reserva.marcarCancelada();

        assertEquals(StatusReserva.CANCELADA, reserva.status());
        assertThrows(ReservaEncerradaException.class, reserva::marcarBaixada);
    }

    @Test
    void reconstituiReservaPersistida() {
        UUID id = UUID.randomUUID();

        ReservaDeEstoque reserva = ReservaDeEstoque.reconstituir(id, PECA_ID, OS_ID, 3, StatusReserva.BAIXADA);

        assertEquals(id, reserva.id());
        assertEquals(3, reserva.quantidade().valor());
        assertEquals(StatusReserva.BAIXADA, reserva.status());
    }

    @Test
    void rejeitaDadosObrigatoriosAusentes() {
        UUID id = UUID.randomUUID();

        assertThrows(NullPointerException.class,
                () -> ReservaDeEstoque.reconstituir(null, PECA_ID, OS_ID, 1, StatusReserva.ATIVA));
        assertThrows(NullPointerException.class,
                () -> ReservaDeEstoque.reconstituir(id, null, OS_ID, 1, StatusReserva.ATIVA));
        assertThrows(NullPointerException.class,
                () -> ReservaDeEstoque.reconstituir(id, PECA_ID, null, 1, StatusReserva.ATIVA));
        assertThrows(NullPointerException.class,
                () -> ReservaDeEstoque.reconstituir(id, PECA_ID, OS_ID, 1, null));
        assertThrows(NullPointerException.class, () -> ReservaDeEstoque.criar(PECA_ID, OS_ID, null));
        assertThrows(QuantidadeInvalidaException.class,
                () -> ReservaDeEstoque.reconstituir(id, PECA_ID, OS_ID, 0, StatusReserva.ATIVA));
    }

    @Test
    void identificaReservaPeloId() {
        UUID id = UUID.randomUUID();
        ReservaDeEstoque reserva = ReservaDeEstoque.reconstituir(id, PECA_ID, OS_ID, 1, StatusReserva.ATIVA);
        ReservaDeEstoque mesmoId = ReservaDeEstoque.reconstituir(id, PECA_ID, OS_ID, 5, StatusReserva.BAIXADA);
        ReservaDeEstoque outra = novaReserva();

        Object mesmaInstancia = reserva;
        Object outroTipo = new Object();
        assertEquals(mesmaInstancia, reserva);
        assertEquals(reserva, mesmoId);
        assertEquals(reserva.hashCode(), mesmoId.hashCode());
        assertNotEquals(reserva, outra);
        assertNotEquals(reserva, null);
        assertNotEquals(outroTipo, reserva);
    }
}
