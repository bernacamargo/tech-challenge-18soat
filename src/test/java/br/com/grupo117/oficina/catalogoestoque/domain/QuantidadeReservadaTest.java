package br.com.grupo117.oficina.catalogoestoque.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class QuantidadeReservadaTest {

    @Test
    void aceitaQuantidadePositiva() {
        assertEquals(1, QuantidadeReservada.de(1).valor());
    }

    @Test
    void rejeitaQuantidadeZeroOuNegativa() {
        QuantidadeInvalidaException erro = assertThrows(
                QuantidadeInvalidaException.class,
                () -> QuantidadeReservada.de(0)
        );

        assertEquals("Quantidade deve ser maior que zero", erro.getMessage());
        assertThrows(QuantidadeInvalidaException.class, () -> QuantidadeReservada.de(-3));
    }
}
