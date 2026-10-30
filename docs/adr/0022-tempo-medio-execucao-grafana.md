# ADR 0022 — Tempo médio de execução pela latência das rotas no Grafana

- Status: Aceito
- Data: 2026-10-29
- Participantes: grupo 117, com aval do professor da Fase 1

## Contexto

O enunciado pede monitoramento ou cálculo do tempo médio de execução dos serviços.
Os ADRs 0013, 0015 e 0016 deixaram essa métrica em aberto: a OS guarda `iniciada_em` e
`finalizada_em`, e o Grafana já mostra a latência HTTP, mas os textos diziam que o
tempo médio ficaria para uma etapa posterior ou que o dashboard não o media.

O professor da Fase 1 avalizou usar o Grafana para obter esse tempo médio a partir da
latência dos endpoints e das requisições.

## Decisão

O tempo médio de execução dos serviços é a latência das rotas da API no dashboard
**Oficina — Saúde da API**:

- **Latência média por rota**: média de `http_server_requests_seconds` por método e URI.
- **Latência p95 por rota**: percentil 95 do mesmo histograma.

A coleta continua a do ADR 0016 (Micrometer, Prometheus a cada 15 segundos, perfil
`observability`). `iniciada_em` e `finalizada_em` permanecem o registro de uma execução
individual da OS. Esta decisão não cria agregação nem endpoint sobre essas colunas.

## Consequências

- Quem for demonstrar o requisito abre `http://localhost:3000/d/oficina-api` e mostra os
  dois painéis de latência, com tráfego real na API.
- Sem requisições, os painéis ficam sem dados. Health e coleta continuam fora da série.
- Trocar essa leitura por uma média da duração das OS exige nova decisão. Os textos do
  glossário, do README, do guia do Grafana e do diagrama de subdomínios seguem esta ADR.

## Referências

- [ADR 0015](0015-grafana-local-docker.md)
- [ADR 0016](0016-metricas-tecnicas-prometheus.md)
- [Guia do Grafana](../grafana.md)
