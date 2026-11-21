-- Exclusão lógica. Registros antigos permanecem visíveis, com a marca nula.
ALTER TABLE cliente ADD COLUMN excluida_em TIMESTAMP WITH TIME ZONE;
ALTER TABLE veiculo ADD COLUMN excluida_em TIMESTAMP WITH TIME ZONE;
