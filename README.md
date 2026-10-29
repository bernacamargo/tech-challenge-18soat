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
| `identidadeacesso` | Usuários administrativos e autenticação JWT das APIs administrativas |

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
- **Notificações por e-mail**: servidor SMTP local **MailPit** em Docker (envio real via SMTP, e-mails visualizados em UI própria)
- **Testcontainers** (testes de integração com PostgreSQL real)
- **JaCoCo** (gate de 80% de cobertura em domain/application), **SonarCloud** (qualidade),
  **OWASP Dependency-Check** (análise de vulnerabilidades)
- **Docker** (Dockerfile multi-stage + docker-compose), **GitHub Actions** (CI)

## Pré-requisitos

- JDK 21 (Temurin)
- Docker + Docker Compose (PostgreSQL, MailPit, Keycloak e Testcontainers)
- Não é necessário Maven instalado (o wrapper `./mvnw` baixa o necessário)

## Passo a passo para executar

A API sobe na máquina, com o perfil `local`. O banco e o MailPit vêm do Compose. O Keycloak sobe num container à parte, na porta `8081`. O token emitido traz o issuer `http://localhost:8081/realms/oficina`, e a API valida o JWT por esse endereço.

### 1. PostgreSQL e MailPit

Na raiz do repositório:

```bash
docker compose --profile dev up -d
```

O perfil `dev` publica o Postgres em `localhost:5432` (banco, usuário e senha `oficina`) e o MailPit em `localhost:8025` (interface) e `localhost:1025` (SMTP). Espere o Postgres ficar saudável:

```bash
docker compose ps
```

### 2. Keycloak

O realm de demonstração está em `docs/e2e/keycloak/oficina-realm.json` (realm `oficina`, client público `oficina-api`, papéis `ATENDENTE`, `MECANICO` e `ADMINISTRADOR`).

```bash
docker run -d --name keycloak \
  -p 8081:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  -e KC_HOSTNAME=http://localhost:8081 \
  -e KC_HOSTNAME_STRICT=false \
  -e KC_HTTP_ENABLED=true \
  -e KC_HEALTH_ENABLED=true \
  -v "$PWD/docs/e2e/keycloak:/opt/keycloak/data/import:ro" \
  quay.io/keycloak/keycloak:26.4.0 \
  start-dev --import-realm --http-port=8080
```

A primeira subida leva um pouco. Confira o issuer antes de seguir:

```bash
curl -s http://localhost:8081/realms/oficina/.well-known/openid-configuration
```

O campo `issuer` precisa ser `http://localhost:8081/realms/oficina`. O console fica em `http://localhost:8081` (usuário `admin`, senha `admin`, só para desenvolvimento).

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

| Serviço | Endereço |
|---|---|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/api/health |
| MailPit | http://localhost:8025 |
| Keycloak | http://localhost:8081 |

### 4. Catálogo de demonstração (opcional)

Com a API já no ar e o Flyway concluído, em outro terminal:

```bash
./scripts/popular-catalogo-demo.sh
```

Os detalhes da carga estão na seção [Catálogo de demonstração](#catálogo-de-demonstração-opcional).

### 5. Token e Swagger

Usuários locais (senha `demo123`): `atendente`, `mecanico`, `administrador`.

```bash
curl -s -X POST "http://localhost:8081/realms/oficina/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=oficina-api&username=administrador&password=demo123"
```

Troque `username` para obter o token dos outros papéis. No Swagger, em **Authorize**, informe `Bearer` seguido do `access_token`.

O e-mail do orçamento aparece no MailPit depois que a OS chega em `AGUARDANDO_APROVACAO` com contato cadastrado. O guia está em [docs/implementacao/notificacao-orcamento-email.md](docs/implementacao/notificacao-orcamento-email.md).

Para entrar no Swagger com os usuários de demonstração, siga os passos 1 a 3. O comando `docker compose --profile full up -d --build` publica a API junto com Postgres e MailPit; o login deste passo a passo continua na API executada na máquina, com o issuer em `localhost:8081`.

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

Acesse **http://localhost:3000/d/oficina-api** com usuário `admin` e senha local
`grafana-local-demo`. O dashboard **Oficina — Saúde da API** já vem provisionado com
requisições, status HTTP, latência, JVM e conexões do banco. O Prometheus coleta a cada
15 segundos e está em http://localhost:9090. As duas interfaces são publicadas em localhost.

Para rodar a aplicação na IDE/Maven, interpretar os painéis ou personalizar as configurações,
veja o [guia de observabilidade](docs/grafana.md) e o [ADR 0016](docs/adr/0016-metricas-tecnicas-prometheus.md).
O tempo de execução dos serviços da oficina continua para uma etapa posterior.

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

## Usuários e credenciais de demonstração

As APIs administrativas exigem o JWT do Keycloak (ADR-0006). O passo a passo acima sobe o realm `oficina` e o client público `oficina-api`. A API valida o token pelo JWKS; ela não emite nem guarda senha.

**Usuários de demonstração** (somente ambiente local):

| Usuário | Senha | Papel |
|---|---|---|
| `atendente` | `demo123` | `ATENDENTE` — atendimento, clientes, veículos e abertura de OS |
| `mecanico` | `demo123` | `MECANICO` — diagnóstico, execução e leitura do catálogo |
| `administrador` | `demo123` | `ADMINISTRADOR` — cadastro de serviços, peças e estoque, além das demais rotas |

O comando do token está no passo 5. Sem `Authorization`, as rotas administrativas respondem 401. Consulta, aprovação e rejeição públicas da OS ficam em `/api/acompanhamento/ordens-de-servico/{id}` e não usam JWT.

## Estrutura dos principais diretórios

```
docs/adr/          decisões arquiteturais (ADRs)
docs/ddd/          linguagem ubíqua, glossário e event storming por fluxo
docs/diagramas/    diagramas C4 e de domínio
src/main/java/br/com/grupo117/oficina/
  atendimento/     contexto core (OS, orçamento) — domain/application/infrastructure
  catalogoestoque/ serviços, peças, estoque — domain/application/infrastructure
  identidadeacesso/ usuários administrativos, JWT — domain/application/infrastructure
  infra/           compartilhado: config, security, api
src/main/resources/db/migration/   migrations Flyway
src/test/java/     testes unitários e de integração (Testcontainers)
```

## Documentação DDD

- Board do Miro (acesso público): **[board público do grupo](https://miro.com/app/board/uXjVHnu_abs=/?share_link_id=659633037228)**
- Export no repositório: [`docs/ddd/`](docs/ddd/) (linguagem ubíqua e glossário) e
  [`docs/ddd/event-storming/`](docs/ddd/event-storming/) (um arquivo por fluxo),
  [`docs/diagramas/`](docs/diagramas/) (C4 e domínio).

## Análise de vulnerabilidades

- **SonarCloud**: [dashboard do projeto](https://sonarcloud.io/project/overview?id=bernacamargo_tech-challenge-18soat) (quality gate + cobertura)
- **OWASP Dependency-Check**: relatório HTML gerado pelo workflow `security` (artifact no GitHub Actions) e semanalmente agendado.

## Link para o vídeo da entrega

**[LINK DO VÍDEO (YouTube, público) — preencher]**
