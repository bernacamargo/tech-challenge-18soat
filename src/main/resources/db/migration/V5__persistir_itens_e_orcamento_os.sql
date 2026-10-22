ALTER TABLE ordem_de_servico
    ADD COLUMN orcamento_total NUMERIC(19, 2),
    ADD CONSTRAINT ck_os_orcamento_total CHECK (orcamento_total IS NULL OR orcamento_total > 0),
    ADD CONSTRAINT ck_os_orcamento_status CHECK (
        orcamento_total IS NULL OR status NOT IN ('RECEBIDA', 'EM_DIAGNOSTICO')
    );

CREATE TABLE item_da_os (
    id UUID PRIMARY KEY,
    ordem_de_servico_id UUID NOT NULL REFERENCES ordem_de_servico (id) ON DELETE CASCADE,
    posicao INTEGER NOT NULL CHECK (posicao >= 0),
    catalogo_id UUID NOT NULL,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('SERVICO', 'PECA')),
    descricao VARCHAR(255) NOT NULL CHECK (length(trim(descricao)) > 0),
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario NUMERIC(19, 2) NOT NULL CHECK (preco_unitario > 0),
    CONSTRAINT uk_item_os_posicao UNIQUE (ordem_de_servico_id, posicao)
);
