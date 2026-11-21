-- Uma OS passa por vários status. A unicidade fica em (OS, situação), não mais só na OS.
ALTER TABLE notificacao_orcamento ADD COLUMN status varchar(32);

UPDATE notificacao_orcamento SET status = 'AGUARDANDO_APROVACAO' WHERE status IS NULL;

ALTER TABLE notificacao_orcamento ALTER COLUMN status SET NOT NULL;

ALTER TABLE notificacao_orcamento DROP CONSTRAINT notificacao_orcamento_ordem_de_servico_id_key;

ALTER TABLE notificacao_orcamento
    ADD CONSTRAINT uk_notificacao_os_status UNIQUE (ordem_de_servico_id, status);

ALTER TABLE notificacao_orcamento ADD CONSTRAINT ck_notificacao_status CHECK (status IN (
    'RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO', 'FINALIZADA', 'ENTREGUE',
    'ORCAMENTO_REJEITADO'
));
