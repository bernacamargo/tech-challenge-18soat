package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.*;
import br.com.grupo117.oficina.atendimento.domain.*;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import br.com.grupo117.oficina.atendimento.infrastructure.service.ItensOrcamentoOSService;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import br.com.grupo117.oficina.catalogoestoque.application.port.EstoqueDaOrdemDeServico;
import br.com.grupo117.oficina.catalogoestoque.application.port.PecaRepositorio;
import br.com.grupo117.oficina.catalogoestoque.application.port.ServicoRepositorio;
import br.com.grupo117.oficina.catalogoestoque.domain.Peca;
import br.com.grupo117.oficina.catalogoestoque.domain.Servico;
import br.com.grupo117.oficina.catalogoestoque.domain.TipoItemEstoque;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

class IntegracaoEstoqueOSIntegrationTest extends AbstractIntegrationTest {
    @Autowired private ItensOrcamentoOSService operacoes;
    @Autowired private OrdemDeServicoService atendimento;
    @Autowired private OrdemDeServicoRepositorio ordens;
    @Autowired private PecaRepositorio pecas;
    @Autowired private ServicoRepositorio servicos;
    @Autowired private ReservaEstoquePort reserva;
    @Autowired private BaixaEstoquePort baixa;
    @Autowired private EstoqueDaOrdemDeServico estoque;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager tx;

    @BeforeEach
    void limparEstoque() { jdbc.execute("truncate peca cascade"); }

    @Test
    void incluiItensComPrecoDoCatalogoEReservaSemBaixaAteAprovacao() {
        OrdemDeServico os = criar();
        Servico servico = servicos.salvar(Servico.cadastrar("Troca de óleo", null, new BigDecimal("80.50")));
        Peca peca = peca(10);
        operacoes.incluirServico(os.id(), servico.id(), 1);
        operacoes.incluirPeca(os.id(), peca.id(), 3);
        operacoes.incluirPeca(os.id(), peca.id(), 2);
        assertSaldo(peca.id(), 10, 5);
        assertThat(qtd("reserva_item_os")).isEqualTo(2);
        assertThat(qtd("reserva_de_estoque")).isEqualTo(2);
        servicos.salvar(servico.alterar("Outro nome", null, new BigDecimal("999.00")));
        atendimento.iniciarDiagnostico(os.id());
        assertThat(atendimento.gerarOrcamento(os.id()).orcamento().orElseThrow().total().valor()).isEqualByComparingTo("206.75");
        assertSaldo(peca.id(), 10, 5);
        Instant antes = Instant.now();
        OrdemDeServico aprovada = operacoes.aprovar(os.id());
        assertThat(aprovada.status()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(aprovada.iniciadaEm().orElseThrow()).isBetween(antes.minusMillis(1), Instant.now());
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().iniciadaEm()).isEqualTo(aprovada.iniciadaEm());
        assertSaldo(peca.id(), 5, 0);
        assertThat(jdbc.queryForObject("select count(*) from reserva_de_estoque where status = 'BAIXADA'", Integer.class)).isEqualTo(2);
        assertThatIllegalStateException().isThrownBy(() -> operacoes.aprovar(os.id()));
        new TransactionTemplate(tx).executeWithoutResult(s -> baixa.baixarReservas(os.id()));
        assertSaldo(peca.id(), 5, 0);
    }

    @Test
    void osSoComServicoPodeSerAprovadaSemReserva() {
        OrdemDeServico os = criar();
        Servico servico = servicos.salvar(Servico.cadastrar("Diagnóstico", null, new BigDecimal("50.00")));
        operacoes.incluirServico(os.id(), servico.id(), 2);
        orcar(os.id());
        assertThat(operacoes.aprovar(os.id()).status()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(qtd("reserva_de_estoque")).isZero();
    }

    @Test
    void saldoInsuficienteNaoIncluiItemOuCriaReserva() {
        OrdemDeServico os = criar();
        Peca peca = peca(1);
        assertThatIllegalStateException().isThrownBy(() -> operacoes.incluirPeca(os.id(), peca.id(), 2));
        assertSaldo(peca.id(), 1, 0);
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().itens()).isEmpty();
        assertThat(qtd("reserva_item_os")).isZero();
        assertThat(qtd("reserva_de_estoque")).isZero();
    }

    @Test
    void falhaNoItemDesfazReservaVinculoESaldo() {
        OrdemDeServico os = criar();
        Peca peca = peca(5);
        jdbc.execute("alter table item_da_os add constraint ck_item_falha_integracao check (false)");
        try {
            assertThatThrownBy(() -> operacoes.incluirPeca(os.id(), peca.id(), 2)).isInstanceOf(RuntimeException.class);
            assertSaldo(peca.id(), 5, 0);
            assertThat(qtd("reserva_item_os")).isZero();
            assertThat(qtd("reserva_de_estoque")).isZero();
            assertThat(ordens.buscarPorId(os.id()).orElseThrow().itens()).isEmpty();
        } finally {
            jdbc.execute("alter table item_da_os drop constraint ck_item_falha_integracao");
        }
    }

