package br.com.grupo117.oficina.catalogoestoque.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServicoTest {

    @Test
    void cadastraServicoComPrecoValido() {
        Servico servico = Servico.cadastrar("  Troca de oleo  ", "  ", new BigDecimal("150"));

        assertEquals("Troca de oleo", servico.nome());
        assertNull(servico.descricao());
        assertEquals(new BigDecimal("150.00"), servico.preco().valor());
        assertEquals(servico, new Servico(servico.id(), "Outro nome", "outra", Dinheiro.de("1")));
        assertTrue(servico.toString().contains("150.00"));
    }

    @Test
    void guardaDescricaoQuandoInformada() {
        Servico servico = Servico.cadastrar("Alinhamento", "  Geometria dianteira  ", new BigDecimal("80.5"));

        assertEquals("Geometria dianteira", servico.descricao());
        assertEquals(new BigDecimal("80.50"), servico.preco().valor());
    }

    @Test
    void rejeitaServicoSemPrecoValido() {
        assertThrows(DinheiroInvalidoException.class, () -> Servico.cadastrar("Troca de oleo", null, null));
        assertThrows(DinheiroInvalidoException.class, () -> Servico.cadastrar("Troca de oleo", null, BigDecimal.ZERO));
        assertThrows(
                DinheiroInvalidoException.class,
                () -> Servico.cadastrar("Troca de oleo", null, new BigDecimal("-10"))
        );
    }

    @Test
    void rejeitaNomeEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> Servico.cadastrar("   ", "desc", new BigDecimal("10")));
        assertThrows(IllegalArgumentException.class, () -> Servico.cadastrar(null, "desc", new BigDecimal("10")));
    }

    @Test
    void rejeitaPrecoAusenteNoConstrutor() {
        assertThrows(
                NullPointerException.class,
                () -> new Servico(UUID.randomUUID(), "Troca de oleo", null, null)
        );
    }

    @Test
    void alterarMantemOIdETrocaOsDemaisCampos() {
        Servico original = Servico.cadastrar("Troca de oleo", null, new BigDecimal("150"));

        Servico alterado = original.alterar("Troca de oleo sintetico", "Inclui filtro", new BigDecimal("220.4"));

        assertEquals(original.id(), alterado.id());
        assertEquals(original, alterado);
        assertEquals("Troca de oleo sintetico", alterado.nome());
        assertEquals("Inclui filtro", alterado.descricao());
        assertEquals(new BigDecimal("220.40"), alterado.preco().valor());
    }

    @Test
    void cadaCadastroRecebeUmIdProprio() {
        Servico primeiro = Servico.cadastrar("Troca de oleo", null, new BigDecimal("150"));
        Servico segundo = Servico.cadastrar("Troca de oleo", null, new BigDecimal("150"));

        assertNotEquals(primeiro, segundo);
        assertNotEquals(primeiro, null);
        assertNotEquals(new Object(), primeiro);
    }
}
