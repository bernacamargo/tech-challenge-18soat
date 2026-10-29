-- Preserva o historico sem enviar uma solicitacao de aprovacao ja decidida.
ALTER TABLE notificacao_orcamento ADD COLUMN cancelada_em timestamptz;
ALTER TABLE notificacao_orcamento ADD CONSTRAINT ck_notificacao_envio_ou_cancelamento
    CHECK (enviada_em IS NULL OR cancelada_em IS NULL);
DROP INDEX idx_notificacao_orcamento_pendente;
CREATE INDEX idx_notificacao_orcamento_pendente
    ON notificacao_orcamento(proxima_tentativa_em)
    WHERE enviada_em IS NULL AND cancelada_em IS NULL;
