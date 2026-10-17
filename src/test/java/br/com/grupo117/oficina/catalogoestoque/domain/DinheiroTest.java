package br.com.grupo117.oficina.catalogoestoque.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DinheiroTest {

    @Test
    void normalizaPrecoPositivoEmDuasCasas() {
        Dinheiro preco = Dinheiro.de(new BigDecimal("10"));
        Dinheiro mesmoValor = Dinheiro.de("10.001");

        assertEquals(new BigDecimal("10.00"), preco.valor());
        assertEquals(preco, mesmoValor);
        assertEquals(preco.hashCode(), mesmoValor.hashCode());
    }

    @Test
    void arredondaMeioParaCima() {
        assertEquals(new BigDecimal("1.01"), Dinheiro.de("1.005").valor());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "0.004", "-1", "-0.01"})
    void rejeitaZeroENegativo(String valor) {
        DinheiroInvalidoException erro = assertThrows(DinheiroInvalidoException.class, () -> Dinheiro.de(valor));

        assertEquals("Preco deve ser maior que zero", erro.getMessage());
    }

    @Test
    void rejeitaPrecoNulo() {
        DinheiroInvalidoException erro = assertThrows(
                DinheiroInvalidoException.class,
                () -> Dinheiro.de((BigDecimal) null)
        );

        assertEquals("Preco e obrigatorio", erro.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "abc", "10,50"})
    void rejeitaTextoIlegivel(String valor) {
        assertThrows(DinheiroInvalidoException.class, () -> Dinheiro.de(valor));
    }
}
