package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.*;
import br.com.grupo117.oficina.atendimento.domain.*;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DecidirOrcamentoTest {
    private static final Instant MOMENTO = Instant.parse("2026-10-20T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(MOMENTO, ZoneOffset.UTC);

    @Test
    void aprovaSomenteAposBaixaEPreservaOrcamento() {
        OrdemDeServico os = orcada();
        OrdensFake ordens = new OrdensFake(os);
        var orcamento = os.orcamento();
        BaixaEstoquePort estoque = id -> {
            ordens.chamadas.add("baixar");
            assertThat(id).isEqualTo(os.id());
            assertThat(os.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
            assertThat(os.iniciadaEm()).isEmpty();
        };
        OrdemDeServico resultado = new AprovarOrcamento(ordens, estoque, CLOCK).aprovar(os.id());
        assertThat(resultado.status()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(resultado.iniciadaEm()).contains(MOMENTO);
        assertThat(resultado.orcamento()).isEqualTo(orcamento);
        assertThat(ordens.chamadas).containsExactly("buscar", "baixar", "salvar");
    }

    @Test
    void falhaNaBaixaNaoIniciaOuSalvaOs() {
        OrdemDeServico os = orcada();
        OrdensFake ordens = new OrdensFake(os);
        BaixaEstoquePort estoque = id -> { throw new IllegalStateException("Reserva indisponível"); };
        assertThatIllegalStateException().isThrownBy(() -> new AprovarOrcamento(ordens, estoque, CLOCK).aprovar(os.id()));
        assertThat(ordens.chamadas).containsExactly("buscar");
        assertThat(os.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(os.iniciadaEm()).isEmpty();
    }

    @Test
    void falhaNoRelogioOcorreAntesDaBaixa() {
        OrdemDeServico os = orcada();
        OrdensFake ordens = new OrdensFake(os);
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenThrow(new IllegalStateException("Relógio indisponível"));
        assertThatIllegalStateException().isThrownBy(() ->
                new AprovarOrcamento(ordens, id -> ordens.chamadas.add("baixar"), clock).aprovar(os.id()));
        assertThat(ordens.chamadas).containsExactly("buscar");
        assertThat(os.iniciadaEm()).isEmpty();
    }

    @Test
    void rejeitaSemBaixaNemInicioDeExecucao() {
        OrdemDeServico os = orcada();
        OrdensFake ordens = new OrdensFake(os);
        OrdemDeServico resultado = new RejeitarOrcamento(ordens, CLOCK).rejeitar(os.id(), "Custo");
        assertThat(resultado.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(resultado.rejeicaoOrcamento().orElseThrow().momento()).isEqualTo(MOMENTO);
        assertThat(resultado.rejeicaoOrcamento().orElseThrow().motivo()).isEqualTo("Custo");
        assertThat(resultado.iniciadaEm()).isEmpty();
        assertThat(ordens.chamadas).containsExactly("buscar", "salvar");
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = "AGUARDANDO_APROVACAO", mode = EnumSource.Mode.EXCLUDE)
    void estadoInvalidoNaoDaBaixaNemSalva(StatusOS status) {
        Instant finalizada = status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE ? MOMENTO : null;
        OrdemDeServico os = OrdemDeServico.restaurar(UUID.randomUUID(), "cliente", "veiculo", status, finalizada);
        OrdensFake ordens = new OrdensFake(os);
        assertThatIllegalStateException().isThrownBy(() -> aprovar(ordens).aprovar(os.id()));
        assertThatIllegalStateException().isThrownBy(() -> new RejeitarOrcamento(ordens, CLOCK).rejeitar(os.id(), "Custo"));
        assertThat(ordens.chamadas).containsExactly("buscar", "buscar");
        assertThat(os.status()).isEqualTo(status);
    }

    @Test
    void semOrcamentoNaoDaBaixa() {
        OrdemDeServico os = OrdemDeServico.restaurar(UUID.randomUUID(), "cliente", "veiculo", StatusOS.AGUARDANDO_APROVACAO, null);
        OrdensFake ordens = new OrdensFake(os);
        assertThatIllegalStateException().isThrownBy(() -> aprovar(ordens).aprovar(os.id()));
        assertThat(ordens.chamadas).containsExactly("buscar");
    }

    @Test
    void rejeicaoPreviaImpedeBaixaOuNovoSalvamento() {
        OrdemDeServico os = orcada();
        os.rejeitarOrcamento(MOMENTO, "Preço");
        OrdensFake ordens = new OrdensFake(os);
        assertThatIllegalStateException().isThrownBy(() -> aprovar(ordens).aprovar(os.id()));
        assertThatIllegalStateException().isThrownBy(() -> new RejeitarOrcamento(ordens, CLOCK).rejeitar(os.id(), "Outro"));
        assertThat(ordens.chamadas).containsExactly("buscar", "buscar");
    }

    @Test
    void aprovacaoRepetidaNaoDaBaixaDuasVezes() {
        OrdemDeServico os = orcada();
        OrdensFake ordens = new OrdensFake(os);
        AprovarOrcamento caso = aprovar(ordens);
        caso.aprovar(os.id());
        assertThatIllegalStateException().isThrownBy(() -> caso.aprovar(os.id()));
        assertThat(ordens.chamadas).containsExactly("buscar", "baixar", "salvar", "buscar");
        assertThat(os.iniciadaEm()).contains(MOMENTO);
    }

    @Test
    void motivoInvalidoNaoSalva() {
        OrdemDeServico os = orcada();
        OrdensFake ordens = new OrdensFake(os);
        assertThatIllegalArgumentException().isThrownBy(() -> new RejeitarOrcamento(ordens, CLOCK).rejeitar(os.id(), " "));
        assertThat(os.rejeicaoOrcamento()).isEmpty();
        assertThat(ordens.chamadas).containsExactly("buscar");
    }

    @Test
    void osAusenteOuIdNuloNaoDaBaixaOuSalva() {
        OrdensFake ordens = new OrdensFake(null);
        AprovarOrcamento aprovar = aprovar(ordens);
        RejeitarOrcamento rejeitar = new RejeitarOrcamento(ordens, CLOCK);
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> aprovar.aprovar(id)).isInstanceOf(OrdemDeServicoNaoEncontradaException.class);
        assertThatThrownBy(() -> rejeitar.rejeitar(id, "Preço")).isInstanceOf(OrdemDeServicoNaoEncontradaException.class);
        assertThatNullPointerException().isThrownBy(() -> aprovar.aprovar(null));
        assertThatNullPointerException().isThrownBy(() -> rejeitar.rejeitar(null, "Preço"));
        assertThat(ordens.chamadas).containsExactly("buscar", "buscar");
    }

    private AprovarOrcamento aprovar(OrdensFake ordens) {
        return new AprovarOrcamento(ordens, id -> ordens.chamadas.add("baixar"), CLOCK);
    }

    private OrdemDeServico orcada() {
        OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), "cliente", "veiculo");
        for (TipoItemOS tipo : TipoItemOS.values()) {
            os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), tipo, "Item", 2, new Dinheiro(new BigDecimal("10.00"))));
        }
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        os.gerarOrcamento();
        return os;
    }

    private static class OrdensFake implements OrdemDeServicoRepositorio {
        private final OrdemDeServico os;
        private final List<String> chamadas = new ArrayList<>();
        OrdensFake(OrdemDeServico os) { this.os = os; }
        public Optional<OrdemDeServico> buscarPorId(UUID id) {
            chamadas.add("buscar");
            return Optional.ofNullable(os).filter(o -> o.id().equals(id));
        }
        public List<OrdemDeServico> listar() { return os == null ? List.of() : List.of(os); }
        public OrdemDeServico salvar(OrdemDeServico os) { chamadas.add("salvar"); return os; }
    }
}
