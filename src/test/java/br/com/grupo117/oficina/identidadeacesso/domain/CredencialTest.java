package br.com.grupo117.oficina.identidadeacesso.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CredencialTest {

    @Test
    void aceitaValorPreenchido() {
        Credencial credencial = new Credencial("token-opaco-123");

        assertEquals("token-opaco-123", credencial.valor());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejeitaCredencialAusente(String valor) {
        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class, () -> new Credencial(valor));

        assertEquals(CredencialInvalidaException.class, e.getClass());
        assertEquals("Credencial ausente", e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"aaa bbb", "aaa\tbbb", "aaa\n", " aaa"})
    void rejeitaCredencialComEspaco(String valor) {
        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class, () -> new Credencial(valor));

        assertEquals("Credencial invalida", e.getMessage());
    }

    @Test
    void toStringNaoExpoeOValor() {
        String texto = new Credencial("token-opaco-123").toString();

        assertEquals("Credencial[valor=***]", texto);
        assertFalse(texto.contains("token-opaco-123"));
    }

    @Test
    void mensagemDaExcecaoNaoContemOValor() {
        CredencialInvalidaException e = assertThrows(CredencialInvalidaException.class,
                () -> new Credencial("segredo-xyz comespaco"));

        assertEquals("Credencial invalida", e.getMessage());
        assertFalse(e.getMessage().contains("segredo-xyz"));
    }

    @Test
    void excecaoPreservaCausa() {
        IllegalStateException causa = new IllegalStateException("x");

        CredencialInvalidaException e = new CredencialInvalidaException("Credencial invalida", causa);

        assertSame(causa, e.getCause());
        assertEquals("Credencial invalida", e.getMessage());
    }

    @Test
    void credencialInvalidaNaoEhIllegalArgumentException() {
        assertFalse(IllegalArgumentException.class.isAssignableFrom(CredencialInvalidaException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(CredencialInvalidaException.class));
    }

    @Test
    void igualdadePorValor() {
        Credencial primeira = new Credencial("token-opaco-123");
        Credencial segunda = new Credencial("token-opaco-123");

        assertEquals(primeira, segunda);
        assertEquals(primeira.hashCode(), segunda.hashCode());
        assertNotEquals(primeira, new Credencial("outro-token"));
    }

    @Test
    void fabricaDeEquivaleAoConstrutor() {
        assertEquals(new Credencial("token-opaco-123"), Credencial.de("token-opaco-123"));
    }
}
