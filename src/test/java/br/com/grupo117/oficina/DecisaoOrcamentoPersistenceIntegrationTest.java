package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.application.usecase.AprovarOrcamento;
import br.com.grupo117.oficina.atendimento.application.usecase.ConcluirServicos;
import br.com.grupo117.oficina.atendimento.application.usecase.RegistrarEntrega;
import br.com.grupo117.oficina.atendimento.application.usecase.RejeitarOrcamento;
import br.com.grupo117.oficina.atendimento.domain.ItemDaOS;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import br.com.grupo117.oficina.atendimento.domain.StatusOS;
import br.com.grupo117.oficina.atendimento.domain.TipoItemOS;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

class DecisaoOrcamentoPersistenceIntegrationTest extends AbstractIntegrationTest {
    private static final Instant MOMENTO = Instant.parse("2026-10-20T12:00:00.123456789Z");
    private static final Instant PERSISTIDO = MOMENTO.truncatedTo(ChronoUnit.MICROS);
    private static final Clock CLOCK = Clock.fixed(MOMENTO, ZoneOffset.UTC);
    @Autowired private OrdemDeServicoService service;
    @Autowired private OrdemDeServicoRepositorio ordens;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void aprovaEReabreComMesmoInicioSemRepetirBaixa() {
        OrdemDeServico os = orcada();
        List<UUID> baixas = new ArrayList<>();
        AprovarOrcamento aprovar = new AprovarOrcamento(ordens, baixas::add, CLOCK);
        OrdemDeServico salva = aprovar.aprovar(os.id());
        OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
        assertThat(salva.iniciadaEm()).contains(PERSISTIDO);
        assertThat(lida.iniciadaEm()).isEqualTo(salva.iniciadaEm());
        assertThat(lida.status()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(lida.rejeicaoOrcamento()).isEmpty();
        assertThat(lida.itens()).isEqualTo(os.itens());
        assertThat(lida.orcamento()).isEqualTo(os.orcamento());
        assertThatIllegalStateException().isThrownBy(() -> aprovar.aprovar(os.id()));
        assertThat(baixas).containsExactly(os.id());
    }

    @Test
    void preservaInicioNaConclusaoEEntregaAposReabrir() {
        OrdemDeServico os = orcada();
        os.aprovarOrcamento(MOMENTO);
        ordens.salvar(os);
        Instant fim = PERSISTIDO.plusSeconds(3600);
        new ConcluirServicos(ordens, Clock.fixed(fim, ZoneOffset.UTC)).concluir(os.id());
        new RegistrarEntrega(ordens).registrar(os.id());
        OrdemDeServico entregue = ordens.buscarPorId(os.id()).orElseThrow();
        assertThat(entregue.status()).isEqualTo(StatusOS.ENTREGUE);
        assertThat(entregue.iniciadaEm()).contains(PERSISTIDO);
        assertThat(entregue.finalizadaEm()).contains(fim);
        assertThat(entregue.orcamento()).isEqualTo(os.orcamento());
        assertThat(entregue.itens()).isEqualTo(os.itens());
    }

    @Test
    void rejeitaEPreservaMotivoDataEBloqueioAposGravacoesSucessivas() {
        OrdemDeServico os = orcada();
        OrdemDeServico salva = new RejeitarOrcamento(ordens, CLOCK).rejeitar(os.id(), "  Preço acima do esperado  ");
        ordens.salvar(ordens.buscarPorId(os.id()).orElseThrow());
        OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
        assertThat(lida.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(lida.iniciadaEm()).isEmpty();
        assertThat(lida.rejeicaoOrcamento()).isEqualTo(salva.rejeicaoOrcamento());
        assertThat(lida.rejeicaoOrcamento().orElseThrow().momento()).isEqualTo(PERSISTIDO);
        assertThat(lida.rejeicaoOrcamento().orElseThrow().motivo()).isEqualTo("Preço acima do esperado");
        assertThat(lida.itens()).isEqualTo(os.itens());
        assertThat(lida.orcamento()).isEqualTo(os.orcamento());
        List<UUID> baixas = new ArrayList<>();
        assertThatIllegalStateException().isThrownBy(() -> new AprovarOrcamento(ordens, baixas::add, CLOCK).aprovar(os.id()));
        assertThatIllegalStateException().isThrownBy(() -> new RejeitarOrcamento(ordens, CLOCK).rejeitar(os.id(), "Outro motivo"));
        assertThat(baixas).isEmpty();
        JsonNode publica = restTemplate.getForObject("/api/acompanhamento/ordens-de-servico/" + os.id(), JsonNode.class);
        assertThat(publica.size()).isEqualTo(2);
        assertThat(publica.path("status").asText()).isEqualTo("AGUARDANDO_APROVACAO");
        assertThat(publica.has("motivoRejeicao")).isFalse();
    }

    @Test
    void motivoLongoComAcentosEPontuacaoNaoSePerde() {
        OrdemDeServico os = orcada();
        String motivo = "Revisão de peças: orçamento não aprovado.\n".repeat(100);
        new RejeitarOrcamento(ordens, CLOCK).rejeitar(os.id(), motivo);
        assertThat(ordens.buscarPorId(os.id()).orElseThrow().rejeicaoOrcamento().orElseThrow().motivo())
                .isEqualTo(motivo.trim());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "iniciada_em = TIMESTAMPTZ '2026-10-20 12:00:00Z'",
            "status = 'EM_EXECUCAO', iniciada_em = now(), orcamento_total = null",
            "orcamento_rejeitado_em = now()",
            "motivo_rejeicao = 'Custo'",
            "orcamento_rejeitado_em = now(), motivo_rejeicao = E' \\t\\n '",
            "status = 'EM_EXECUCAO', orcamento_rejeitado_em = now(), motivo_rejeicao = 'Custo'",
            "orcamento_total = null, orcamento_rejeitado_em = now(), motivo_rejeicao = 'Custo'",
            "status = 'FINALIZADA', iniciada_em = TIMESTAMPTZ '2026-10-20 12:00:00Z', finalizada_em = TIMESTAMPTZ '2026-10-20 11:59:59Z'"
    })
    void bancoRecusaEstadosInconsistentesSemModificarRegistro(String alteracoes) {
        OrdemDeServico os = orcada();
        assertThatThrownBy(() -> jdbc.update("update ordem_de_servico set " + alteracoes + " where id = ?", os.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
        OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
        assertThat(lida.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(lida.iniciadaEm()).isEmpty();
        assertThat(lida.rejeicaoOrcamento()).isEmpty();
        assertThat(lida.orcamento()).isEqualTo(os.orcamento());
    }

    @Test
    void falhaAoSalvarRejeicaoPreservaEstadoAnteriorNoBanco() {
        OrdemDeServico os = orcada();
        jdbc.execute("alter table ordem_de_servico add constraint ck_rejeicao_falha_teste check (orcamento_rejeitado_em is null)");
        try {
            assertThatThrownBy(() -> new RejeitarOrcamento(ordens, CLOCK).rejeitar(os.id(), "Custo"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
            assertThat(lida.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
            assertThat(lida.rejeicaoOrcamento()).isEmpty();
            assertThat(lida.itens()).isEqualTo(os.itens());
            assertThat(lida.orcamento()).isEqualTo(os.orcamento());
        } finally {
            jdbc.execute("alter table ordem_de_servico drop constraint ck_rejeicao_falha_teste");
        }
    }

    @Test
    void falhaAoGravarInicioNaoPersisteTransicaoParcial() {
        OrdemDeServico os = orcada();
        os.aprovarOrcamento(MOMENTO);
        jdbc.execute("alter table ordem_de_servico add constraint ck_inicio_falha_teste check (iniciada_em is null)");
        try {
            assertThatThrownBy(() -> ordens.salvar(os)).isInstanceOf(DataIntegrityViolationException.class);
            OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
            assertThat(lida.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
            assertThat(lida.iniciadaEm()).isEmpty();
            assertThat(lida.itens()).isEqualTo(os.itens());
        } finally {
            jdbc.execute("alter table ordem_de_servico drop constraint ck_inicio_falha_teste");
        }
    }

    @Test
    void semDecisaoGravaCamposNulos() {
        OrdemDeServico os = orcada();
        var registro = jdbc.queryForMap("select iniciada_em, orcamento_rejeitado_em, motivo_rejeicao from ordem_de_servico where id = ?", os.id());
        assertThat(registro).containsEntry("iniciada_em", null)
                .containsEntry("orcamento_rejeitado_em", null).containsEntry("motivo_rejeicao", null);
        assertThat(ordens.listar()).hasSize(1);
        assertThat(ordens.listar().getFirst().iniciadaEm()).isEmpty();
    }

    private OrdemDeServico orcada() {
        OrdemDeServico os = service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
        os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Diagnóstico", 1, new Dinheiro(new BigDecimal("50.00"))));
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        os.gerarOrcamento();
        return ordens.salvar(os);
    }
}
