# oficina — Sistema Integrado de Atendimento e Execução de Serviços

Back-end (MVP) do sistema de gestão de uma oficina mecânica: ordens de serviço, clientes,
veículos, serviços, peças e estoque — Tech Challenge da Fase 1 do curso **SOAT (Software
Architecture)**, **Grupo 117**.

> Documentação DDD (Miro, acesso público): **[board público do grupo](https://miro.com/app/board/uXjVHnu_abs=/?share_link_id=659633037228)**
> Vídeo da entrega: **[LINK DO VÍDEO — preencher]**

## Objetivo do projeto

Substituir o processo manual (planilhas e anotações) do atendimento da oficina por um sistema
que garanta: controle de priorização dos atendimentos, controle de peças e insumos, acompanhamento
do status dos serviços, histórico de clientes e veículos e um fluxo confiável de orçamentos e
autorizações — com o cliente acompanhando o progresso via API.

## Descrição resumida da solução

API REST monolítica organizada por **Clean Architecture + DDD**, com três bounded contexts:

| Contexto | Responsabilidade |
|---|---|
| `atendimento` (core) | Ciclo de vida da Ordem de Serviço: criação, orçamento, aprovação, execução, entrega |
| `catalogoestoque` | Catálogo de serviços, peças e insumos, com controle e reserva de estoque |
| `identidadeacesso` | Identidade por requisição a partir do token do Keycloak; sem cadastro de usuários na aplicação |

Fluxo principal: cliente identificado por **CPF/CNPJ** → veículo cadastrado (placa, marca, modelo,
ano) → serviços e peças incluídos → **orçamento gerado automaticamente** → aprovação/rejeição pelo
cliente → execução com transições de status (Recebida → Em diagnóstico → Aguardando aprovação →
Em execução → Finalizada → Entregue).

## Arquitetura utilizada

Monolito em camadas seguindo **Clean Architecture** combinada com **DDD** (detalhes e
justificativa no [ADR 0001](docs/adr/0001-clean-architecture-ddd.md)). Cada bounded context possui
as camadas `domain` (regras de negócio puras), `application` (casos de uso e ports) e
`infrastructure` (controllers REST e adaptadores JPA), com dependências apontando sempre para dentro.

## Tecnologias utilizadas

- **Java 21**, **Spring Boot 3.5.x** (Web, Data JPA, Security, Validation, Actuator)
- **PostgreSQL 17** + **Flyway** (migrations) + Spring Data JPA
- **springdoc-openapi** (Swagger UI)
- **Notificações por e-mail**: servidor SMTP local **MailPit** em Docker (envio real via SMTP, e-mails visualizados em UI própria), compartilhado pela aplicação (orçamento, [ADR 0007](docs/adr/0007-notificacoes-smtp-local.md) e [ADR 0017](docs/adr/0017-notificacao-orcamento-email.md)) e pelo Keycloak (e-mails de ação de conta)
- **Keycloak** (autenticação e Perfis; [ADR 0006](docs/adr/0006-keycloak-autenticacao-autorizacao.md) e [ADR 0019](docs/adr/0019-integracao-operacional-keycloak.md)): a API valida o access token RS256 pelo JWKS
- **Testcontainers** (testes de integração com PostgreSQL real)
- **JaCoCo** (gate de 80% de cobertura em domain/application), **SonarCloud** (qualidade),
  **OWASP Dependency-Check** (análise de vulnerabilidades)
- **Docker** (Dockerfile multi-stage + docker-compose), **GitHub Actions** (CI + release por tag no GHCR)

## Pré-requisitos

- JDK 21 (Temurin)
- Docker + Docker Compose (PostgreSQL, Keycloak, MailPit e Testcontainers), com pelo menos **4 GB** de RAM para o Docker (medido no perfil de execução `full`: cerca de 1,6 GB em uso, mais folga para o build da imagem)
- `curl`, `jq` (1.6+) e `uuidgen`, usados nos exemplos de autenticação
- Não é necessário Maven instalado (o wrapper `./mvnw` baixa o necessário)

## Passo a passo para executar

Há dois perfis de execução do Compose:

| Perfil de execução | Serviços | A API roda |
|---|---|---|
| `dev` | `postgres`, `postgres-init` (one-shot que cria as roles e os schemas e termina), `keycloak`, `mailpit` | na máquina, com `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` |
| `full` | os mesmos + `app` | no container `app` |

Imagens: `postgres:17-alpine`, `quay.io/keycloak/keycloak:26.8.1-1` e `axllent/mailpit:v1.31.6`. Nos dois perfis de execução o token traz o issuer `http://localhost:8081/realms/oficina`, e a API valida o JWT por esse endereço.

> **Mudança em relação ao roteiro anterior (#91/E2E):** o Keycloak agora sobe pelo compose (perfis de execução `dev` e `full`, imagem `quay.io/keycloak/keycloak:26.8.1-1`), e não mais por `docker run` com a imagem 26.4.0; o único realm versionado é `keycloak/realm-oficina.json` (o `docs/e2e/keycloak/oficina-realm.json` foi removido no PR #79); a senha dos usuários de demonstração mudou (`demo123` deixou de valer por causa da política de senha do realm, com mínimo de 8 caracteres) e o admin do console (usuário `admin`) tem senha própria, diferente do usuário, com os valores na [tabela de credenciais](#credenciais-de-demonstração-somente-local); os registros de execução em `docs/e2e/2026-10-29*` foram mantidos como histórico e citam o roteiro antigo.

### 1. Postgres, Keycloak e MailPit

Na raiz do repositório:

```bash
docker compose --profile dev up -d --wait
```

O `--wait` só termina quando `postgres`, `keycloak` e `mailpit` estão saudáveis e o `postgres-init` terminou. Suba o compose antes do `./mvnw spring-boot:run`: é o `postgres-init` que cria a role `oficina_app` usada pela aplicação. O Postgres fica em `127.0.0.1:5432` e o MailPit em `127.0.0.1:8025` (interface) e `127.0.0.1:1025` (SMTP). Os usuários e as senhas estão na [tabela de credenciais](#credenciais-de-demonstração-somente-local).

```bash
docker compose ps -a
```

### 2. Keycloak

O Keycloak sobe com o compose e, na primeira subida, importa o realm versionado em `keycloak/realm-oficina.json` (realm `oficina`, client público `oficina-api`, sem client secret, Perfis `ATENDENTE`, `MECANICO` e `ADMINISTRADOR`). Confira o issuer:

```bash
curl -s http://localhost:8081/realms/oficina/.well-known/openid-configuration | jq -r .issuer
```

A saída precisa ser `http://localhost:8081/realms/oficina`. O console fica em `http://localhost:8081` (só em `127.0.0.1`), com o admin do realm `master` da tabela de credenciais.

### 3. API

Em outro terminal, na raiz:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

O Flyway aplica as migrations ao iniciar. Quando o log mostrar a aplicação no ar, confira:

```bash
curl -s http://localhost:8080/api/health
```

A resposta esperada é `status` `UP` e `app` `oficina`. O Actuator responde em `http://localhost:8080/actuator/health`.

Alternativa com a API em container (perfil de execução `full`; não rode o `spring-boot:run` ao mesmo tempo, porque os dois usam a porta `8080`):

```bash
docker compose --profile full up -d --build --wait
```

| Serviço | Endereço |
|---|---|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/api/health e http://localhost:8080/actuator/health |
| Keycloak (console, só `127.0.0.1`) | http://localhost:8081 |
| Endpoint de token | http://localhost:8081/realms/oficina/protocol/openid-connect/token |
| MailPit (só `127.0.0.1`) | http://localhost:8025 |

A porta `8080` da API é publicada em todas as interfaces (desvio conhecido, [ADR 0021](docs/adr/0021-politica-seguranca-identidade.md)); as demais ficam em `127.0.0.1`.

### 4. Catálogo de demonstração (opcional)

Com a API já no ar e o Flyway concluído, em outro terminal:

```bash
./scripts/popular-catalogo-demo.sh
```

Os detalhes da carga estão na seção [Catálogo de demonstração](#catálogo-de-demonstração-opcional).

### 5. Token e Swagger

O token, as chamadas de exemplo e os códigos esperados estão em [Procedimento de autenticação](#procedimento-de-autenticação). No Swagger, em **Authorize**, cole só o valor do `access_token` (sem o prefixo `Bearer`); para copiar sem exibir, use `printf %s "$TOKEN" | pbcopy` (macOS) ou `printf %s "$TOKEN" | xclip -selection clipboard` (Linux).

O e-mail do orçamento aparece no MailPit depois que a OS chega em `AGUARDANDO_APROVACAO` com contato cadastrado. O guia está em [docs/implementacao/notificacao-orcamento-email.md](docs/implementacao/notificacao-orcamento-email.md).

## E-mail do orçamento

Cadastre `email` em `POST /api/clientes` ou na abertura da OS. A geração do orçamento pela
conclusão do diagnóstico ou submissão agenda o envio automaticamente após o commit. Leia os
e-mails de demonstração no MailPit em **http://localhost:8025**. Cadastros antigos sem contato
continuam funcionando; a mensagem aguarda a inclusão do e-mail. Falhas SMTP são retentadas.
Veja o [guia de notificações](docs/implementacao/notificacao-orcamento-email.md).

## Catálogo de demonstração (opcional)

Após subir o banco **e iniciar a aplicação**, aguarde a conclusão das migrations Flyway.
Em outro terminal, na raiz do repositório, execute:

```bash
./scripts/popular-catalogo-demo.sh
```

O script usa o PostgreSQL do Compose e cadastra **4 serviços e 5 peças/insumos**, incluindo
um item sem estoque para demonstrar a validação. Não exige `psql` instalado no host nem JWT.
Funciona com o banco já iniciado pelos profiles `dev` ou `full`. A carga é manual e fica
fora das migrations: iniciar a aplicação normalmente não insere dados de demonstração.

Os IDs são fixos e exibidos no terminal. É possível repetir a carga: registros existentes,
preços alterados, reservas e estoque consumido são preservados. A carga não cria clientes,
veículos ou OS. Os detalhes e exemplos estão em [guia da demonstração](docs/demo.md).

## Observabilidade local (opcional)

```bash
APP_PROFILES=observability docker compose --profile full --profile observability up -d --build
```

Acesse **http://localhost:3000/d/oficina-api** com o usuário e a senha do Grafana da seção
[Credenciais de demonstração](#credenciais-de-demonstração-somente-local). O dashboard **Oficina — Saúde da API** já vem provisionado com
requisições, status HTTP, latência, JVM e conexões do banco. O Prometheus coleta a cada
15 segundos e está em http://localhost:9090. As duas interfaces são publicadas em localhost.

Para rodar a aplicação na IDE/Maven, interpretar os painéis ou personalizar as configurações,
veja o [guia de observabilidade](docs/grafana.md) e o [ADR 0016](docs/adr/0016-metricas-tecnicas-prometheus.md).
O tempo médio de execução dos serviços, com aval do professor da Fase 1, é a latência média
e o p95 das rotas nesse dashboard ([ADR 0022](docs/adr/0022-tempo-medio-execucao-grafana.md)).

## Instruções para execução dos testes

```bash
./mvnw test     # testes unitários e de integração (Testcontainers sobe um PostgreSQL real)
./mvnw verify   # idem + gate de cobertura de 80% nos pacotes domain/application
```

## Como contribuir

Branch, commits, PRs empilhados e o que a CI exige estão em [CONTRIBUTING.md](CONTRIBUTING.md).
As regras completas do repositório continuam em [AGENTS.md](AGENTS.md).

## Acesso ao Swagger / documentação da API

Com a aplicação no ar: **http://localhost:8080/swagger-ui.html** (OpenAPI em `/v3/api-docs`).

## Decisão e justificativa do banco de dados escolhido

**PostgreSQL** para todas as necessidades de persistência ("Postgres for everything" — ADR 0002):

- O fluxo crítico (criação de OS + reserva de peças no estoque + geração de orçamento) envolve
  **múltiplos agregados em uma única transação** — o modelo relacional com ACID garante
  consistência (ex.: não reservar a mesma peça duas vezes).
- Integridade referencial nativa entre cliente/veículo/OS/itens.
- JSONB disponível caso haja necessidade de dados semiestruturados, sem introduzir um segundo banco.
- Ecossistema maduro: JPA/Hibernate, Flyway e Testcontainers com suporte de primeira classe.

O Keycloak usa o mesmo banco `oficina`, no schema `keycloak` ([ADR 0020](docs/adr/0020-isolamento-dados-identidade.md)). Na primeira subida, o Keycloak cria sozinho cerca de 100 tabelas nesse schema: é o modelo fixo do produto, migrado por ele mesmo e fora do Flyway. Muitas são de recursos que não usamos (federação LDAP, login social, organizações, autorização fina) e ficam vazias. Elas não fazem parte do modelo da oficina, que fica no schema `public`. Detalhes em [Por que o Keycloak tem mais de 100 tabelas](docs/adr/0020-isolamento-dados-identidade.md#por-que-o-keycloak-tem-mais-de-100-tabelas).

## Usuários, credenciais de demonstração e procedimento de autenticação

As APIs administrativas exigem o access token do Keycloak (ADR 0006). A API valida o token pelo JWKS e não emite nem guarda senha. Consulta, aprovação e rejeição públicas da OS ficam em `/api/acompanhamento/ordens-de-servico/{id}` e não usam token.

### Credenciais de demonstração (somente local)

| Serviço/conta | Usuário | Senha | Onde se configura | Observação |
|---|---|---|---|---|
| Postgres administração | `oficina` | `oficina` | `docker-compose.yml` | superusuário da imagem, só administração e o init `postgres-init`; a app não o usa; `127.0.0.1:5432` |
| Postgres da app | `oficina_app` | `oficina-app-local` | `APP_DB_PASSWORD` | usuário da app e do Flyway, sem superusuário, dono do schema `public` |
| Keycloak no Postgres | `keycloak` | `keycloak-local` | `KEYCLOAK_DB_PASSWORD` | dono do schema `keycloak` do banco `oficina`, sem acesso ao `public` |
| Admin do Keycloak (realm `master`) | `admin` | `admin-local-demo` | `KC_BOOTSTRAP_ADMIN_USERNAME`, `KC_BOOTSTRAP_ADMIN_PASSWORD` | mudar a variável depois da 1ª subida não tem efeito sem reset do realm |
| Realm `oficina` | `atendente` | `Oficina@2026` | `keycloak/realm-oficina.json` | Perfil `ATENDENTE` |
| Realm `oficina` | `mecanico` | `Oficina@2026` | `keycloak/realm-oficina.json` | Perfil `MECANICO` |
| Realm `oficina` | `administrador` | `Oficina@2026` | `keycloak/realm-oficina.json` | Perfil `ADMINISTRADOR` |
| Grafana (perfil de execução `observability`) | `admin` | `grafana-local-demo` | `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD` | `127.0.0.1:3000`; a senha só vale no primeiro boot do volume |

Um banco só, `oficina` (ADR-0002, [ADR 0020](docs/adr/0020-isolamento-dados-identidade.md)): as tabelas da oficina ficam no schema `public` e as do Keycloak, criadas e migradas por ele mesmo, no schema `keycloak`; cada role só acessa o próprio schema.

Perfil é o termo do domínio; no Keycloak corresponde à *realm role* e no Spring à authority `ROLE_*` (ver [glossário](docs/ddd/linguagem-ubiqua.md)).

São valores públicos, só para o ambiente local: o enunciado pede usuários, credenciais de demonstração ou procedimento de autenticação. O password grant está habilitado só para demonstração (RFC 9700); em produção, Authorization Code + PKCE ([ADR 0021](docs/adr/0021-politica-seguranca-identidade.md)).

#### Desbloqueio de usuário

O realm tem proteção contra força bruta: 5 senhas erradas bloqueiam o usuário temporariamente (espera de 60 s a 900 s), e durante o bloqueio até a senha certa recebe `invalid_grant`. Duas tentativas com menos de 1 s de intervalo disparam o *quick login check*. Para desbloquear antes do tempo, use a admin API com o admin do realm `master`:

```bash
# Desbloqueio pela admin API (troque mecanico pelo usuário bloqueado)
ADM=$(curl -s -X POST http://localhost:8081/realms/master/protocol/openid-connect/token \
  -d grant_type=password -d client_id=admin-cli -d username="${KC_BOOTSTRAP_ADMIN_USERNAME:-admin}" \
  --data-urlencode "password=${KC_BOOTSTRAP_ADMIN_PASSWORD:-admin-local-demo}" | jq -r .access_token)
USR_ID=$(curl -s -H "Authorization: Bearer $ADM" \
  "http://localhost:8081/admin/realms/oficina/users?username=mecanico&exact=true" | jq -r '.[0].id')
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE -H "Authorization: Bearer $ADM" \
  "http://localhost:8081/admin/realms/oficina/attack-detection/brute-force/users/$USR_ID"                     # 204
```

O `204` também volta quando o usuário não está bloqueado (a chamada é idempotente).

### Procedimento de autenticação

Com o perfil de execução `dev` (e a API na máquina) ou `full` no ar, exporte a senha dos usuários de demonstração, copiada da tabela de credenciais acima, numa variável de shell (por exemplo, `read -rs SENHA_DEMO && export SENHA_DEMO`). Os blocos abaixo a leem de `SENHA_DEMO` e não a exibem.

```bash
# Funções de apoio (bash/zsh). jwtp decodifica o payload (ou o header com 1) sem exibir o token.
: "${SENHA_DEMO:?exporte SENHA_DEMO com a senha da tabela de credenciais}"
jwtp(){ local p; p=$(cut -d. -f"${2:-2}" <<<"$1" | tr '_-' '/+'); while (( ${#p} % 4 )); do p="$p="; done; base64 -d <<<"$p"; }
token(){ curl -s -X POST http://localhost:8081/realms/oficina/protocol/openid-connect/token \
  -d grant_type=password -d client_id=oficina-api -d username="$1" \
  --data-urlencode "password=$SENHA_DEMO" | jq -r .access_token; }

TOKEN=$(token atendente)                      # não faça echo "$TOKEN"
jwtp "$TOKEN" | jq '{iss,aud,azp,typ,sub,preferred_username,realm_access,exp,iat}'
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/clientes                                   # 401
curl -s -o /dev/null -w '%{http_code}\n' -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/clientes # 200
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/pecas/$(uuidgen)"                                                                # 403
TOKEN_ADM=$(token administrador)
curl -s -o /dev/null -w '%{http_code}\n' -X POST -H "Authorization: Bearer $TOKEN_ADM" \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Troca de oleo (demo)","descricao":"Demonstracao","preco":120.00}' \
  http://localhost:8080/api/servicos                                                                          # 201
```

As claims mostram `iss` `http://localhost:8081/realms/oficina`, `aud` e `azp` `oficina-api`, `typ` `Bearer` e, em `realm_access.roles`, só o Perfil do usuário; o token não traz e-mail nem nome. Ele vale 15 minutos. Troque `atendente` por `mecanico` ou `administrador` para os outros Perfis. Sem token a API responde `401`; com um Perfil sem permissão, `403`.

### Reset do realm

O realm só é importado na primeira subida. Para reimportar `keycloak/realm-oficina.json` (depois de mudar o arquivo ou `KC_BOOTSTRAP_ADMIN_*`), apague só o schema `keycloak`; o volume do banco e as tabelas da oficina ficam.

```bash
# Reset do realm (reimporta keycloak/realm-oficina.json). Apaga só o schema keycloak; as tabelas da oficina (schema public) ficam.
# NUNCA use "docker compose down -v" nem "docker volume rm" do pgdata: apagam o banco da oficina.
# Vale para os perfis de execução dev e full; não sobe o container app.
docker compose --profile dev stop keycloak
docker compose --profile dev exec -T postgres psql -U oficina -d oficina -c 'DROP SCHEMA keycloak CASCADE'
docker compose --profile dev up -d --wait postgres-init keycloak
```

O bloco foi medido nos dois perfis de execução: a API continua no ar e aceita os tokens do realm reimportado sem reinício. Com o perfil de execução `full` no ar, o `up` recria o container `postgres` (o volume `pgdata` fica) e a API reconecta sozinha.

### Comportamentos esperados

| Situação | Resultado | O que fazer |
|---|---|---|
| Senha errada 5 vezes | `invalid_grant` também com a senha certa, até o tempo de espera passar | [desbloqueio](#desbloqueio-de-usuário) |
| `Authorization: Bearer` inválido numa rota pública | `401` com `WWW-Authenticate: Bearer error="invalid_token", error_description="Token invalido"` | não envie `Authorization` em rota pública |
| Keycloak parado e a API sem a chave de assinatura em cache | `500` (falha técnica, não credencial inválida) | subir o Keycloak (`docker compose --profile full up -d keycloak`) |
| Alterou `keycloak/realm-oficina.json` e nada mudou | o realm antigo continua: o import só roda na 1ª subida | [reset do realm](#reset-do-realm) |
| Alterou `KC_BOOTSTRAP_ADMIN_PASSWORD` depois da 1ª subida | a senha antiga continua valendo: o bootstrap só roda com o `master` vazio | [reset do realm](#reset-do-realm) |
| Rodou `./mvnw spring-boot:run` sem subir o compose antes | a API não sobe (`password authentication failed for user "oficina_app"` ou `role "oficina_app" does not exist`) | subir o compose antes (passo 1) |

Mais detalhes da integração em [ADR 0019](docs/adr/0019-integracao-operacional-keycloak.md) e da política de segurança em [ADR 0021](docs/adr/0021-politica-seguranca-identidade.md).

## Estrutura dos principais diretórios

```
docs/adr/          decisões arquiteturais (ADRs)
docs/ddd/          linguagem ubíqua, glossário e event storming por fluxo
docs/diagramas/    diagramas C4 e de domínio
keycloak/          realm-oficina.json e postgres-init.sql (realm versionado, sem senhas nem chaves) reais: os usuários usam só a senha de demonstração
src/main/java/br/com/grupo117/oficina/
  atendimento/     contexto core (OS, orçamento) — domain/application/infrastructure
  catalogoestoque/ serviços, peças, estoque — domain/application/infrastructure
  identidadeacesso/ identidade por requisição a partir do token do Keycloak, Perfis — domain/application/infrastructure
  infra/           compartilhado: config, security, api
src/main/resources/db/migration/   migrations Flyway
src/test/java/     testes unitários e de integração (Testcontainers)
```

## Documentação DDD

- Board do Miro (acesso público): **[board público do grupo](https://miro.com/app/board/uXjVHnu_abs=/?share_link_id=659633037228)**
- Export no repositório: [`docs/ddd/`](docs/ddd/) (linguagem ubíqua e glossário) e
  [`docs/ddd/event-storming/`](docs/ddd/event-storming/) (um arquivo por fluxo),
  [`docs/diagramas/`](docs/diagramas/) (C4 e domínio).

## Release (imagem no GHCR)

A release é cortada manualmente a partir de um commit de `main` cujo check `ci` esteja verde
([ADR 0019](docs/adr/0019-release-por-tag-ghcr.md)). Não há deploy neste fluxo; a hospedagem
na AWS fica para um ADR futuro.

```bash
# a partir de main atualizada, no SHA desejado:
git tag -a v0.1.0 -m "v0.1.0"
git push origin v0.1.0
```

O workflow `.github/workflows/release.yml` publica:

- Imagem: `ghcr.io/bernacamargo/tech-challenge-18soat:0.1.0` (e `sha-<curto>`)
- [GitHub Release](https://github.com/bernacamargo/tech-challenge-18soat/releases) com notas geradas

Após o primeiro push, nas Settings do pacote no GHCR: vincular o repositório e tornar o pacote
público (o token do Actions não altera visibilidade).

## Análise de vulnerabilidades

- **SonarCloud**: [dashboard do projeto](https://sonarcloud.io/project/overview?id=bernacamargo_tech-challenge-18soat) (quality gate + cobertura)
- **OWASP Dependency-Check**: relatório HTML gerado pelo workflow `security` (artifact no GitHub Actions) e semanalmente agendado.

## Link para o vídeo da entrega

**[LINK DO VÍDEO (YouTube, público) — preencher]**
