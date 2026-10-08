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
- Docker + Docker Compose (para o banco e para testes de integração via Testcontainers)
- Não é necessário Maven instalado (o wrapper `./mvnw` baixa o necessário)

## Instruções para execução

```bash
# 1. Subir o PostgreSQL
docker compose --profile dev up -d

# 2. Rodar a aplicação (perfil local)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Acesse:
#   API:         http://localhost:8080
#   Health:      http://localhost:8080/actuator/health
#   Swagger UI:  http://localhost:8080/swagger-ui.html
```

Alternativa totalmente containerizada: `docker compose --profile full up -d --build`.

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

## Usuários e credenciais de demonstração / procedimento de autenticação

As APIs administrativas usam autenticação **JWT via Keycloak** self-hosted (sobem junto no
docker-compose, porta `8081`), com o realm **`oficina`** importado automaticamente (config-as-code)
e os papéis `ATENDENTE`, `MECANICO` e `ADMINISTRADOR` (ADR-0006).

**Usuários de demonstração** (somente ambiente local — nunca usar em produção):

| Usuário | Senha | Papel |
|---|---|---|
| `atendente` | `demo123` | `ATENDENTE` — atendimento e criação de OS |
| `mecanico` | `demo123` | `MECANICO` — diagnóstico e execução |
| `administrador` | `demo123` | `ADMINISTRADOR` — CRUD de clientes, veículos, serviços, peças e estoque |

**Procedimento para obter o token** (client público `oficina-api`, realm `oficina`):

```bash
# 1. Subir o ambiente (aplicação + postgres + keycloak)
docker compose --profile full up -d

# 2. Obter o access token (password grant, habilitado apenas para demonstração)
curl -X POST "http://localhost:8081/realms/oficina/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=oficina-api&scope=openid" \
  -d "username=administrador&password=demo123"

# 3. Chamar uma API administrativa com o token
curl http://localhost:8080/api/admin/... \
  -H "Authorization: Bearer <access_token>"
```

O console administrativo do Keycloak fica em `http://localhost:8081` (login `admin`/`admin`
definido no `docker-compose.yml`, apenas para desenvolvimento). O token é validado pela API via
JWKS do Keycloak — a aplicação não emite nem armazena credenciais.

> ℹ️ Este fluxo entra em funcionamento com a fatia vertical de `identidadeacesso`
> ([ADR-0006](docs/adr/0006-keycloak-autenticacao-autorizacao.md)); até lá as APIs administrativas
> respondem 401 para qualquer credencial.

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
