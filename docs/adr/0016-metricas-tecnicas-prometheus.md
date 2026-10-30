# ADR 0016 — Métricas técnicas com Micrometer e Prometheus

- Status: Proposto
- Data: 2026-10-24

> O tempo médio de execução dos serviços está no [ADR 0022](0022-tempo-medio-execucao-grafana.md):
> latência média e p95 por rota deste dashboard, com aval do professor da Fase 1.

## Contexto

O Grafana do ADR 0015 precisa de uma fonte de dados reproduzível para demonstrar saúde e
desempenho da API. O Actuator já está presente. A duração de cada OS entre início e
finalização fica de fora desta coleta.

## Decisão

Adicionar o registro Prometheus do Micrometer, com versão gerenciada pelo Spring Boot.
Ativar exposição e histogramas HTTP somente com o perfil Spring `observability`.
Usar porta de management 9091 separada da API, em loopback por padrão. No Compose ela
escuta dentro do container, sem publicação no host. Uma cadeia de segurança específica
permite apenas o endpoint Prometheus; as APIs continuam exigindo seus JWT e roles.

Adicionar Prometheus 3.15.1 no profile Compose `observability`, com coleta a cada 15 segundos,
volume próprio e retenção limitada a 7 dias ou 1 GB. O Prometheus guarda séries técnicas,
não dados de negócio: é outra exceção de infraestrutura delimitada ao ADR 0002.
As interfaces Grafana/Prometheus são publicadas somente em loopback.

Provisionar fonte de dados e dashboard por arquivos versionados e montados somente para
leitura. Usar URI normalizada pelo Spring (templates), método e status; não adicionar IDs
de OS, CPF, placas ou tokens como labels. Excluir health/coleta dos painéis HTTP.
O p95 é estimado com histogramas; sem tráfego não há amostras de latência.
`up` indica sucesso da coleta, não a saúde funcional completa nem disponibilidade do banco.

## Consequências

O caminho recomendado de demonstração é app, banco e observabilidade no Compose.
Há configuração alternativa para JVM na máquina via host.docker.internal; nesse modo a
porta de management precisa aceitar conexões da bridge Docker, portanto usar apenas em
ambiente local confiável. Não habilitar essa exposição em ambiente público sem controle
adicional de rede/autenticação.

O dashboard apresenta HTTP, JVM e pool Hikari. A latência média e o p95 por rota são o
tempo médio de execução dos serviços (ADR 0022). Não monitora todo o PostgreSQL nem adiciona
logs, tracing ou alertas. `iniciada_em` e `finalizada_em` continuam só na OS.
Esta decisão complementa a instalação inicial do ADR 0015.

## Referências

- https://docs.spring.io/spring-boot/3.5/reference/actuator/metrics.html
- https://prometheus.io/docs/prometheus/latest/configuration/configuration/
- https://grafana.com/docs/grafana/latest/administration/provisioning/
