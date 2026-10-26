package br.com.grupo117.oficina.atendimento.infrastructure.notification;

import br.com.grupo117.oficina.atendimento.application.port.NotificacaoPort;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Outbox PostgreSQL: agenda o orçamento atomicamente com a transição da OS. */
@Component
public class OrcamentoNotificationAdapter implements NotificacaoPort {
    private final JdbcTemplate jdbc;

    public OrcamentoNotificationAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void agendarOrcamento(OrdemDeServico os) {
        var moeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        var corpo = new StringBuilder("Seu orçamento está pronto para aprovação.\n\nOS: ")
                .append(os.id()).append("\nVeículo: ").append(os.veiculoId()).append("\n\n");
        os.itens().forEach(item -> corpo.append(item.descricao()).append(" — ")
                .append(item.quantidade()).append(" x ").append(moeda.format(item.precoUnitario().valor()))
                .append(" = ").append(moeda.format(item.subtotal().valor())).append('\n'));
        corpo.append("\nTotal: ").append(moeda.format(os.orcamento().orElseThrow().total().valor()))
                .append("\n\nInforme sua aprovação ou rejeição à oficina, identificando a OS acima.");
        jdbc.update("""
                INSERT INTO notificacao_orcamento
                    (id, ordem_de_servico_id, cliente_id, destinatario, assunto, corpo)
                SELECT ?, ?, cpf_cnpj, email, ?, ? FROM cliente WHERE cpf_cnpj = ?
                ON CONFLICT (ordem_de_servico_id) DO NOTHING
                """, UUID.randomUUID(), os.id(), "Orçamento da OS " + os.id(), corpo.toString(), os.clienteId());
    }
}
