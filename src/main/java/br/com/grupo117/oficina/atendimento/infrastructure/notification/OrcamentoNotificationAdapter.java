package br.com.grupo117.oficina.atendimento.infrastructure.notification;

import br.com.grupo117.oficina.atendimento.application.port.NotificacaoPort;
import br.com.grupo117.oficina.atendimento.domain.OrdemDeServico;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Outbox PostgreSQL: agenda a situação atual da OS atomicamente com a transição. */
@Component
public class OrcamentoNotificationAdapter implements NotificacaoPort {
    private final JdbcTemplate jdbc;

    public OrcamentoNotificationAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void agendar(OrdemDeServico os) {
        var mensagem = MensagemDeStatus.de(os);
        jdbc.update("""
                INSERT INTO notificacao_orcamento
                    (id, ordem_de_servico_id, cliente_id, destinatario, assunto, corpo, status)
                SELECT ?, ?, cpf_cnpj, email, ?, ?, ? FROM cliente WHERE cpf_cnpj = ?
                ON CONFLICT (ordem_de_servico_id, status) DO NOTHING
                """, UUID.randomUUID(), os.id(), mensagem.assunto(), mensagem.corpo(),
                mensagem.situacao(), os.clienteId());
    }
}
