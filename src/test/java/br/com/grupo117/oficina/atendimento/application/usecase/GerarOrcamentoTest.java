package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.*;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;

class GerarOrcamentoTest {
    @Test
    void calculaOrcamentoESalvaEmAguardandoAprovacao() {
        OrdemDeServico os = ordemNoStatus(StatusOS.EM_DIAGNOSTICO);
        os.incluirItem(item(TipoItemOS.SERVICO, "50.00", 2));
        os.incluirItem(item(TipoItemOS.PECA, "10.50", 3));
        OrdensFake ordens = new OrdensFake(os);

        OrdemDeServico resultado = new GerarOrcamento(ordens).gerar(os.id());

        assertThat(resultado).isSameAs(os);
        assertThat(resultado.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(resultado.orcamento().orElseThrow().total().valor()).isEqualByComparingTo("131.50");
        assertThat(ordens.salvamentos).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = "EM_DIAGNOSTICO", mode = EnumSource.Mode.EXCLUDE)
    void estadoInvalidoNaoSalva(StatusOS status) {
        OrdemDeServico os = ordemNoStatus(status);
        OrdensFake ordens = new OrdensFake(os);
        GerarOrcamento casoDeUso = new GerarOrcamento(ordens);
        assertThatIllegalStateException().isThrownBy(() -> casoDeUso.gerar(os.id()));
        assertThat(os.status()).isEqualTo(status);
        assertThat(os.orcamento()).isEmpty();
        assertThat(ordens.salvamentos).isZero();
    }

    @Test
    void semItensNaoSalvaOuMudaStatus() {
        OrdemDeServico os = ordemNoStatus(StatusOS.EM_DIAGNOSTICO);
        OrdensFake ordens = new OrdensFake(os);
        GerarOrcamento casoDeUso = new GerarOrcamento(ordens);
        assertThatIllegalArgumentException().isThrownBy(() -> casoDeUso.gerar(os.id()));
        assertThat(os.status()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        assertThat(ordens.salvamentos).isZero();
    }

    @Test
    void osAusenteOuIdNuloNaoSalva() {
        OrdensFake ordens = new OrdensFake(null);
        GerarOrcamento casoDeUso = new GerarOrcamento(ordens);
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> casoDeUso.gerar(id)).isInstanceOf(OrdemDeServicoNaoEncontradaException.class);
        assertThatNullPointerException().isThrownBy(() -> casoDeUso.gerar(null));
        assertThat(ordens.salvamentos).isZero();
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

    private static final class OrdensFake implements OrdemDeServicoRepositorio {
        private final OrdemDeServico os;
        private int salvamentos;
        OrdensFake(OrdemDeServico os) { this.os = os; }
        @Override
        public Optional<OrdemDeServico> buscarPorId(UUID id) {
            return Optional.ofNullable(os).filter(o -> o.id().equals(id));
        }
        @Override
        public List<OrdemDeServico> listar() { return os == null ? List.of() : List.of(os); }
        @Override
        public OrdemDeServico salvar(OrdemDeServico os) { salvamentos++; return os; }
    }
}
