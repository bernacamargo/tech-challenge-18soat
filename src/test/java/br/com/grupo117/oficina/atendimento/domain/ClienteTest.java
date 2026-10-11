package br.com.grupo117.oficina.atendimento.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClienteTest {

    @Test
    void cadastraClienteComCpfValido() {
        Cliente cliente = Cliente.cadastrar("  Maria Souza  ", "529.982.247-25");

        assertEquals("Maria Souza", cliente.nome());
        assertEquals(CpfCnpj.de("52998224725"), cliente.cpfCnpj());
        assertTrue(cliente.toString().contains("52998224725"));
    }

    @Test
    void cadastraClienteComCnpjValido() {
        Cliente cliente = Cliente.cadastrar("Oficina Parceira LTDA", "11.222.333/0001-81");

        assertEquals("11222333000181", cliente.cpfCnpj().digitos());
        assertTrue(cliente.cpfCnpj().cnpj());
    }

    @Test
    void rejeitaCpfInvalido() {
        CpfCnpjInvalidoException erro = assertThrows(
                CpfCnpjInvalidoException.class,
                () -> Cliente.cadastrar("Maria Souza", "111.111.111-11")
        );

        assertEquals("CPF/CNPJ invalido", erro.getMessage());
    }

    @Test
    void rejeitaCnpjInvalido() {
        assertThrows(
                CpfCnpjInvalidoException.class,
                () -> Cliente.cadastrar("Oficina Parceira LTDA", "11.222.333/0001-80")
        );
    }

    @Test
    void rejeitaNomeEmBranco() {
        CpfCnpj documento = CpfCnpj.de("52998224725");

        assertThrows(IllegalArgumentException.class, () -> new Cliente("   ", documento));
        assertThrows(IllegalArgumentException.class, () -> Cliente.cadastrar(null, "52998224725"));
    }

    @Test
    void rejeitaDocumentoAusente() {
        assertThrows(NullPointerException.class, () -> new Cliente("Maria Souza", null));
    }

    @Test
    void identificaClientePeloDocumento() {
        Cliente maria = Cliente.cadastrar("Maria Souza", "52998224725");
        Cliente mesmoDocumento = Cliente.cadastrar("Maria S.", "529.982.247-25");
        Cliente outro = Cliente.cadastrar("Joao Lima", "12345678909");

        assertEquals(maria, maria);
        assertEquals(maria, mesmoDocumento);
        assertEquals(maria.hashCode(), mesmoDocumento.hashCode());
        assertNotEquals(maria, outro);
        assertNotEquals(maria, null);
        assertNotEquals(maria, "52998224725");
    }
}
