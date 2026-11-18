# ADR-0023 — Data de abertura e prioridade na listagem de OS

- **Status** — Proposto
- **Data** — 2026-11-17

## Contexto

A Fase 2 exige listar as OS por prioridade de status e antiguidade, omitindo finalizadas e
entregues. O banco da Fase 1 armazena UUID aleatório e datas de execução e finalização.
A data de abertura das OS existentes é desconhecida.

## Decisão

Registrar a abertura de novas OS no agregado, em UTC, e persistir em aberta_em com precisão
de microssegundos. A migration V11 preserva nulo nos registros existentes e define um valor
padrão para novas inserções SQL. A reconstituição preserva o instante conhecido ou sua ausência.

O caso de uso de listagem filtra finalizadas e entregues e ordena por Em execução,
Aguardando aprovação, Em diagnóstico e Recebida. Dentro de cada status, usa a abertura crescente.
OS legadas com data desconhecida vêm primeiro. UUID textual crescente desempata datas iguais
e registros legados. A resposta administrativa expõe abertaEm, permitindo conferir a ordenação.

O port de persistência mantém a consulta completa para os demais consumidores internos.
A política da listagem fica na aplicação e as consultas individuais mantêm acesso ao histórico.

## Consequências

- A antiguidade das novas OS usa o instante de abertura, preservado em alterações posteriores.
- A ordenação entre OS legadas é determinística, com data de abertura apresentada como nula.
- O filtro atua na consulta de listagem e preserva os registros e seus relacionamentos.
- O caso de uso ainda materializa a consulta completa. Uma futura paginação poderá levar o
  filtro e a ordenação à consulta do banco, mantendo esta política.
