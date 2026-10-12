package br.com.grupo117.oficina.atendimento.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Year;
import org.junit.jupiter.api.Test;

class VeiculoTest {

    @Test
    void cadastraVeiculoComPlacaValida() {
        Veiculo veiculo = Veiculo.cadastrar("abc-1d23", "  Fiat  ", " Uno ", 2012);

        assertEquals(Placa.de("ABC1D23"), veiculo.placa());
        assertEquals("Fiat", veiculo.marca());
        assertEquals("Uno", veiculo.modelo());
        assertEquals(2012, veiculo.ano());
        assertTrue(veiculo.toString().contains("ABC1D23"));
    }

    @Test
    void rejeitaPlacaInvalida() {
        PlacaInvalidaException erro = assertThrows(
                PlacaInvalidaException.class,
                () -> Veiculo.cadastrar("ABC12D3", "Fiat", "Uno", 2012)
        );

        assertEquals("Placa invalida", erro.getMessage());
    }

    @Test
    void rejeitaMarcaEModeloEmBranco() {
        Placa placa = Placa.de("ABC1234");

        assertThrows(IllegalArgumentException.class, () -> new Veiculo(placa, "  ", "Uno", 2012));
        assertThrows(IllegalArgumentException.class, () -> new Veiculo(placa, "Fiat", null, 2012));
        assertThrows(IllegalArgumentException.class, () -> Veiculo.cadastrar("ABC1234", null, "Uno", 2012));
    }

    @Test
    void rejeitaAnoForaDaFaixa() {
        assertThrows(IllegalArgumentException.class, () -> Veiculo.cadastrar("ABC1234", "Fiat", "Uno", 1899));
        assertThrows(
                IllegalArgumentException.class,
                () -> Veiculo.cadastrar("ABC1234", "Fiat", "Uno", Year.now().getValue() + 2)
        );
    }

    @Test
    void aceitaAnoNoLimiteDaFaixa() {
        Veiculo classico = Veiculo.cadastrar("ABC1234", "Ford", "T", 1900);
        Veiculo anoQueVem = Veiculo.cadastrar("ABC1D23", "Fiat", "Uno", Year.now().getValue() + 1);

        assertEquals(1900, classico.ano());
        assertEquals(Year.now().getValue() + 1, anoQueVem.ano());
    }

    @Test
    void rejeitaPlacaAusente() {
        assertThrows(NullPointerException.class, () -> new Veiculo(null, "Fiat", "Uno", 2012));
    }

    @Test
    void identificaVeiculoPelaPlaca() {
        Veiculo uno = Veiculo.cadastrar("ABC1234", "Fiat", "Uno", 2012);
        Veiculo mesmaPlaca = Veiculo.cadastrar("abc-1234", "Fiat", "Uno Vivace", 2013);
        Veiculo outro = Veiculo.cadastrar("ABC1D23", "Fiat", "Uno", 2012);

        Object mesmaInstancia = uno;
        Object outroTipo = new Object();
        assertEquals(mesmaInstancia, uno);
        assertEquals(uno, mesmaPlaca);
        assertEquals(uno.hashCode(), mesmaPlaca.hashCode());
        assertNotEquals(uno, outro);
        assertNotEquals(uno, null);
        assertNotEquals(outroTipo, uno);
    }
}
