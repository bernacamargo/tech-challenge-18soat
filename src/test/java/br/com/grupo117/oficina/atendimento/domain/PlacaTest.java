package br.com.grupo117.oficina.atendimento.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PlacaTest {

    @ParameterizedTest
    @ValueSource(strings = {"ABC1234", "abc-1234", "ABC 1234", " abc-1234 "})
    void aceitaPlacaAntigaComOuSemMascara(String informada) {
        Placa placa = Placa.de(informada);

        assertEquals("ABC1234", placa.valor());
        assertTrue(placa.antiga());
        assertFalse(placa.mercosul());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC1D23", "abc-1d23", "ABC 1D23"})
    void aceitaPlacaMercosulComOuSemMascara(String informada) {
        Placa placa = Placa.de(informada);

        assertEquals("ABC1D23", placa.valor());
        assertTrue(placa.mercosul());
        assertFalse(placa.antiga());
    }

    @Test
    void normalizaPlacaMascaradaParaOMesmoValor() {
        assertEquals(Placa.de("ABC-1234"), Placa.de("abc1234"));
        assertEquals(Placa.de("ABC-1D23"), Placa.de("abc1d23"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "ABC123",
            "ABC12345",
            "1234ABC",
            "AB11234",
            "ABC12D3",
            "1111111",
            "ABC@1234",
            "ABC@123",
            "AB-C1234",
            "ABC--1234",
            "ABCD-123",
            "ABC1234-"
    })
    void rejeitaPlacaInvalida(String informada) {
        assertThrows(PlacaInvalidaException.class, () -> Placa.de(informada));
    }
}
