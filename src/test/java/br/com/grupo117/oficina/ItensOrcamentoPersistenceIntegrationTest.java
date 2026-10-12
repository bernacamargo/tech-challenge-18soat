package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.*;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import br.com.grupo117.oficina.infra.security.JwtTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

class ItensOrcamentoPersistenceIntegrationTest extends AbstractIntegrationTest {
    @Autowired private OrdemDeServicoService service;
    @Autowired private OrdemDeServicoRepositorio ordens;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void preservaItensPrecosOrdemEOrcamentoEmGravacoesSucessivas() {
        OrdemDeServico os = criar();
        ItemDaOS servico = item(TipoItemOS.SERVICO, "Troca de óleo", 1, "80.50");
        ItemDaOS peca = item(TipoItemOS.PECA, "Óleo", 3, "25.25");
        os.incluirItem(servico);
        ordens.salvar(os);
        os = ordens.buscarPorId(os.id()).orElseThrow();
        os.incluirItem(peca);
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        ordens.salvar(os);
        os = ordens.buscarPorId(os.id()).orElseThrow();
        assertThat(os.itens()).containsExactly(servico, peca);
        assertThat(os.orcamento()).isEmpty();
        os.gerarOrcamento();
        ordens.salvar(os);
        ordens.salvar(ordens.buscarPorId(os.id()).orElseThrow());
        OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
        assertThat(lida.itens()).containsExactly(servico, peca);
        assertThat(lida.status()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(lida.orcamento().orElseThrow().total().valor()).isEqualByComparingTo("156.25");
        assertThat(jdbc.queryForObject("select count(*) from item_da_os", Long.class)).isEqualTo(2L);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestSupport.token("ATENDENTE"));
        ResponseEntity<JsonNode> detalhe = restTemplate.exchange("/api/ordens-de-servico/" + os.id(),
                HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(detalhe.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detalhe.getBody().path("itens").size()).isEqualTo(2);
        assertThat(detalhe.getBody().path("itens").get(1).path("subtotal").decimalValue())
                .isEqualByComparingTo("75.75");
        assertThat(detalhe.getBody().path("orcamento").path("total").decimalValue())
                .isEqualByComparingTo("156.25");
        JsonNode publico = restTemplate.getForObject("/api/acompanhamento/ordens-de-servico/" + os.id(), JsonNode.class);
        assertThat(publico.size()).isEqualTo(2);
        assertThat(publico.has("itens")).isFalse();
        assertThat(publico.has("orcamento")).isFalse();
    }

    @Test
    void falhaAoGravarItemDesfazAlteracoesDoAgregado() {
        OrdemDeServico os = criar();
        ItemDaOS original = item(TipoItemOS.SERVICO, "Diagnóstico", 1, "10.00");
        os.incluirItem(original);
        ordens.salvar(os);
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        os.incluirItem(item(TipoItemOS.PECA, "x".repeat(256), 1, "20.00"));
        assertThatThrownBy(() -> ordens.salvar(os)).isInstanceOf(RuntimeException.class);
        OrdemDeServico lida = ordens.buscarPorId(os.id()).orElseThrow();
        assertThat(lida.status()).isEqualTo(StatusOS.RECEBIDA);
        assertThat(lida.itens()).containsExactly(original);
    }

    @Test
    void rejeitaTotalPersistidoInconsistenteComItens() {
        OrdemDeServico os = criar();
        os.incluirItem(item(TipoItemOS.SERVICO, "Diagnóstico", 1, "10.00"));
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        os.gerarOrcamento();
        ordens.salvar(os);
        jdbc.update("update ordem_de_servico set orcamento_total = 11 where id = ?", os.id());
        assertThatThrownBy(() -> ordens.buscarPorId(os.id())).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    private OrdemDeServico criar() {
        return service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
    }
    private ItemDaOS item(TipoItemOS tipo, String descricao, int quantidade, String preco) {
        return new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), tipo, descricao, quantidade,
                new Dinheiro(new BigDecimal(preco)));
    }
}
