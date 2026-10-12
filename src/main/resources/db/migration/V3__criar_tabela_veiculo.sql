-- Veiculo da oficina, identificado pela placa e vinculado a um cliente.
CREATE TABLE veiculo (
    placa             VARCHAR(7)  PRIMARY KEY,
    marca             VARCHAR(80) NOT NULL,
    modelo            VARCHAR(80) NOT NULL,
    ano               INTEGER     NOT NULL,
    cpf_cnpj_cliente  VARCHAR(14) NOT NULL REFERENCES cliente (cpf_cnpj)
);

CREATE INDEX idx_veiculo_cpf_cnpj_cliente ON veiculo (cpf_cnpj_cliente);
