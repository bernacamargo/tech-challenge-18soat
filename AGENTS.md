# AGENTS.md — instruções para agentes de IA (Claude, Cursor, etc.)

Este arquivo é o ponto de partida de qualquer agente trabalhando neste repositório.
Projeto: **Sistema Integrado de Atendimento e Execução de Serviços de uma oficina mecânica** —
Tech Challenge da fase 1 do curso **SOAT (Software Architecture)**, grupo **117**.

## Contexto e documentação (leia antes de codificar)

- Requisitos e entregáveis: ver material do grupo; resumo dos requisitos em `README.md`.
- Linguagem ubíqua e glossário (atores, agregados, VOs): `docs/ddd/linguagem-ubiqua.md`
- Event Storming por fluxo: `docs/ddd/event-storming/`
- Diagramas (C4 e domínio): `docs/diagramas/`
- Decisões arquiteturais: `docs/adr/` — **toda decisão de arquitetura relevante gera um ADR novo**.

## Stack

- Java 21 (somente Java; sem Kotlin), Spring Boot 3.5.x, Maven via wrapper (`./mvnw` — não requer Maven instalado)
- Autenticação/autorização: **Keycloak self-hosted** (realm `oficina`, perfis `ATENDENTE`/`MECANICO`/`ADMINISTRADOR` como realm roles; realm versionado único em `keycloak/realm-oficina.json`, importado pelo compose com `quay.io/keycloak/keycloak:26.8.1-1`): a API valida o access token (RS256, issuer + audience `oficina-api`) e o contexto `identidadeacesso` monta a identidade por requisição; as tabelas do Keycloak ficam no schema `keycloak` do banco único `oficina`, com role própria (a app usa `oficina_app`, sem superusuário). Ver ADR 0006, 0019, 0020 e 0021
- E-mail: **SMTP local via MailPit** (Docker, SMTP 127.0.0.1:1025 / UI 127.0.0.1:8025), compartilhado: usado pela aplicação (notificação de orçamento, #82; port `NotificacaoPort` na application — ADR 0007/0017) e pelo **Keycloak** (e-mails de ação de conta)
- PostgreSQL 17 ("Postgres for everything", ADR 0002), **banco único `oficina`**: schema `public` com a role `oficina_app` (app e Flyway, sem superusuário) e schema `keycloak` com a role `keycloak` (tabelas criadas e migradas pelo próprio Keycloak); roles e permissões em `keycloak/postgres-init.sql`, aplicado pelo serviço `postgres-init` do compose (ADR 0020). Spring Data JPA + Flyway (migrations só da oficina, snake_case)
- Testcontainers para testes de integração (PostgreSQL real, `@ServiceConnection`)
- springdoc-openapi 2.8.x (Swagger UI em `/swagger-ui.html`)
- SonarCloud (qualidade + cobertura) e OWASP Dependency-Check (CVE de dependências)

## Arquitetura — Clean Architecture + DDD (ADR 0001)

Um pacote por bounded context, com camadas por dentro:

```
br.com.grupo117.oficina
├── atendimento          (contexto core: Ordem de Serviço)
├── catalogoestoque      (serviços, peças, estoque)
├── identidadeacesso     (identidade por requisição a partir do token do Keycloak, ADR 0006/0019: valida o Bearer RS256
│                         com iss/aud pelo caso de uso `AutenticarUsuarioAdministrativo`; sem tabela de usuário)
└── infra                (compartilhado: config, security, api)
```

Cada contexto tem `domain` / `application` / `infrastructure`:

- **domain**: agregados, entidades, value objects (records), eventos de domínio, regras de negócio. **Sem dependência de framework (Spring/JPA)**.
- **application**: casos de uso e ports de saída (interfaces). Orquestra o domínio. Sem controllers/repositorios aqui. Exceção consciente: em `identidadeacesso`, o port `ValidadorDeCredencial` fica no `domain`, como a issue #4 pediu (ADR 0019).
- **infrastructure**: controllers REST, adaptadores JPA (`*Repository`), implementações de ports. **Depende para dentro; domain nunca depende de infrastructure**.

Convenções de nome:

- **Domínio em português** (linguagem ubíqua): `OrdemDeServico`, `Peca`, `Veiculo`, `CpfCnpj`, `Placa`, `StatusOS`, `Orcamento`, `ReservaDeEstoque`, `ItemDaOS`.
- **Infraestrutura em inglês**: controllers, configs, security, application.yml.
- Tabelas/colunas em `snake_case`, nomes em português. Value objects e DTOs como **records**. **Sem Lombok**.

## Comandos

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local   # app local (requer compose dev)
docker compose --profile dev up -d                        # PostgreSQL (+ postgres-init), Keycloak e MailPit
docker compose --profile full up -d --build               # app + os serviços do perfil dev
APP_PROFILES=observability docker compose --profile full --profile observability up -d --build   # + Grafana e Prometheus (#76/#84)
./mvnw test                                               # testes unitários + integração (Testcontainers)
./mvnw verify                                             # testes + gate de cobertura (80% em domain/application)
./mvnw sonar:sonar                                        # envia análise ao SonarCloud (requer SONAR_TOKEN)
./mvnw dependency-check:check                             # relatório CVE local
```

## Teste ponta a ponta

Para repetir o teste do sistema no ar (atendimento, catálogo e estoque, identidade e notificação, com Swagger, MailPit, Keycloak e relatório versionado por data/hora), siga a skill [`.cursor/skills/teste-e2e/SKILL.md`](.cursor/skills/teste-e2e/SKILL.md). As execuções ficam em `docs/e2e/`.

## Qualidade — definição de pronto

- Testes de unidade (domain/application, sem Spring) **e** de integração (fluxos, extends `AbstractIntegrationTest`).
- **Cobertura mínima de 80% de linha nos pacotes `domain` e `application`** — o gate do JaCoCo falha o build caso contrário (ADR 0003).
- APIs novas documentadas via springdoc (anotações) e visíveis no Swagger.
- Transações tratadas nos fluxos principais (`@Transactional` em casos de uso com múltiplos agregados).
- Mudanças de arquitetura/banco: **ABRIR ADR** em `docs/adr/` antes de mergear.
- CI verde: `ci.yml` (build+testes+cobertura+Sonar) é check obrigatório para mergear em `main`.

## Git e colaboração (obrigatório pelo enunciado)

- **Nada vai direto para `main`** — main protegida: tudo via Pull Request com CI verde.
- Branches: `feat/*`, `fix/*`, `docs/*`, `chore/*`.
- **PRs empilhados (stack) para fatias verticais**: divida a fatia em PRs por camada —
  `feat/<contexto>-domain` → `feat/<contexto>-application` → `feat/<contexto>-api` — cada um
  baseado no anterior e **mesclados em ordem estrita**; docs-only podem ser PRs independentes.
  PRs pequenos e revisáveis por camada, nunca um PR gigante com tudo junto.
- **Commits semânticos** (conventional commits): `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:` — ex.: `feat(atendimento): cria caso de uso de aprovação de orçamento`.
- PRs usam o template (`.github/PULL_REQUEST_TEMPLATE.md`): **WHY / WHAT / HOW / ADDITIONAL DETAILS (evidências)** — evidências (Sonar, cobertura, screenshots) são obrigatórias, alimentam a entrega final.
