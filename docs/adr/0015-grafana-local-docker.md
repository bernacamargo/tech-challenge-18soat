# ADR 0015 — Grafana local pelo Docker Compose

- Status: Proposto
- Data: 2026-10-24
- Contexto: infraestrutura inicial de observabilidade, sem implementação da issue #39

> A coleta e o provisionamento posteriores estão definidos no [ADR 0016](0016-metricas-tecnicas-prometheus.md).

## Contexto

O grupo precisa disponibilizar o Grafana de forma reproduzível para desenvolvimento e
avaliação. A definição e implementação da métrica de tempo médio de execução dos serviços
ficará para uma etapa posterior. Instalar a interface não implica coletar métricas da aplicação.

## Decisão

Adicionar a imagem oficial open source `grafana/grafana`, fixada em `13.2.4`, ao Compose.
O profile opt-in `observability` permite iniciar somente o Grafana ou combiná-lo com `dev`/`full`.
O serviço não depende do PostgreSQL nem da aplicação. Publicar a porta somente no loopback
(`127.0.0.1:3000` por padrão), com autenticação própria, cadastro público e acesso anônimo desabilitados.
Usuário, senha inicial e porta podem ser configurados por variáveis de ambiente.

Persistir `/var/lib/grafana` em volume nomeado `grafana_data`. O banco SQLite embutido armazena
exclusivamente os metadados internos da ferramenta (usuários, preferências e dashboards).
Isso delimita uma exceção de infraestrutura ao ADR 0002: os dados de negócio continuam
exclusivamente no PostgreSQL, sem novo banco nem datasource na aplicação. Usar PostgreSQL
para metadados do Grafana poderá ser reavaliado quando houver requisitos de disponibilidade.

O healthcheck consulta `/api/health` no próprio container. Não provisionar fontes de dados,
Prometheus, logs, tracing ou dashboards nesta fatia. Não criar conexão com o banco da oficina
nem conceder acesso aos seus dados. O login do Grafana é independente de Keycloak/JWT da API.

## Consequências

O ambiente padrão `dev`/`full` não inicia o Grafana sem a seleção explícita do profile ou
serviço. O avaliador pode abrir a interface, autenticar e preservar configurações após recriar
o container. A aplicação permanece sem alteração de código, schema, regras de domínio ou
exposição de endpoints Actuator adicionais.

O volume precisa ser preservado para conservar os metadados. A configuração de senha inicial
é usada na criação do administrador; mudar a variável não altera um usuário já persistido.
Credenciais locais de demonstração são documentadas; outros ambientes exigem configuração própria.
O serviço isolado ainda não demonstra métricas reais nem encerra a issue #39.

## Referências

- [Imagem Docker oficial e persistência](https://grafana.com/docs/grafana/latest/setup-grafana/installation/docker/)
- [Configuração por variáveis de ambiente](https://grafana.com/docs/grafana/latest/setup-grafana/configure-docker/)
- [Release 13.2.4](https://github.com/grafana/grafana/releases/tag/v13.2.4)
