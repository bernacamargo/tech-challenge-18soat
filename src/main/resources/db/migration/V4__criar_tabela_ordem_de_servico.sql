CREATE TABLE ordem_de_servico (
    id UUID PRIMARY KEY,
    cpf_cnpj_cliente VARCHAR(14) NOT NULL REFERENCES cliente (cpf_cnpj),
    placa_veiculo VARCHAR(7) NOT NULL REFERENCES veiculo (placa),
    status VARCHAR(30) NOT NULL CHECK (status IN (
        'RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO', 'FINALIZADA', 'ENTREGUE'
    )),
    finalizada_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_os_finalizacao CHECK (
        (status IN ('FINALIZADA', 'ENTREGUE') AND finalizada_em IS NOT NULL)
        OR (status NOT IN ('FINALIZADA', 'ENTREGUE') AND finalizada_em IS NULL)
    )
);

CREATE INDEX idx_os_cliente ON ordem_de_servico (cpf_cnpj_cliente);
CREATE INDEX idx_os_veiculo ON ordem_de_servico (placa_veiculo);
