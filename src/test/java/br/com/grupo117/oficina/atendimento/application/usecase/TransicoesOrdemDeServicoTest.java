package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransicoesOrdemDeServicoTest {

    private static final Instant MOMENTO = Instant.parse("2026-10-12T17:00:00Z");
    private static final Clock RELOGIO = Clock.fixed(MOMENTO, ZoneOffset.UTC);

    @ParameterizedTest
    @MethodSource("combinacoes")
    void aplicaSomenteTransicaoPermitida(String acao, StatusOS origem) {
        OrdensFake ordens = new OrdensFake(ordemNoStatus(origem));
        Function<UUID, OrdemDeServico> executar = acao(acao, ordens);
        StatusOS esperado = switch (acao) {
            case "diagnostico" -> StatusOS.RECEBIDA;
            case "conclusao" -> StatusOS.EM_EXECUCAO;
            case "entrega" -> StatusOS.FINALIZADA;
            default -> throw new IllegalArgumentException(acao);
        };
        Optional<Instant> finalizacaoAnterior = ordens.os.finalizadaEm();

        if (origem == esperado) {
            OrdemDeServico resultado = executar.apply(ordens.os.id());
            StatusOS destino = switch (acao) {
                case "diagnostico" -> StatusOS.EM_DIAGNOSTICO;
                case "conclusao" -> StatusOS.FINALIZADA;
                case "entrega" -> StatusOS.ENTREGUE;
                default -> throw new IllegalArgumentException(acao);
            };
            assertThat(resultado).isSameAs(ordens.os);
            assertThat(resultado.status()).isEqualTo(destino);
            assertThat(ordens.salvamentos).isEqualTo(1);
            assertThat(resultado.finalizadaEm()).isEqualTo(
                    acao.equals("conclusao") ? Optional.of(MOMENTO) : finalizacaoAnterior);
        } else {
            assertThatIllegalStateException().isThrownBy(() -> executar.apply(ordens.os.id()));
            assertThat(ordens.os.status()).isEqualTo(origem);
            assertThat(ordens.os.finalizadaEm()).isEqualTo(finalizacaoAnterior);
            assertThat(ordens.salvamentos).isZero();
        }
    }

    @ParameterizedTest
    @MethodSource("acoes")
    void osAusenteNaoESalva(String nome) {
        OrdensFake ordens = new OrdensFake(null);
        UUID id = UUID.randomUUID();
        Function<UUID, OrdemDeServico> executar = acao(nome, ordens);

        assertThatThrownBy(() -> executar.apply(id))
                .isInstanceOf(OrdemDeServicoNaoEncontradaException.class)
                .hasMessage("Ordem de serviço não encontrada: " + id);
        assertThat(ordens.salvamentos).isZero();
    }

    @ParameterizedTest
    @MethodSource("acoes")
    void idNuloNaoConsultaRepositorio(String nome) {
        OrdensFake ordens = new OrdensFake(null);
        Function<UUID, OrdemDeServico> executar = acao(nome, ordens);

        assertThatNullPointerException().isThrownBy(() -> executar.apply(null));
        assertThat(ordens.consultas).isZero();
        assertThat(ordens.salvamentos).isZero();
    }

    @Test
    void momentoNuloNaoAlteraStatusOuFinalizacao() {
        OrdemDeServico os = ordemNoStatus(StatusOS.EM_EXECUCAO);

        assertThatNullPointerException().isThrownBy(() -> os.transicionarPara(StatusOS.FINALIZADA, null));
        assertThat(os.status()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(os.finalizadaEm()).isEmpty();
    }

    @Test
    void entregaPreservaMomentoDaConclusao() {
        OrdensFake ordens = new OrdensFake(ordemNoStatus(StatusOS.EM_EXECUCAO));
        new ConcluirServicos(ordens, RELOGIO).concluir(ordens.os.id());

        new RegistrarEntrega(ordens).registrar(ordens.os.id());

        assertThat(ordens.os.status()).isEqualTo(StatusOS.ENTREGUE);
        assertThat(ordens.os.finalizadaEm()).contains(MOMENTO);
        assertThat(ordens.salvamentos).isEqualTo(2);
    }

    private static Function<UUID, OrdemDeServico> acao(String nome, OrdensFake ordens) {
        return switch (nome) {
            case "diagnostico" -> new IniciarDiagnostico(ordens)::iniciar;
            case "conclusao" -> new ConcluirServicos(ordens, RELOGIO)::concluir;
            case "entrega" -> new RegistrarEntrega(ordens)::registrar;
            default -> throw new IllegalArgumentException(nome);
        };
    }

    private static Stream<String> acoes() {
        return Stream.of("diagnostico", "conclusao", "entrega");
    }

    private static Stream<Arguments> combinacoes() {
        return acoes().flatMap(acao -> Arrays.stream(StatusOS.values()).map(status -> Arguments.of(acao, status)));
    }

    private static OrdemDeServico ordemNoStatus(StatusOS status) {
        OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
        for (StatusOS etapa : StatusOS.values()) {
            if (os.status() == status) {
                break;
            }
            if (etapa != StatusOS.RECEBIDA) {
                os.transicionarPara(etapa, MOMENTO.minusSeconds(60));
            }
        }
        return os;
    }

    private static final class OrdensFake implements OrdemDeServicoRepositorio {
        private final OrdemDeServico os;
        private int consultas;
        private int salvamentos;

        private OrdensFake(OrdemDeServico os) {
            this.os = os;
        }

        @Override
        public Optional<OrdemDeServico> buscarPorId(UUID id) {
            consultas++;
            return Optional.ofNullable(os).filter(ordem -> ordem.id().equals(id));
        }

        @Override
        public List<OrdemDeServico> listar() {
            return os == null ? List.of() : List.of(os);
        }

        @Override
        public OrdemDeServico salvar(OrdemDeServico ordem) {
            salvamentos++;
            return ordem;
        }
    }
}
