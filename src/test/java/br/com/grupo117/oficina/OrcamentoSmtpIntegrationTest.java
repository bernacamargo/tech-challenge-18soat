package br.com.grupo117.oficina;

import br.com.grupo117.oficina.atendimento.application.port.OrdemDeServicoRepositorio;
import br.com.grupo117.oficina.atendimento.domain.*;
import br.com.grupo117.oficina.atendimento.domain.valueobject.Dinheiro;
import br.com.grupo117.oficina.atendimento.infrastructure.notification.OrcamentoEmailDispatcher;
import br.com.grupo117.oficina.atendimento.infrastructure.service.OrdemDeServicoService;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Duration;
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
            var mensagens = http.getForObject(url + "/messages", JsonNode.class);
            assertThat(mensagens.path("total").asInt()).isEqualTo(1);
            var resumo = mensagens.path("messages").get(0);
            var mensagem = http.getForObject(url + "/message/" + resumo.path("ID").asText(), JsonNode.class);
            assertThat(mensagem.path("To").get(0).path("Address").asText()).isEqualTo("maria@exemplo.com");
            assertThat(mensagem.path("Subject").asText()).contains(os.id().toString());
            assertThat(mensagem.path("Text").asText()).contains("Revisão de freios", "241,00", "120,50");
            assertThat(jdbc.queryForObject("SELECT enviada_em IS NOT NULL FROM notificacao_orcamento", Boolean.class)).isTrue();
        });
        dispatcher.enviarPendentes();
        assertThat(http.getForObject(url + "/messages", JsonNode.class).path("total").asInt()).isEqualTo(1);
    }
}
