package br.com.grupo117.oficina.atendimento.domain;

import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class ItensDaOrdemDeServicoTest {
    @ParameterizedTest
    @EnumSource(StatusOS.class)
    void incluiItensSomenteAntesDoOrcamento(StatusOS status) {
        OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
        for (StatusOS etapa : StatusOS.values()) {
            if (os.status() == status) break;
            if (etapa != StatusOS.RECEBIDA) os.transicionarPara(etapa);
        }
        ItemDaOS item = item();
        if (status == StatusOS.RECEBIDA || status == StatusOS.EM_DIAGNOSTICO) {
            os.incluirItem(item);
            assertThat(os.itens()).containsExactly(item);
        } else {
            assertThatIllegalStateException().isThrownBy(() -> os.incluirItem(item));
            assertThat(os.itens()).isEmpty();
        }
    }

    @Test
    void subtotalUsaQuantidadeEPrecoEmCentavos() {
        ItemDaOS item = item();
        assertThat(item.subtotal().valor()).isEqualByComparingTo("25.50");
        assertThat(item.descricao()).isEqualTo("Filtro");
    }

    @Test
    void itensSaoImutaveisENaoAceitamIdDuplicadoOuNulo() {
        OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
        ItemDaOS item = item();
        os.incluirItem(item);
        assertThatThrownBy(() -> os.itens().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatIllegalArgumentException().isThrownBy(() -> os.incluirItem(item));
        assertThatNullPointerException().isThrownBy(() -> os.incluirItem(null));
        assertThat(os.itens()).containsExactly(item);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void quantidadeInvalida(int quantidade) {
        UUID id = UUID.randomUUID();
        Dinheiro preco = new Dinheiro(BigDecimal.TEN);
        assertThatIllegalArgumentException().isThrownBy(
                () -> new ItemDaOS(id, id, TipoItemOS.SERVICO, "Serviço", quantidade, preco));
        assertThatIllegalArgumentException().isThrownBy(() -> preco.multiplicar(quantidade));
    }

    @Test
    void validaPrecoEDescricao() {
        UUID id = UUID.randomUUID();
        Dinheiro zero = new Dinheiro(BigDecimal.ZERO);
        assertThatIllegalArgumentException().isThrownBy(
                () -> new ItemDaOS(id, id, TipoItemOS.PECA, "Peça", 1, zero));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new ItemDaOS(id, id, TipoItemOS.PECA, " ", 1, zero));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new ItemDaOS(id, id, TipoItemOS.PECA, null, 1, zero));
        assertThatNullPointerException().isThrownBy(
                () -> new ItemDaOS(null, id, TipoItemOS.PECA, "Peça", 1, zero));
        assertThatNullPointerException().isThrownBy(
                () -> new ItemDaOS(id, null, TipoItemOS.PECA, "Peça", 1, zero));
        assertThatNullPointerException().isThrownBy(
                () -> new ItemDaOS(id, id, null, "Peça", 1, zero));
        assertThatNullPointerException().isThrownBy(
                () -> new ItemDaOS(id, id, TipoItemOS.PECA, "Peça", 1, null));
    }

    @Test
    void dinheiroRejeitaValoresInvalidosENormalizaEscala() {
        assertThatNullPointerException().isThrownBy(() -> new Dinheiro(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new Dinheiro(new BigDecimal("-0.01")));
        assertThatIllegalArgumentException().isThrownBy(() -> new Dinheiro(new BigDecimal("1.001")));
        assertThat(new Dinheiro(BigDecimal.ONE).valor()).isEqualTo(new BigDecimal("1.00"));
    }

    private static ItemDaOS item() {
        return new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.PECA, " Filtro ", 2,
                new Dinheiro(new BigDecimal("12.75")));
    }
}
