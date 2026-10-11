package br.com.grupo117.oficina.atendimento.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CpfCnpjTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", "12345678909", "123.456.789-09"})
    void aceitaCpfValidoComOuSemMascara(String documento) {
        CpfCnpj cpf = CpfCnpj.de(documento);

        assertTrue(cpf.cpf());
        assertFalse(cpf.cnpj());
        assertEquals(11, cpf.digitos().length());
    }

    @Test
    void normalizaCpfMascaradoParaOsMesmosDigitos() {
        CpfCnpj mascarado = CpfCnpj.de("529.982.247-25");
        CpfCnpj somenteDigitos = CpfCnpj.de("52998224725");

        assertEquals("52998224725", mascarado.digitos());
        assertEquals(somenteDigitos, mascarado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"11222333000181", "11.222.333/0001-81"})
    void aceitaCnpjValidoComOuSemMascara(String documento) {
        CpfCnpj cnpj = CpfCnpj.de(documento);

        assertTrue(cnpj.cnpj());
        assertFalse(cnpj.cpf());
        assertEquals("11222333000181", cnpj.digitos());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "11111111111",
            "00000000000",
            "12345678900",
            "529.982.247-26",
            "00000000000000",
            "11111111111111",
            "11222333000182",
            "11.222.333/0001-80",
            "123",
            "123456789012",
            "abc",
            "5299822472X",
            "..."
    })
    void rejeitaDocumentoInvalido(String documento) {
        assertThrows(CpfCnpjInvalidoException.class, () -> CpfCnpj.de(documento));
    }
}
