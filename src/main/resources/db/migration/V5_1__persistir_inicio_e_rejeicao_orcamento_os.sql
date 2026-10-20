ALTER TABLE ordem_de_servico
    ADD COLUMN iniciada_em TIMESTAMPTZ,
    ADD COLUMN orcamento_rejeitado_em TIMESTAMPTZ,
    ADD COLUMN motivo_rejeicao TEXT,
    ADD CONSTRAINT ck_os_inicio_estado CHECK (
        iniciada_em IS NULL OR (
            status IN ('EM_EXECUCAO', 'FINALIZADA', 'ENTREGUE')
            AND orcamento_total IS NOT NULL
        )
    ),
    ADD CONSTRAINT ck_os_execucao_cronologia CHECK (
        iniciada_em IS NULL OR finalizada_em IS NULL OR finalizada_em >= iniciada_em
    ),
    ADD CONSTRAINT ck_os_rejeicao_estado CHECK (
        (orcamento_rejeitado_em IS NULL AND motivo_rejeicao IS NULL)
        OR (
            orcamento_rejeitado_em IS NOT NULL
            AND motivo_rejeicao IS NOT NULL
            AND motivo_rejeicao ~ '[^[:space:]]'
            AND status = 'AGUARDANDO_APROVACAO'
            AND orcamento_total IS NOT NULL
            AND iniciada_em IS NULL
        )
    );
