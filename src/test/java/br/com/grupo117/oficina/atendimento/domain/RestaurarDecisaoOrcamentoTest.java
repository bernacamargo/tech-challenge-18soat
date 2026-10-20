package br.com.grupo117.oficina.atendimento.domain;

import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Orcamento;
import br.com.grupo117.oficina.atendimento.domain.valueobject.RejeicaoOrcamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;

class RestaurarDecisaoOrcamentoTest {
    private static final Instant INICIO = Instant.parse("2026-10-20T12:00:00Z");
    private final ItemDaOS item = new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
            "Diagnóstico", 1, new Dinheiro(new BigDecimal("50.00")));
    private final List<ItemDaOS> itens = List.of(item);
    private final Orcamento orcamento = new Orcamento(itens);

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"EM_EXECUCAO", "FINALIZADA", "ENTREGUE"})
    void restauraInicioEFinalizacaoSemExecutarTransicoes(StatusOS status) {
        Instant fim = status == StatusOS.EM_EXECUCAO ? null : INICIO.plusSeconds(600);
        OrdemDeServico os = restaurar(status, fim, orcamento, INICIO, null);
        assertThat(os.status()).isEqualTo(status);
        assertThat(os.iniciadaEm()).contains(INICIO);
        assertThat(os.finalizadaEm().orElse(null)).isEqualTo(fim);
        assertThat(os.orcamento()).contains(orcamento);
        assertThat(os.itens()).containsExactly(item);
    }

    @Test
    void restauraRejeicaoEConservaBloqueioDeNovaDecisao() {
        RejeicaoOrcamento rejeicao = new RejeicaoOrcamento(INICIO, "Custo");
        OrdemDeServico os = restaurar(StatusOS.AGUARDANDO_APROVACAO, null, orcamento, null, rejeicao);
        assertThat(os.rejeicaoOrcamento()).contains(rejeicao);
        assertThat(os.iniciadaEm()).isEmpty();
        assertThatIllegalStateException().isThrownBy(() -> os.aprovarOrcamento(INICIO.plusSeconds(1)));
        assertThatIllegalStateException().isThrownBy(() -> os.rejeitarOrcamento(INICIO.plusSeconds(1), "Outro"));
        assertThatIllegalStateException().isThrownBy(() -> os.transicionarPara(StatusOS.EM_EXECUCAO));
    }

    @ParameterizedTest
    @EnumSource(StatusOS.class)
    void preservaCompatibilidadeComEstadoLegadoSemDecisao(StatusOS status) {
        Instant fim = status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE ? INICIO : null;
        OrdemDeServico os = restaurar(status, fim, null, null, null);
        assertThat(os.status()).isEqualTo(status);
        assertThat(os.iniciadaEm()).isEmpty();
        assertThat(os.rejeicaoOrcamento()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO"})
    void inicioNaoPodeExistirAntesDaExecucao(StatusOS status) {
        assertThatIllegalArgumentException().isThrownBy(() -> restaurar(status, null, null, INICIO, null));
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"EM_EXECUCAO", "FINALIZADA", "ENTREGUE"})
    void inicioExigeOrcamento(StatusOS status) {
        Instant fim = status == StatusOS.EM_EXECUCAO ? null : INICIO.plusSeconds(1);
        assertThatIllegalArgumentException().isThrownBy(() -> restaurar(status, fim, null, INICIO, null));
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"FINALIZADA", "ENTREGUE"})
    void rejeitaCronologiaInvalida(StatusOS status) {
        assertThatIllegalArgumentException().isThrownBy(() -> restaurar(status, INICIO.minusSeconds(1), orcamento, INICIO, null));
    }

    @Test
    void rejeicaoExigeOrcamentoEEstadoAguardando() {
        RejeicaoOrcamento rejeicao = new RejeicaoOrcamento(INICIO, "Custo");
        assertThatIllegalArgumentException().isThrownBy(() ->
                restaurar(StatusOS.AGUARDANDO_APROVACAO, null, null, null, rejeicao));
        assertThatIllegalArgumentException().isThrownBy(() ->
                restaurar(StatusOS.EM_EXECUCAO, null, orcamento, null, rejeicao));
        assertThatIllegalArgumentException().isThrownBy(() ->
                restaurar(StatusOS.EM_EXECUCAO, null, orcamento, INICIO, rejeicao));
    }

    @Test
    void restauracaoPreservaValidacaoDeConclusaoAnteriorAoInicio() {
        OrdemDeServico os = restaurar(StatusOS.EM_EXECUCAO, null, orcamento, INICIO, null);
        assertThatIllegalArgumentException().isThrownBy(() -> os.transicionarPara(StatusOS.FINALIZADA, INICIO.minusSeconds(1)));
        assertThat(os.status()).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    private OrdemDeServico restaurar(StatusOS status, Instant fim, Orcamento orcamento,
                                     Instant inicio, RejeicaoOrcamento rejeicao) {
        return OrdemDeServico.restaurar(UUID.randomUUID(), "cliente", "veiculo", status, fim,
                itens, orcamento, inicio, rejeicao);
    }
}
