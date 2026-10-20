package br.com.grupo117.oficina.atendimento.domain;

import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class DecisaoOrcamentoTest {
    private static final Instant INICIO = Instant.parse("2026-10-20T12:00:00Z");

    @Test
    void aprovaEPreservaOrcamentoEInicioAteAEntrega() {
        OrdemDeServico os = orcada();
        var orcamento = os.orcamento();
        os.aprovarOrcamento(INICIO);
        assertThat(os.status()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(os.iniciadaEm()).contains(INICIO);
        assertThat(os.rejeicaoOrcamento()).isEmpty();
        assertThat(os.orcamento()).isEqualTo(orcamento);
        os.transicionarPara(StatusOS.FINALIZADA, INICIO.plusSeconds(120));
        os.transicionarPara(StatusOS.ENTREGUE, INICIO.plusSeconds(180));
        assertThat(os.iniciadaEm()).contains(INICIO);
        assertThat(os.finalizadaEm()).contains(INICIO.plusSeconds(120));
    }

    @Test
    void registraRejeicaoSemIniciarOuModificarItens() {
        OrdemDeServico os = orcada();
        var itens = os.itens();
        var orcamento = os.orcamento();
        os.rejeitarOrcamento(INICIO, "  Preço acima do esperado  ");
        assertThat(os.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(os.iniciadaEm()).isEmpty();
        assertThat(os.rejeicaoOrcamento().orElseThrow().momento()).isEqualTo(INICIO);
        assertThat(os.rejeicaoOrcamento().orElseThrow().motivo()).isEqualTo("Preço acima do esperado");
        assertThat(os.itens()).isEqualTo(itens);
        assertThat(os.orcamento()).isEqualTo(orcamento);
    }

    @Test
    void rejeicaoBloqueiaNovaDecisaoETransicaoGenericaParaExecucao() {
        OrdemDeServico os = orcada();
        os.rejeitarOrcamento(INICIO, "Preço");
        var rejeicao = os.rejeicaoOrcamento();
        assertThatIllegalStateException().isThrownBy(() -> os.aprovarOrcamento(INICIO.plusSeconds(1)));
        assertThatIllegalStateException().isThrownBy(() -> os.rejeitarOrcamento(INICIO.plusSeconds(1), "Outro"));
        assertThatIllegalStateException().isThrownBy(() -> os.transicionarPara(StatusOS.EM_EXECUCAO));
        assertThat(os.rejeicaoOrcamento()).isEqualTo(rejeicao);
        assertThat(os.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(os.iniciadaEm()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = "AGUARDANDO_APROVACAO", mode = EnumSource.Mode.EXCLUDE)
    void rejeitaDecisoesForaDoEstadoPermitido(StatusOS status) {
        Instant finalizacao = status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE ? INICIO : null;
        OrdemDeServico os = OrdemDeServico.restaurar(UUID.randomUUID(), "cliente", "veiculo", status, finalizacao);
        assertThatIllegalStateException().isThrownBy(() -> os.aprovarOrcamento(INICIO));
        assertThatIllegalStateException().isThrownBy(() -> os.rejeitarOrcamento(INICIO, "Preço"));
        assertThat(os.status()).isEqualTo(status);
        assertThat(os.rejeicaoOrcamento()).isEmpty();
    }

    @Test
    void estadoAguardandoSemOrcamentoNaoAdmiteDecisao() {
        OrdemDeServico os = OrdemDeServico.restaurar(UUID.randomUUID(), "cliente", "veiculo",
                StatusOS.AGUARDANDO_APROVACAO, null);
        assertThatIllegalStateException().isThrownBy(() -> os.aprovarOrcamento(INICIO));
        assertThatIllegalStateException().isThrownBy(() -> os.rejeitarOrcamento(INICIO, "Preço"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void motivoObrigatorioSemRegistrarRejeicao(String motivo) {
        OrdemDeServico os = orcada();
        assertThatIllegalArgumentException().isThrownBy(() -> os.rejeitarOrcamento(INICIO, motivo));
        assertThat(os.rejeicaoOrcamento()).isEmpty();
    }

    @Test
    void momentoObrigatorioSemMudarEstado() {
        OrdemDeServico os = orcada();
        assertThatNullPointerException().isThrownBy(() -> os.aprovarOrcamento(null));
        assertThatNullPointerException().isThrownBy(() -> os.rejeitarOrcamento(null, "Preço"));
        assertThat(os.iniciadaEm()).isEmpty();
        assertThat(os.rejeicaoOrcamento()).isEmpty();
        assertThat(os.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
    }

    @Test
    void aprovacaoRepetidaNaoSobrescreveInicio() {
        OrdemDeServico os = orcada();
        os.aprovarOrcamento(INICIO);
        assertThatIllegalStateException().isThrownBy(() -> os.aprovarOrcamento(INICIO.plusSeconds(1)));
        assertThat(os.iniciadaEm()).contains(INICIO);
    }

    @Test
    void conclusaoAnteriorAoInicioNaoMudaEstado() {
        OrdemDeServico os = orcada();
        os.aprovarOrcamento(INICIO);
        assertThatIllegalArgumentException().isThrownBy(() -> os.transicionarPara(StatusOS.FINALIZADA, INICIO.minusSeconds(1)));
        assertThat(os.status()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(os.finalizadaEm()).isEmpty();
    }

    private OrdemDeServico orcada() {
        OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), "cliente", "veiculo");
        os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Diagnóstico", 1, new Dinheiro(new BigDecimal("50.00"))));
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        os.gerarOrcamento();
        return os;
    }
}
