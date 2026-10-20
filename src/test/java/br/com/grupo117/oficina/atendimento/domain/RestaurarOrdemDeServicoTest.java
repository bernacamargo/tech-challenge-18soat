package br.com.grupo117.oficina.atendimento.domain;

import java.time.Instant;
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

    private boolean concluida(StatusOS status) {
        return status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE;
    }
}
