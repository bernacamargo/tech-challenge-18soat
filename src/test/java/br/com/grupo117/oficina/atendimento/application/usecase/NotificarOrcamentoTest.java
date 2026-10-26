package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.port.*;
import br.com.grupo117.oficina.atendimento.domain.*;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class NotificarOrcamentoTest {
    @Test
    void ambosOsCaminhosAgendamAposSalvarENaoAgendamNaRepeticao() {
        for (boolean diagnostico : new boolean[]{true, false}) {
            var repo = mock(OrdemDeServicoRepositorio.class);
            var notificacoes = mock(NotificacaoPort.class);
            var os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
            os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                    "Troca", 1, new Dinheiro(new BigDecimal("80.50"))));
            if (diagnostico) os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
            when(repo.buscarPorId(os.id())).thenReturn(Optional.of(os));
            when(repo.salvar(os)).thenReturn(os);
            Runnable executar = diagnostico ? () -> new GerarOrcamento(repo, notificacoes).gerar(os.id())
                    : () -> new SubmeterOrdemDeServico(repo, notificacoes).submeter(os.id());
            executar.run();
            var ordem = inOrder(repo, notificacoes);
            ordem.verify(repo).salvar(os);
            ordem.verify(notificacoes).agendarOrcamento(os);
            assertThatIllegalStateException().isThrownBy(executar::run);
            verify(notificacoes, times(1)).agendarOrcamento(os);
        }
    }

    @Test
    void naoAgendaQuandoASalvacaoFalha() {
        var repo = mock(OrdemDeServicoRepositorio.class);
        var notificacoes = mock(NotificacaoPort.class);
        var os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
        os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Troca", 1, new Dinheiro(new BigDecimal("80.50"))));
        when(repo.buscarPorId(os.id())).thenReturn(Optional.of(os));
        when(repo.salvar(any())).thenThrow(new IllegalStateException("Falha"));
        assertThatIllegalStateException().isThrownBy(() -> new SubmeterOrdemDeServico(repo, notificacoes).submeter(os.id()));
        verifyNoInteractions(notificacoes);
    }
}
