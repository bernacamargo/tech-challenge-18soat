package br.com.grupo117.oficina.atendimento.domain;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Orcamento;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RestaurarOrdemDeServicoTest {
    private static final Instant MOMENTO = Instant.parse("2026-10-12T17:00:00Z");

    @ParameterizedTest
    @EnumSource(StatusOS.class)
    void restauraEstadoPersistido(StatusOS status) {
        UUID id = UUID.randomUUID();
        Instant finalizacao = concluida(status) ? MOMENTO : null;
        OrdemDeServico os = OrdemDeServico.restaurar(id, "52998224725", "ABC1D23", status, finalizacao);
        assertThat(os.id()).isEqualTo(id);
        assertThat(os.status()).isEqualTo(status);
        assertThat(os.finalizadaEm().orElse(null)).isEqualTo(finalizacao);
    }

    @ParameterizedTest
    @EnumSource(StatusOS.class)
    void rejeitaFinalizacaoIncompativel(StatusOS status) {
        UUID id = UUID.randomUUID();
        Instant finalizacao = concluida(status) ? null : MOMENTO;
        assertThatIllegalArgumentException().isThrownBy(
                () -> OrdemDeServico.restaurar(id, "52998224725", "ABC1D23", status, finalizacao));
    }

    @Test
    void rejeitaStatusAusente() {
        UUID id = UUID.randomUUID();
        assertThatNullPointerException().isThrownBy(
                () -> OrdemDeServico.restaurar(id, "52998224725", "ABC1D23", null, null));
    }

    @Test
    void restauraItensComCopiaDefensiva() {
        ItemDaOS item = item();
        var itens = new ArrayList<>(List.of(item));
        OrdemDeServico os = restaurar(StatusOS.EM_DIAGNOSTICO, itens, null);
        itens.clear();
        assertThat(os.itens()).containsExactly(item);
        assertThat(os.orcamento()).isEmpty();
    }

    @Test
    void rejeitaIdentificadoresDeItemDuplicados() {
        ItemDaOS item = item();
        assertThatIllegalArgumentException().isThrownBy(() ->
                restaurar(StatusOS.EM_DIAGNOSTICO, List.of(item, item), null));
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"RECEBIDA", "EM_DIAGNOSTICO"})
    void rejeitaOrcamentoEmEstadoAnteriorAGeracao(StatusOS status) {
        var itens = List.of(item());
        var orcamento = new Orcamento(itens);
        assertThatIllegalArgumentException().isThrownBy(() -> restaurar(status, itens, orcamento));
    }

    @Test
    void rejeitaOrcamentoComItensDiferentes() {
        var itens = List.of(item());
        var orcamento = new Orcamento(List.of(item()));
        assertThatIllegalArgumentException().isThrownBy(() ->
                restaurar(StatusOS.AGUARDANDO_APROVACAO, itens, orcamento));
    }

    private OrdemDeServico restaurar(StatusOS status, List<ItemDaOS> itens,
            Orcamento orcamento) {
        return OrdemDeServico.restaurar(UUID.randomUUID(), "52998224725", "ABC1D23", status, null, itens, orcamento);
    }

    private ItemDaOS item() {
        return new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO, "Diagnóstico", 1,
                new Dinheiro(new BigDecimal("10.00")));
    }

    private boolean concluida(StatusOS status) {
        return status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE;
    }
}
