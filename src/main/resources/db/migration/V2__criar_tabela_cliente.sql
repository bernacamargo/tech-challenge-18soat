-- Cadastro de clientes da oficina. CPF/CNPJ e a identidade do cliente.
CREATE TABLE cliente (
    cpf_cnpj VARCHAR(14) PRIMARY KEY,
    nome     VARCHAR(150) NOT NULL
);
