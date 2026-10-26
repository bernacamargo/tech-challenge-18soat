ALTER TABLE cliente ADD COLUMN email varchar(254);

-- Snapshot imutável do orçamento; nenhuma mensagem é enviada antes do commit desta linha.
CREATE TABLE notificacao_orcamento (
    id uuid PRIMARY KEY,
    ordem_de_servico_id uuid NOT NULL UNIQUE REFERENCES ordem_de_servico(id) ON DELETE CASCADE,
    cliente_id varchar(14) NOT NULL REFERENCES cliente(cpf_cnpj),
    destinatario varchar(254),
    assunto varchar(200) NOT NULL,
    corpo text NOT NULL,
    criada_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviada_em timestamptz,
    tentativas integer NOT NULL DEFAULT 0 CHECK (tentativas >= 0),
    proxima_tentativa_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_erro varchar(64)
);
CREATE INDEX idx_notificacao_orcamento_pendente
    ON notificacao_orcamento(proxima_tentativa_em) WHERE enviada_em IS NULL;
