# ADR-0002: PostgreSQL para todas as necessidades de persistência

- **Status**: Aceito
- **Data**: 2026-10-01
- **Participantes**: Grupo 117 (proposto por Bernardo Camargo; alinhado com Vitor Melo e Frederico Brion)

## Contexto

O enunciado deixa o banco livre, com decisão justificada. O domínio tem transações que tocam
múltiplos agregados (criar OS → reservar peças no estoque → gerar orçamento) e exige histórico
de clientes/veículos.

## Decisão

Usar **PostgreSQL (17)** para tudo — "Postgres for everything" — sem Redis, MongoDB ou outros
bancos auxiliares nesta fase.

## Consequências

- Positivas: ACID e integridade referencial para o fluxo crítico de OS + estoque; JSONB disponível
  para dados semiestruturados; um único motor para operar/demonstrar; ecossistema maduro com
  JPA/Hibernate, Flyway e Testcontainers; justificativa simples e sólida para a entrega.
- Negativas: casos de uso de cache/filas ficarão no Postgres (aceito no MVP; reavaliar em fases futuras).
- Alternativas descartadas: MongoDB (transações multi-agregado menos naturais para o fluxo de OS),
  H2 (divergência de dialeto nos testes — usamos Testcontainers com Postgres real).
