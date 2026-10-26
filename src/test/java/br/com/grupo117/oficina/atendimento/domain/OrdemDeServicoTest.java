package br.com.grupo117.oficina.atendimento.domain;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class OrdemDeServicoTest {

    private static final String CLIENTE_ID = "52998224725";
    private static final String VEICULO_ID = "ABC1D23";

    @Test
    void nasceRecebidaComIdentificadoresDoClienteEVeiculo() {
        UUID id = UUID.randomUUID();
        OrdemDeServico os = new OrdemDeServico(id, CLIENTE_ID, VEICULO_ID);

        assertThat(os.id()).isEqualTo(id);
        assertThat(os.clienteId()).isEqualTo(CLIENTE_ID);
        assertThat(os.veiculoId()).isEqualTo(VEICULO_ID);
        assertThat(os.status()).isEqualTo(StatusOS.RECEBIDA);
    }

    @ParameterizedTest
    @MethodSource("transicoesValidas")
    void permiteTransicaoPrevista(StatusOS origem, StatusOS destino) {
        OrdemDeServico os = ordemNoStatus(origem);

        os.transicionarPara(destino);

        assertThat(os.status()).isEqualTo(destino);
    }

    @ParameterizedTest
    @MethodSource("transicoesInvalidas")
    void rejeitaTransicaoInvalidaSemAlterarEstado(StatusOS origem, StatusOS destino) {
        OrdemDeServico os = ordemNoStatus(origem);

        assertThatIllegalStateException().isThrownBy(() -> os.transicionarPara(destino))
                .withMessage("Transição de " + origem + " para " + destino + " não permitida");
        assertThat(os.status()).isEqualTo(origem);
    }

    @Test
    void rejeitaDestinoNuloSemAlterarEstado() {
        OrdemDeServico os = ordemNoStatus(StatusOS.RECEBIDA);

        assertThatNullPointerException().isThrownBy(() -> os.transicionarPara(null));
        assertThat(os.status()).isEqualTo(StatusOS.RECEBIDA);
    }

    @Test
    void rejeitaIdentificadorDaOsNulo() {
        assertThatNullPointerException()
                .isThrownBy(() -> new OrdemDeServico(null, CLIENTE_ID, VEICULO_ID));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejeitaClienteAusente(String clienteId) {
        UUID id = UUID.randomUUID();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrdemDeServico(id, clienteId, VEICULO_ID));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejeitaVeiculoAusente(String veiculoId) {
        UUID id = UUID.randomUUID();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrdemDeServico(id, CLIENTE_ID, veiculoId));
    }

    private static Stream<Arguments> transicoesValidas() {
        return Stream.of(
                Arguments.of(StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO),
                Arguments.of(StatusOS.RECEBIDA, StatusOS.AGUARDANDO_APROVACAO),
                Arguments.of(StatusOS.EM_DIAGNOSTICO, StatusOS.AGUARDANDO_APROVACAO),
                Arguments.of(StatusOS.AGUARDANDO_APROVACAO, StatusOS.EM_EXECUCAO),
                Arguments.of(StatusOS.EM_EXECUCAO, StatusOS.FINALIZADA),
                Arguments.of(StatusOS.FINALIZADA, StatusOS.ENTREGUE));
    }

    private static Stream<Arguments> transicoesInvalidas() {
        return Arrays.stream(StatusOS.values()).flatMap(origem -> Arrays.stream(StatusOS.values())
                .filter(destino -> !ehTransicaoValida(origem, destino))
                .map(destino -> Arguments.of(origem, destino)));
    }

    private static boolean ehTransicaoValida(StatusOS origem, StatusOS destino) {
        return transicoesValidas().anyMatch(par -> par.get()[0] == origem && par.get()[1] == destino);
    }

    private static OrdemDeServico ordemNoStatus(StatusOS status) {
        OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), CLIENTE_ID, VEICULO_ID);
        for (StatusOS etapa : StatusOS.values()) {
            if (etapa == StatusOS.RECEBIDA) {
                continue;
            }
            if (os.status() == status) {
                break;
            }
            os.transicionarPara(etapa);
        }
        return os;
    }
}
