-- Pecas e insumos com controle de estoque. A reserva separa quantidade para uma OS;
-- a baixa (inicio da execucao) consome a reserva. versao sustenta o lock otimista.
CREATE TABLE peca (
    id                    UUID          PRIMARY KEY,
    nome                  VARCHAR(150)  NOT NULL,
    descricao             VARCHAR(500),
    tipo                  VARCHAR(10)   NOT NULL CHECK (tipo IN ('PECA', 'INSUMO')),
    preco_unitario        NUMERIC(10,2) NOT NULL CHECK (preco_unitario > 0),
    quantidade_disponivel INTEGER       NOT NULL CHECK (quantidade_disponivel >= 0),
    quantidade_reservada  INTEGER       NOT NULL CHECK (quantidade_reservada >= 0),
    versao                BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_peca_reservada_ate_disponivel CHECK (quantidade_reservada <= quantidade_disponivel)
);

-- A OS e referenciada so pelo id: e de outro bounded context (atendimento).
CREATE TABLE reserva_de_estoque (
    id                  UUID        PRIMARY KEY,
    peca_id             UUID        NOT NULL REFERENCES peca (id),
    ordem_de_servico_id UUID        NOT NULL,
    quantidade          INTEGER     NOT NULL CHECK (quantidade > 0),
    status              VARCHAR(10) NOT NULL CHECK (status IN ('ATIVA', 'BAIXADA', 'CANCELADA'))
);

CREATE INDEX idx_reserva_os_status ON reserva_de_estoque (ordem_de_servico_id, status);
CREATE INDEX idx_reserva_peca ON reserva_de_estoque (peca_id);
