package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.*;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import br.com.grupo117.oficina.atendimento.infrastructure.notification.OrcamentoEmailDispatcher;
import br.com.grupo117.oficina.atendimento.infrastructure.service.ItensOrcamentoOSService;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/** Usa SMTP real e a API de leitura do MailPit, com portas e volume isolados. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrcamentoSmtpIntegrationTest extends AbstractIntegrationTest {
    static final GenericContainer<?> mailpit = new GenericContainer<>("axllent/mailpit:v1.31.6")
            .withExposedPorts(1025, 8025)
            .waitingFor(Wait.forHttp("/livez").forPort(8025));
    static { mailpit.start(); }

    @DynamicPropertySource
    static void smtp(DynamicPropertyRegistry registry) {
        registry.add("oficina.notificacoes.habilitadas", () -> true);
        registry.add("spring.mail.host", mailpit::getHost);
        registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
        registry.add("oficina.notificacoes.intervalo-ms", () -> 200);
    }

    @Autowired OrdemDeServicoService service;
    @Autowired ItensOrcamentoOSService itens;
    @Autowired OrdemDeServicoRepositorio ordens;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired OrcamentoEmailDispatcher dispatcher;

    @Test
    void schedulerEnviaOrcamentoComSmtpRealAposCommit() {
        jdbc.update("INSERT INTO cliente(cpf_cnpj, nome, email) VALUES (?, ?, ?)",
                "52998224725", "Maria", "maria@exemplo.com");
        var os = service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
        os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Revisão de freios", 2, new Dinheiro(new BigDecimal("120.50"))));
        ordens.salvar(os);
        service.submeter(os.id());
        var http = new RestTemplate();
        String url = "http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025) + "/api/v1";
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var textos = textosDaOs(http, url, os.id());
            assertThat(textos).hasSize(2);
            assertThat(textos).anyMatch(texto -> texto.contains("Revisão de freios")
                    && texto.contains("241,00") && texto.contains("120,50"));
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM notificacao_orcamento WHERE ordem_de_servico_id = ? AND enviada_em IS NOT NULL",
                    Integer.class, os.id())).isEqualTo(2);
        });
        dispatcher.enviarPendentes();
        assertThat(textosDaOs(http, url, os.id())).hasSize(2);
    }

    @Test
    void schedulerEntregaCadaStatusDoFluxoNoMailPit() {
        jdbc.update("INSERT INTO cliente(cpf_cnpj, nome, email) VALUES (?, ?, ?)",
                "52998224725", "Maria", "maria@exemplo.com");
        var os = service.criar("52998224725", "Maria", "ABC1D23", "Fiat", "Uno", 2012);
        os.incluirItem(new ItemDaOS(UUID.randomUUID(), UUID.randomUUID(), TipoItemOS.SERVICO,
                "Troca de óleo", 1, new Dinheiro(new BigDecimal("80.50"))));
        ordens.salvar(os);
        var http = new RestTemplate();
        String url = "http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025) + "/api/v1";
        aguardar(http, url, os.id(), "Recebemos o seu veículo", 1);
        service.iniciarDiagnostico(os.id());
        aguardar(http, url, os.id(), "diagnóstico do seu veículo foi iniciado", 2);
        service.gerarOrcamento(os.id());
        aguardar(http, url, os.id(), "pronto para aprovação", 3);
        itens.aprovar(os.id());
        aguardar(http, url, os.id(), "execução dos serviços começou", 4);
        service.concluirServicos(os.id());
        aguardar(http, url, os.id(), "foram concluídos", 5);
        service.registrarEntrega(os.id());
        aguardar(http, url, os.id(), "veículo foi entregue", 6);
        dispatcher.enviarPendentes();
        assertThat(textosDaOs(http, url, os.id())).hasSize(6);
    }

    private static void aguardar(RestTemplate http, String url, UUID os, String trecho, int total) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var textos = textosDaOs(http, url, os);
            assertThat(textos).hasSize(total);
            assertThat(textos).anyMatch(texto -> texto.contains(trecho));
        });
    }

    private static List<String> textosDaOs(RestTemplate http, String url, UUID os) {
        var mensagens = http.getForObject(url + "/messages", JsonNode.class);
        var textos = new ArrayList<String>();
        for (var resumo : mensagens.path("messages")) {
            var mensagem = http.getForObject(url + "/message/" + resumo.path("ID").asText(), JsonNode.class);
            String texto = mensagem.path("Subject").asText() + "\n" + mensagem.path("Text").asText();
            if (texto.contains(os.toString())) textos.add(texto);
        }
        return textos;
    }
}