    @Test
    void falhaNaOsDesfazBaixaDeTodasAsPecasEPreservaReservasAtivas() {
        OrdemDeServico os = criar();
        Peca a = peca(5);
        Peca b = peca(7);
        operacoes.incluirPeca(os.id(), a.id(), 2);
        operacoes.incluirPeca(os.id(), b.id(), 3);
        orcar(os.id());
        jdbc.execute("alter table ordem_de_servico add constraint ck_os_falha_integracao check (status <> 'EM_EXECUCAO')");
        try {
            assertThatThrownBy(() -> operacoes.aprovar(os.id())).isInstanceOf(RuntimeException.class);
            assertSaldo(a.id(), 5, 2);
            assertSaldo(b.id(), 7, 3);
            OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
            assertThat(lida.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
            assertThat(lida.iniciadaEm()).isEmpty();
            assertThat(jdbc.queryForObject("select count(*) from reserva_de_estoque where status = 'ATIVA'", Integer.class)).isEqualTo(2);
        } finally {
            jdbc.execute("alter table ordem_de_servico drop constraint ck_os_falha_integracao");
        }
    }

    @Test
    void reservaDoMesmoItemEIdempotenteMasParametrosDivergentesSaoRecusados() {
        OrdemDeServico os = criar();
        Peca peca = peca(5);
        UUID itemId = UUID.randomUUID();
        new TransactionTemplate(tx).executeWithoutResult(s -> {
            assertThat(reserva.reservar(os.id(), itemId, peca.id(), 2)).isTrue();
            assertThat(reserva.reservar(os.id(), itemId, peca.id(), 2)).isTrue();
            OrdemDeServico atual = ordens.buscarPorId(os.id()).orElseThrow();
            atual.incluirItem(new ItemDaOS(itemId, peca.id(), TipoItemOS.PECA, peca.nome(), 2, new Dinheiro(peca.precoUnitario().valor())));
            ordens.salvar(atual);
        });
        assertSaldo(peca.id(), 5, 2);
        assertThat(qtd("reserva_de_estoque")).isEqualTo(1);
        assertThatIllegalStateException().isThrownBy(() -> new TransactionTemplate(tx).executeWithoutResult(s ->
                reserva.reservar(os.id(), itemId, peca.id(), 3)));
        assertSaldo(peca.id(), 5, 2);
    }

    @Test
    void naoAprovaPecaLegadaSemVinculoDeReserva() {
        OrdemDeServico os = criar();
        Peca peca = peca(5);
        os.incluirItem(new ItemDaOS(UUID.randomUUID(), peca.id(), TipoItemOS.PECA, peca.nome(), 2, new Dinheiro(peca.precoUnitario().valor())));
        ordens.salvar(os);
        orcar(os.id());
        assertThatIllegalStateException().isThrownBy(() -> operacoes.aprovar(os.id()));
        assertSaldo(peca.id(), 5, 0);
    }

    @Test
    void naoConsomeReservaExtraCriadaForaDosItensDaOs() {
        OrdemDeServico os = criar();
        Peca peca = peca(10);
        operacoes.incluirPeca(os.id(), peca.id(), 2);
        estoque.reservar(os.id(), peca.id(), 1);
        orcar(os.id());
        assertThatIllegalStateException().isThrownBy(() -> operacoes.aprovar(os.id()));
        assertSaldo(peca.id(), 10, 3);
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().iniciadaEm()).isEmpty();
    }

    @Test
    void reservaAusenteOuQuantidadeInconsistenteNaoEquivaleABaixaBemSucedida() {
        OrdemDeServico os = criar();
        Peca peca = peca(5);
        operacoes.incluirPeca(os.id(), peca.id(), 2);
        orcar(os.id());
        jdbc.update("update reserva_item_os set quantidade = 1 where ordem_de_servico_id = ?", os.id());
        assertThatIllegalStateException().isThrownBy(() -> operacoes.aprovar(os.id()));
        jdbc.update("update reserva_item_os set quantidade = 2 where ordem_de_servico_id = ?", os.id());
        jdbc.update("delete from reserva_de_estoque where ordem_de_servico_id = ?", os.id());
        assertThatIllegalStateException().isThrownBy(() -> operacoes.aprovar(os.id()));
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
    }

