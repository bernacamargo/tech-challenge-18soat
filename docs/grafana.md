# Observabilidade local: Grafana e Prometheus

O Prometheus coleta métricas do Actuator a cada 15 segundos. O Grafana consulta esse histórico.
Fonte de dados e dashboard são provisionados pelos arquivos de `observability/`, sem configuração
manual na interface. Não é necessário Grafana Cloud. O login do Grafana é independente do JWT.

## Ambiente completo pelo Docker (recomendado)

Pare antes a aplicação local que estiver usando a porta 8080. Na raiz do repositório:

```bash
APP_PROFILES=observability docker compose --profile full --profile observability up -d --build
```

- Dashboard: http://localhost:3000/d/oficina-api — **Oficina — Saúde da API**, pasta **Oficina**.
- Login inicial: `admin` / `grafana-local-demo`.
- Prometheus e targets: http://localhost:9090/targets — job `oficina` deve ficar UP.
- Swagger: http://localhost:8080/swagger-ui.html.

Espere a aplicação iniciar e duas coletas (cerca de 30 segundos). Faça requisições reais pelo
Swagger ou sua ferramenta HTTP. Os painéis HTTP excluem health e coleta, para não confundir
tráfego de monitoramento com uso da API. Sem tráfego, latência pode aparecer sem dados.
A senha inicial só é usada no primeiro boot; mudar a variável não redefine a senha persistida.

## Aplicação pela IDE/Maven

O Prometheus precisa acessar a JVM na máquina. Se já houver uma aplicação rodando, reinicie-a
com o perfil adicional. Preserve as variáveis de autenticação utilizadas em seus testes locais.

```bash
PROMETHEUS_CONFIG_FILE=./observability/prometheus/prometheus-local.yml \
  docker compose --profile dev --profile observability up -d
MANAGEMENT_SERVER_ADDRESS=0.0.0.0 ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=local,observability
```

Nesse modo, `host.docker.internal` aponta para o host via `host-gateway`, inclusive no Linux.
A porta 9091 aceita conexões da bridge Docker e demais interfaces: use em ambiente local
confiável. No modo completo Docker, 9091 fica interna e não é publicada. A porta normal 8080
não expõe o endpoint Prometheus; a autorização das APIs continua igual.
Para voltar ao modo completo, remova `PROMETHEUS_CONFIG_FILE` do ambiente e use o primeiro comando.

## Painéis

| Painel | Interpretação |
|---|---|
| Coleta da aplicação | 1 = Prometheus conseguiu coletar; 0 = falha; não garante saúde do banco |
| Tempo desde inicialização | Uptime do processo Java |
| Requisições por rota | Taxa por segundo, agrupada por método e template da rota |
| Respostas por status | 4xx incluem validações e acesso negado; 5xx são falhas internas |
| Latência p95 e média | Tempo HTTP em segundos; p95 estimado cobre 95% das respostas |
| Memória heap / CPU | Recursos utilizados pela JVM/processo Java |
| Conexões do pool | Ocupadas, livres e limite do Hikari da aplicação |
| Threads esperando conexão | Espera por conexão disponível no pool |

Os painéis não medem o tempo de execução dos serviços da oficina (issue #39). Não há logs,
tracing, alertas ou métricas de negócio nesta entrega. Os arquivos JSON são a fonte de verdade:
para alterar o dashboard padrão, edite-os e versione a mudança, em vez de salvar pela UI.

## Operação

```bash
docker compose --profile observability ps
curl --fail http://localhost:3000/api/health
curl --fail http://localhost:9090/-/ready
docker compose --profile observability logs --tail=50 prometheus grafana
# Parar apenas observabilidade, preservando volumes:
docker compose --profile observability stop prometheus grafana
```

O profile Compose seleciona os containers; `APP_PROFILES=observability` seleciona o perfil
Spring do app containerizado. São configurações diferentes. `dev`/`full` isolados não sobem
Prometheus/Grafana. Apenas subir os containers de observabilidade não instrumenta uma JVM antiga.

Se houver **No data**, confira o target no Prometheus, o perfil da aplicação e a configuração
`prometheus.yml` (app Docker) ou `prometheus-local.yml` (JVM no host). Se o target estiver UP,
gere tráfego e aguarde ao menos duas coletas. Ausência de dados não equivale a zero erros.

Variáveis opcionais: `GRAFANA_PORT` (3000), `PROMETHEUS_PORT` (9090), `GRAFANA_ADMIN_USER`
(admin), `GRAFANA_ADMIN_PASSWORD` (grafana-local-demo), `PROMETHEUS_CONFIG_FILE` (config Docker).
Defina credenciais antes do primeiro boot ou altere a senha pelo Grafana depois.

Os volumes `grafana_data` e `prometheus_data` preservam metadados e séries após recriação.
A retenção Prometheus é limitada a 7 dias ou 1 GB. Não use `docker compose down -v` para parar:
isso também pode apagar os dados PostgreSQL do projeto.

Decisões: [ADR 0015](adr/0015-grafana-local-docker.md) e
[ADR 0016](adr/0016-metricas-tecnicas-prometheus.md).

## Evidências locais — 2026-10-24

- `./mvnw verify`: 487 testes passaram e gate JaCoCo aprovado.
- Testes de observabilidade verificam scrape anônimo na porta de management, histogramas,
  JVM e Hikari; endpoint ausente na porta da API mesmo com JWT de administrador; APIs
  administrativas permanecem protegidas. Também verificam perfil desabilitado.
- Build da imagem da aplicação pelo Docker Compose concluído.
- Ambas as configurações Prometheus passaram no `promtool check config`.
- Ambiente Compose temporário com PostgreSQL próprio, JAR compilado em Java 21, Prometheus
  e Grafana: target UP, datasource saudável, dashboard provisionado e consultas dos dez
  painéis retornando séries reais após tráfego HTTP. Parar a aplicação produziu `up=0`.
- Histórico Prometheus preservado após recriar seu container. Containers e volumes do
  ambiente temporário removidos ao final; dados do banco local preservados.
