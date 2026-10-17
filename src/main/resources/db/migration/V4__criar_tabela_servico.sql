-- Servico do catalogo da oficina, identificado por id e precificado em dinheiro.
CREATE TABLE servico (
    id         UUID           PRIMARY KEY,
    nome       VARCHAR(120)   NOT NULL,
    descricao  VARCHAR(500),
    preco      NUMERIC(12, 2) NOT NULL,
    CONSTRAINT ck_servico_preco_positivo CHECK (preco > 0)
);