    @Test
    void duasOrdensDisputamSaldoSemExcederEstoque() throws Exception {
        OrdemDeServico a = criar();
        OrdemDeServico b = criar();
        Peca peca = peca(3);
        List<Boolean> resultado = concorrer(() -> tentarIncluir(a.id(), peca.id()), () -> tentarIncluir(b.id(), peca.id()));
        assertThat(resultado).containsExactlyInAnyOrder(true, false);
        assertSaldo(peca.id(), 3, 2);
        assertThat(qtd("reserva_de_estoque")).isEqualTo(1);
        assertThat(qtd("reserva_item_os")).isEqualTo(1);
    }

    @Test
    void inclusoesConcorrentesNaMesmaOsNaoPerdemItens() throws Exception {
        OrdemDeServico os = criar();
        Servico servico = servicos.salvar(Servico.cadastrar("Diagnóstico", null, new BigDecimal("50.00")));
        assertThat(concorrer(() -> { operacoes.incluirServico(os.id(), servico.id(), 1); return true; },
                () -> { operacoes.incluirServico(os.id(), servico.id(), 1); return true; })).containsExactly(true, true);
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().itens()).hasSize(2);
    }

    @Test
    void aprovacoesConcorrentesNaoBaixamEstoqueDuasVezes() throws Exception {
        OrdemDeServico os = criar();
        Peca peca = peca(5);
        operacoes.incluirPeca(os.id(), peca.id(), 2);
        orcar(os.id());
        assertThat(concorrer(() -> tentarAprovar(os.id()), () -> tentarAprovar(os.id())))
                .containsExactlyInAnyOrder(true, false);
        assertSaldo(peca.id(), 3, 0);
    }

    @Test
    void reservaSemItemNaoPodeSerConfirmadaNoCommit() {
        OrdemDeServico os = criar();
        Peca peca = peca(5);
        assertThatThrownBy(() -> new TransactionTemplate(tx).executeWithoutResult(s ->
                reserva.reservar(os.id(), UUID.randomUUID(), peca.id(), 2)))
                .isInstanceOf(RuntimeException.class);
        assertSaldo(peca.id(), 5, 0);
        assertThat(qtd("reserva_item_os")).isZero();
        assertThat(qtd("reserva_de_estoque")).isZero();
    }

    @Test
    void inclusaoAposOrcamentoOuAprovacaoNaoReservaEstoque() {
        OrdemDeServico os = criar();
        Peca peca = peca(5);
        operacoes.incluirPeca(os.id(), peca.id(), 2);
        orcar(os.id());
        assertThatIllegalStateException().isThrownBy(() -> operacoes.incluirPeca(os.id(), peca.id(), 1));
        assertSaldo(peca.id(), 5, 2);
        operacoes.aprovar(os.id());
        assertThatIllegalStateException().isThrownBy(() -> operacoes.incluirPeca(os.id(), peca.id(), 1));
        assertSaldo(peca.id(), 3, 0);
        assertThat(qtd("reserva_de_estoque")).isEqualTo(1);
    }

    private boolean tentarIncluir(UUID osId, UUID pecaId) {
        try { operacoes.incluirPeca(osId, pecaId, 2); return true; }
        catch (IllegalStateException e) { return false; }
    }

    private boolean tentarAprovar(UUID osId) {
        try { operacoes.aprovar(osId); return true; }
        catch (IllegalStateException e) { return false; }
    }

    private List<Boolean> concorrer(Callable<Boolean> a, Callable<Boolean> b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch pronta = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            var tarefas = List.of(a, b).stream().map(acao -> pool.submit(() -> {
                pronta.countDown(); inicio.await(); return acao.call();
            })).toList();
            assertThat(pronta.await(5, TimeUnit.SECONDS)).isTrue();
            inicio.countDown();
            return List.of(tarefas.get(0).get(15, TimeUnit.SECONDS), tarefas.get(1).get(15, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }

    private OrdemDeServico criar() { return atendimento.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012); }
    private void orcar(UUID id) { atendimento.iniciarDiagnostico(id); atendimento.gerarOrcamento(id); }
    private Peca peca(int saldo) {
        return pecas.salvar(Peca.cadastrar("Óleo", null, TipoItemEstoque.PECA,
                br.com.grupo117.oficina.catalogoestoque.domain.Dinheiro.de(new BigDecimal("25.25")), saldo));
    }
    private void assertSaldo(UUID id, int fisico, int reservado) {
        Peca p = pecas.buscarPorId(id).orElseThrow();
        assertThat(p.quantidadeDisponivel()).isEqualTo(fisico);
        assertThat(p.quantidadeReservada()).isEqualTo(reservado);
    }
    private long qtd(String tabela) { return jdbc.queryForObject("select count(*) from " + tabela, Long.class); }
}
