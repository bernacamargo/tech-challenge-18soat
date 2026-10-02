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
- PostgreSQL 17 ("Postgres for everything"), Spring Data JPA + Flyway (migrations, snake_case)
- Testcontainers para testes de integração (PostgreSQL real, `@ServiceConnection`)
- springdoc-openapi 2.8.x (Swagger UI em `/swagger-ui.html`)
- SonarCloud (qualidade + cobertura) e OWASP Dependency-Check (CVE de dependências)

## Arquitetura — Clean Architecture + DDD (ADR 0001)

Um pacote por bounded context, com camadas por dentro:

```
br.com.grupo117.oficina
├── atendimento          (contexto core: Ordem de Serviço)
├── catalogoestoque      (serviços, peças, estoque)
├── identidadeacesso     (usuários administrativos, JWT)
└── infra                (compartilhado: config, security, api)
```

Cada contexto tem `domain` / `application` / `infrastructure`:

- **domain**: agregados, entidades, value objects (records), eventos de domínio, regras de negócio. **Sem dependência de framework (Spring/JPA)**.
- **application**: casos de uso e ports de saída (interfaces). Orquestra o domínio. Sem controllers/repositorios aqui.
- **infrastructure**: controllers REST, adaptadores JPA (`*Repository`), implementações de ports. **Depende para dentro; domain nunca depende de infrastructure**.

Convenções de nome:

- **Domínio em português** (linguagem ubíqua): `OrdemDeServico`, `Peca`, `Veiculo`, `CpfCnpj`, `Placa`, `StatusOS`, `Orcamento`, `ReservaDeEstoque`, `ItemDaOS`.
- **Infraestrutura em inglês**: controllers, configs, security, application.yml.
- Tabelas/colunas em `snake_case`, nomes em português. Value objects e DTOs como **records**. **Sem Lombok**.

## Comandos

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local   # app local (requer compose dev)
docker compose --profile dev up -d                        # só o PostgreSQL
docker compose --profile full up -d --build               # app + PostgreSQL
./mvnw test                                               # testes unitários + integração (Testcontainers)
./mvnw verify                                             # testes + gate de cobertura (80% em domain/application)
./mvnw sonar:sonar                                        # envia análise ao SonarCloud (requer SONAR_TOKEN)
./mvnw dependency-check:check                             # relatório CVE local
```

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
- **Commits semânticos** (conventional commits): `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:` — ex.: `feat(atendimento): cria caso de uso de aprovação de orçamento`.
- PRs usam o template (`.github/PULL_REQUEST_TEMPLATE.md`): **WHY / WHAT / HOW / ADDITIONAL DETAILS (evidências)** — evidências (Sonar, cobertura, screenshots) são obrigatórias, alimentam a entrega final.

## Decisões pendentes (não implementar sem definir)

- **Mecanismo de envio de notificações/orçamentos ao cliente** — aguardando resposta no fórum do professor. Quando definido, entra como **port na camada application** com implementação em infrastructure (SMTP local via Docker é a hipótese atual).
