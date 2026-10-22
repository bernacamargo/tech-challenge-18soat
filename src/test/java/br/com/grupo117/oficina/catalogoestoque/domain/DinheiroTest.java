package br.com.grupo117.oficina.catalogoestoque.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DinheiroTest {

    @Test
    void normalizaParaDuasCasasDecimais() {
        assertEquals(new BigDecimal("10.50"), Dinheiro.de(" 10.5 ").valor());
        assertEquals(new BigDecimal("0.12"), Dinheiro.de(new BigDecimal("0.125")).valor());
        assertEquals(Dinheiro.de("10.50"), Dinheiro.de("10.5"));
    }

    @Test
    void aceitaZeroMasNaoOConsideraPositivo() {
        assertFalse(Dinheiro.ZERO.positivo());
        assertTrue(Dinheiro.de("0.01").positivo());
    }

    @Test
    void rejeitaValorAusenteMalFormatadoOuNegativo() {
        assertThrows(DinheiroInvalidoException.class, () -> Dinheiro.de((BigDecimal) null));
        assertThrows(DinheiroInvalidoException.class, () -> Dinheiro.de((String) null));
        assertThrows(DinheiroInvalidoException.class, () -> Dinheiro.de("  "));
        assertThrows(DinheiroInvalidoException.class, () -> Dinheiro.de("dez reais"));

        DinheiroInvalidoException erro = assertThrows(DinheiroInvalidoException.class, () -> Dinheiro.de("-1"));
        assertEquals("Valor monetario nao pode ser negativo", erro.getMessage());
    }

    @Test
    void somaEMultiplicaValores() {
        Dinheiro preco = Dinheiro.de("35.90");

        assertEquals(Dinheiro.de("107.70"), preco.multiplicar(3));
        assertEquals(Dinheiro.de("45.90"), preco.somar(Dinheiro.de("10")));
        assertEquals(Dinheiro.ZERO, preco.multiplicar(0));
    }
}
