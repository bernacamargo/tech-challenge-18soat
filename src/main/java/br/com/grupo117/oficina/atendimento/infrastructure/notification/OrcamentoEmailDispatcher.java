package br.com.grupo117.oficina.atendimento.infrastructure.notification;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/** Entrega pelo menos uma vez. Falhas e ausência de contato permanecem na fila. */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "oficina.notificacoes.habilitadas", havingValue = "true", matchIfMissing = true)
public class OrcamentoEmailDispatcher {
    private final JdbcTemplate jdbc;
    private final JavaMailSender mail;
    private final String remetente;

    public OrcamentoEmailDispatcher(JdbcTemplate jdbc, JavaMailSender mail,
                                    @Value("${oficina.notificacoes.remetente}") String remetente) {
        this.jdbc = jdbc;
        this.mail = mail;
        this.remetente = remetente;
    }

    @Scheduled(fixedDelayString = "${oficina.notificacoes.intervalo-ms:5000}",
            initialDelayString = "${oficina.notificacoes.intervalo-ms:5000}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enviarPendentes() {
        var pendentes = jdbc.query("""
                SELECT n.id, COALESCE(n.destinatario, c.email) AS email, n.assunto, n.corpo
                FROM notificacao_orcamento n JOIN cliente c ON c.cpf_cnpj = n.cliente_id
                WHERE n.enviada_em IS NULL AND n.proxima_tentativa_em <= CURRENT_TIMESTAMP
                ORDER BY n.proxima_tentativa_em, n.id LIMIT 10 FOR UPDATE OF n SKIP LOCKED
                """, (rs, row) -> new Mensagem(rs.getObject("id", UUID.class), rs.getString("email"),
                        rs.getString("assunto"), rs.getString("corpo")));
        for (var mensagem : pendentes) {
            if (mensagem.email() == null) {
                reagendar(mensagem.id(), "AGUARDANDO_EMAIL");
                continue;
            }
            var email = new SimpleMailMessage();
            email.setFrom(remetente);
            email.setTo(mensagem.email());
            email.setSubject(mensagem.assunto());
            email.setText(mensagem.corpo());
            try {
                mail.send(email);
                jdbc.update("""
                        UPDATE notificacao_orcamento SET enviada_em = CURRENT_TIMESTAMP,
                            destinatario = ?, tentativas = tentativas + 1, ultimo_erro = NULL WHERE id = ?
                        """, mensagem.email(), mensagem.id());
            } catch (MailException falha) {
                // Não persistir/logar mensagens SMTP: podem conter credenciais ou dados pessoais.
                jdbc.update("UPDATE notificacao_orcamento SET tentativas = tentativas + 1 WHERE id = ?", mensagem.id());
                reagendar(mensagem.id(), "FALHA_SMTP");
            }
        }
    }

    private void reagendar(UUID id, String erro) {
        jdbc.update("""
                UPDATE notificacao_orcamento SET ultimo_erro = ?,
                    proxima_tentativa_em = CURRENT_TIMESTAMP + INTERVAL '1 minute' WHERE id = ?
                """, erro, id);
    }

    private record Mensagem(UUID id, String email, String assunto, String corpo) { }
}
