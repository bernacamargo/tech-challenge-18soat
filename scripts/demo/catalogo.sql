-- Catálogo opcional para demonstração local. Não faz parte das migrations Flyway.
-- IDs fixos tornam a carga repetível sem duplicar ou sobrescrever dados existentes.
BEGIN;

DO $$
BEGIN
    IF to_regclass('public.servico') IS NULL OR to_regclass('public.peca') IS NULL THEN
        RAISE EXCEPTION 'Tabelas do catálogo ausentes. Inicie a aplicação e aguarde o Flyway antes de executar a carga demo.';
    END IF;
END $$;

INSERT INTO servico (id, nome, descricao, preco) VALUES
    ('11700000-0000-4000-8000-000000000101', '[DEMO] Troca de óleo', 'Mão de obra para troca de óleo; peças e insumos cobrados separadamente.', 80.50),
    ('11700000-0000-4000-8000-000000000102', '[DEMO] Alinhamento e balanceamento', 'Ajuste de alinhamento e balanceamento das rodas.', 120.00),
    ('11700000-0000-4000-8000-000000000103', '[DEMO] Revisão de freios', 'Inspeção e manutenção do sistema de freios; peças cobradas separadamente.', 180.00),
    ('11700000-0000-4000-8000-000000000104', '[DEMO] Diagnóstico eletrônico', 'Leitura de falhas e diagnóstico com scanner automotivo.', 90.00)
ON CONFLICT (id) DO NOTHING;

INSERT INTO peca (id, nome, descricao, tipo, preco_unitario, quantidade_disponivel, quantidade_reservada, versao) VALUES
    ('11700000-0000-4000-8000-000000000201', '[DEMO] Óleo de motor 5W30 - 1 litro', 'Unidade de um litro para demonstração de reserva e baixa.', 'INSUMO', 25.25, 20, 0, 0),
    ('11700000-0000-4000-8000-000000000202', '[DEMO] Filtro de óleo', 'Filtro de óleo para veículo de demonstração.', 'PECA', 35.00, 10, 0, 0),
    ('11700000-0000-4000-8000-000000000203', '[DEMO] Jogo de pastilhas de freio', 'Jogo de pastilhas para demonstração de orçamento.', 'PECA', 240.00, 5, 0, 0),
    ('11700000-0000-4000-8000-000000000204', '[DEMO] Filtro de ar', 'Filtro de ar para veículo de demonstração.', 'PECA', 60.00, 8, 0, 0),
    ('11700000-0000-4000-8000-000000000205', '[DEMO] Correia sem estoque', 'Item com saldo zero para validar recusa de inclusão na OS.', 'PECA', 100.00, 0, 0, 0)
ON CONFLICT (id) DO NOTHING;

COMMIT;

-- Mostrar os dados atuais, inclusive saldos que já tenham sido consumidos/reservados.
SELECT id, nome, preco FROM servico
WHERE id IN ('11700000-0000-4000-8000-000000000101', '11700000-0000-4000-8000-000000000102',
             '11700000-0000-4000-8000-000000000103', '11700000-0000-4000-8000-000000000104')
ORDER BY id;

SELECT id, nome, tipo, preco_unitario, quantidade_disponivel AS saldo_fisico,
       quantidade_reservada AS reservado, quantidade_disponivel - quantidade_reservada AS livre
FROM peca
WHERE id IN ('11700000-0000-4000-8000-000000000201', '11700000-0000-4000-8000-000000000202',
             '11700000-0000-4000-8000-000000000203', '11700000-0000-4000-8000-000000000204',
             '11700000-0000-4000-8000-000000000205')
ORDER BY id;
