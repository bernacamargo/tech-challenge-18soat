# ADR-0003: Gate de cobertura de 80% nos domínios críticos via CI

- **Status**: Aceito
- **Data**: 2026-10-01
- **Participantes**: Grupo 117 (proposto por Bernardo Camargo; alinhado com Frederico Brion)

## Contexto

O enunciado exige cobertura mínima de 80% nos domínios críticos, com evidência. Cobertura
verificada manualmente não é confiável ao longo do desenvolvimento do grupo.

## Decisão

Configurar **JaCoCo** com regra de **mínimo 80% de linhas cobertas nos pacotes `domain` e
`application`** dos bounded contexts, aplicada pelo goal `check` e executada no CI
(GitHub Actions) como condição para merge em `main`.

## Consequências

- Positivas: evidência contínua e auditável do requisito; falha cedo, antes do merge.
- Negativas: infra classes (config, security) fora do gate — monitoradas por SonarCloud, mas sem
  piso obrigatório nesta fase.
- Alternativas descartadas: cobertura global 80% (puniria o scaffold/infra sem domínio), verificação
  manual (não auditável).
