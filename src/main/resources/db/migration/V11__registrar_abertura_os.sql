-- Os registros existentes permanecem sem data conhecida.
ALTER TABLE ordem_de_servico ADD COLUMN aberta_em TIMESTAMP WITH TIME ZONE;
ALTER TABLE ordem_de_servico ALTER COLUMN aberta_em SET DEFAULT CURRENT_TIMESTAMP;
