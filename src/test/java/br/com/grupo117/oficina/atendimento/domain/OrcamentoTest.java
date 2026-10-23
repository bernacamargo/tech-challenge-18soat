package br.com.grupo117.oficina.atendimento.domain;

import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Orcamento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;

class OrcamentoTest {
    @Test
    void somaServicosEPecasConsiderandoQuantidades() {
        OrdemDeServico os = ordemNoStatus(StatusOS.EM_DIAGNOSTICO);
        os.incluirItem(item(TipoItemOS.SERVICO, "100.10", 2));
        os.incluirItem(item(TipoItemOS.PECA, "12.75", 3));
        assertThat(os.orcamento()).isEmpty();

        Orcamento orcamento = os.gerarOrcamento();

        assertThat(orcamento.total().valor()).isEqualByComparingTo("238.45");
        assertThat(os.orcamento()).contains(orcamento);
        assertThat(os.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(orcamento.itens()).containsExactlyElementsOf(os.itens());
        assertThatIllegalStateException().isThrownBy(() -> os.incluirItem(item(TipoItemOS.PECA, "1.00", 1)));
        assertThatIllegalStateException().isThrownBy(os::gerarOrcamento);
        assertThat(os.orcamento()).contains(orcamento);
    }

    @Test
    void submeterOsRecebidaCalculaOrcamentoEEnviaParaAprovacao() {
        OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
        os.incluirItem(item(TipoItemOS.SERVICO, "40.00", 1));
        Orcamento orcamento = os.submeter();
        assertThat(os.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(orcamento.total().valor()).isEqualByComparingTo("40.00");
        assertThatIllegalStateException().isThrownBy(os::submeter);
    }

    @Test
    void orcamentoCopiaColecaoERejeitaItensAusentes() {
        List<ItemDaOS> origem = new ArrayList<>(List.of(item(TipoItemOS.PECA, "0.10", 3)));
        Orcamento orcamento = new Orcamento(origem);
        origem.clear();
        assertThat(orcamento.total().valor()).isEqualByComparingTo("0.30");
        assertThatThrownBy(() -> orcamento.itens().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatNullPointerException().isThrownBy(() -> new Orcamento(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new Orcamento(List.of()));
    }

    @Test
    void osSemItensNaoMudaDeStatus() {
        OrdemDeServico os = ordemNoStatus(StatusOS.EM_DIAGNOSTICO);
        assertThatIllegalArgumentException().isThrownBy(os::gerarOrcamento);
        assertThat(os.status()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        assertThat(os.orcamento()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = "EM_DIAGNOSTICO", mode = EnumSource.Mode.EXCLUDE)
    void estadoInvalidoNaoGeraOrcamento(StatusOS status) {
        OrdemDeServico os = ordemNoStatus(status);
        assertThatIllegalStateException().isThrownBy(os::gerarOrcamento);
        assertThat(os.status()).isEqualTo(status);
        assertThat(os.orcamento()).isEmpty();
    }

    private static OrdemDeServico ordemNoStatus(StatusOS destino) {
        OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
        for (StatusOS etapa : StatusOS.values()) {
            if (os.status() == destino) break;
            if (etapa != StatusOS.RECEBIDA) os.transicionarPara(etapa);
        }
        return os;
    }

    private static ItemDaOS item(TipoItemOS tipo, String preco, int quantidade) {
        return new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), tipo, "Item", quantidade,
                new Dinheiro(new BigDecimal(preco)));
    }
}
