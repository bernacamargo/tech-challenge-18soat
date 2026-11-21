package br.com.grupo117.oficina.atendimento.application.usecase;

import br.com.grupo117.oficina.atendimento.application.CadastrarVeiculo;
import br.com.grupo117.oficina.atendimento.application.ClienteRepositorio;
import br.com.grupo117.oficina.atendimento.application.IdentificarCliente;
import br.com.grupo117.oficina.atendimento.application.VeiculoRepositorio;
import br.com.grupo117.oficina.atendimento.application.port.BaixaEstoquePort;
import br.com.grupo117.oficina.atendimento.application.port.CatalogoOSPort;
import br.com.grupo117.oficina.atendimento.application.port.NotificacaoPort;
import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.application.port.ReservaEstoquePort;
import br.com.grupo117.oficina.atendimento.domain.Cliente;
import br.com.grupo117.oficina.atendimento.domain.CpfCnpj;
import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificarStatusTest {
    private static final Instant MOMENTO = Instant.parse("2026-11-18T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(MOMENTO, ZoneOffset.UTC);

    @Test
    void aberturaAgendaRecebidaDepoisDeSalvar() {
        var notificacoes = mock(NotificacaoPort.class);
        var ordens = mock(OrdemDeServicoRepositorio.class);
        when(ordens.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        CriarOrdemDeServico caso = casoDeAbertura(ordens, notificacoes);

        OrdemDeServico os = caso.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);

        var ordem = inOrder(ordens, notificacoes);
        ordem.verify(ordens).salvar(any());
        ordem.verify(notificacoes).agendar(os);
        verify(notificacoes, times(1)).agendar(os);
        assertThat(os.status()).isEqualTo(StatusOS.RECEBIDA);
    }

    @Test
    void falhaAoSalvarAberturaNaoAgenda() {
        var notificacoes = mock(NotificacaoPort.class);
        var ordens = mock(OrdemDeServicoRepositorio.class);
        when(ordens.salvar(any())).thenThrow(new IllegalStateException("Falha"));
        assertThatIllegalStateException().isThrownBy(() ->
                casoDeAbertura(ordens, notificacoes).criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012));
        verifyNoInteractions(notificacoes);
    }

    @Test
    void diagnosticoConclusaoEEntregaAgendamUmaVez() {
        var repo = mock(OrdemDeServicoRepositorio.class);
        var notificacoes = mock(NotificacaoPort.class);
        when(repo.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        List<StatusOS> agendados = new ArrayList<>();
        doAnswer(inv -> {
            agendados.add(inv.<OrdemDeServico>getArgument(0).status());
            return null;
        }).when(notificacoes).agendar(any());
        OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
        os.incluirItem(item());
        when(repo.buscarPorId(os.id())).thenReturn(Optional.of(os));

        new IniciarDiagnostico(repo, notificacoes).iniciar(os.id());
        assertThatIllegalStateException().isThrownBy(() -> new IniciarDiagnostico(repo, notificacoes).iniciar(os.id()));
        os.gerarOrcamento();
        os.aprovarOrcamento(MOMENTO);
        new ConcluirServicos(repo, CLOCK, notificacoes).concluir(os.id());
        assertThatIllegalStateException().isThrownBy(() -> new ConcluirServicos(repo, CLOCK, notificacoes).concluir(os.id()));
        new RegistrarEntrega(repo, notificacoes).registrar(os.id());
        assertThatIllegalStateException().isThrownBy(() -> new RegistrarEntrega(repo, notificacoes).registrar(os.id()));

        assertThat(agendados).containsExactly(StatusOS.EM_DIAGNOSTICO, StatusOS.FINALIZADA, StatusOS.ENTREGUE);
    }

    @Test
    void falhaAoSalvarTransicaoNaoAgenda() {
        var repo = mock(OrdemDeServicoRepositorio.class);
        var notificacoes = mock(NotificacaoPort.class);
        OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
        when(repo.buscarPorId(os.id())).thenReturn(Optional.of(os));
        when(repo.salvar(any())).thenThrow(new IllegalStateException("Falha"));
        assertThatIllegalStateException().isThrownBy(() -> new IniciarDiagnostico(repo, notificacoes).iniciar(os.id()));
        verifyNoInteractions(notificacoes);
    }

    @Test
    void aprovacaoERejeicaoAgendamDepoisDeSalvarENaoRepetem() {
        var aprovacao = mock(NotificacaoPort.class);
        var repoAprovacao = mock(OrdemDeServicoRepositorio.class);
        OrdemDeServico aprovada = orcada();
        when(repoAprovacao.buscarPorId(aprovada.id())).thenReturn(Optional.of(aprovada));
        when(repoAprovacao.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        new AprovarOrcamento(repoAprovacao, id -> { }, CLOCK, aprovacao).aprovar(aprovada.id());
        assertThatIllegalStateException().isThrownBy(() ->
                new AprovarOrcamento(repoAprovacao, id -> { }, CLOCK, aprovacao).aprovar(aprovada.id()));
        verify(aprovacao, times(1)).agendar(aprovada);

        var rejeicao = mock(NotificacaoPort.class);
        var repoRejeicao = mock(OrdemDeServicoRepositorio.class);
        OrdemDeServico rejeitada = orcada();
        when(repoRejeicao.buscarPorId(rejeitada.id())).thenReturn(Optional.of(rejeitada));
        when(repoRejeicao.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        new RejeitarOrcamento(repoRejeicao, id -> { }, CLOCK, rejeicao).rejeitar(rejeitada.id(), "Preço alto");
        assertThatIllegalStateException().isThrownBy(() ->
                new RejeitarOrcamento(repoRejeicao, id -> { }, CLOCK, rejeicao).rejeitar(rejeitada.id(), "Outro"));
        verify(rejeicao, times(1)).agendar(rejeitada);
    }

    @Test
    void falhaAoSalvarDecisaoNaoAgenda() {
        var notificacoes = mock(NotificacaoPort.class);
        var repo = mock(OrdemDeServicoRepositorio.class);
        OrdemDeServico os = orcada();
        when(repo.buscarPorId(os.id())).thenReturn(Optional.of(os));
        when(repo.salvar(any())).thenThrow(new IllegalStateException("Falha"));
        BaixaEstoquePort baixa = id -> { };
        assertThatIllegalStateException().isThrownBy(() ->
                new AprovarOrcamento(repo, baixa, CLOCK, notificacoes).aprovar(os.id()));
        verifyNoInteractions(notificacoes);
    }

    private static OrdemDeServico orcada() {
        OrdemDeServico os = new OrdemDeServico(UUID.randomUUID(), "52998224725", "ABC1D23");
        os.incluirItem(item());
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        os.gerarOrcamento();
        return os;
    }

    private static ItemDaOS item() {
        return new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Revisão", 1, new Dinheiro(new BigDecimal("80.50")));
    }

    private static CriarOrdemDeServico casoDeAbertura(OrdemDeServicoRepositorio ordens, NotificacaoPort notificacoes) {
        Map<CpfCnpj, Cliente> clientesSalvos = new HashMap<>();
        ClienteRepositorio clientes = mock(ClienteRepositorio.class);
        when(clientes.buscarPorCpfCnpj(any())).thenAnswer(inv -> Optional.ofNullable(clientesSalvos.get(inv.getArgument(0))));
        when(clientes.salvar(any())).thenAnswer(inv -> {
            Cliente cliente = inv.getArgument(0);
            clientesSalvos.put(cliente.cpfCnpj(), cliente);
            return cliente;
        });
        VeiculoRepositorio veiculos = mock(VeiculoRepositorio.class);
        when(veiculos.buscarPorPlaca(any())).thenReturn(Optional.empty());
        when(veiculos.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        return new CriarOrdemDeServico(new IdentificarCliente(clientes), new CadastrarVeiculo(clientes, veiculos),
                veiculos, ordens, mock(CatalogoOSPort.class), mock(ReservaEstoquePort.class), notificacoes);
    }
}
