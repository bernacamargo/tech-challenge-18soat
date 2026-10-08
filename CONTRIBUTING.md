# Contribuindo

Guia curto para quem contribui neste repositório (Grupo 117, Tech Challenge SOAT — oficina).
As regras completas estão em [AGENTS.md](AGENTS.md); este arquivo resume o fluxo de Git, o CI
e como rodar o projeto localmente. O corpo do pull request segue
[.github/PULL_REQUEST_TEMPLATE.md](.github/PULL_REQUEST_TEMPLATE.md).

## Branches

- Nada vai direto para `main`. A branch é protegida: toda mudança entra por pull request com a CI verde.
- Nomes: `feat/*`, `fix/*`, `docs/*`, `chore/*`.

## Commits

Commits semânticos (Conventional Commits): `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:`.

Exemplo: `feat(atendimento): cria caso de uso de aprovação de orçamento`.

## Pull requests empilhados

Uma fatia vertical vira uma pilha de PRs por camada. Cada PR se baseia no anterior e os merges
seguem essa ordem, sem pular etapa:

`feat/<contexto>-domain` → `feat/<contexto>-application` → `feat/<contexto>-api`

PRs pequenos e revisáveis por camada. Mudança só de documentação pode ser um PR independente.

## Descrição do pull request

Use o template: **WHY / WHAT / HOW / ADDITIONAL DETAILS (evidências)**. As evidências
(SonarCloud, cobertura JaCoCo, screenshots) são obrigatórias e alimentam a entrega final.

## CI

O check obrigatório para mergear em `main` é o workflow `.github/workflows/ci.yml`
(build, testes, cobertura e SonarCloud). O mesmo gate de build e cobertura, localmente:

```bash
./mvnw -B verify
```

- **JaCoCo**: mínimo de 80% de linhas nos pacotes `domain` e `application`. Abaixo disso o `verify` falha ([ADR 0003](docs/adr/0003-gate-cobertura-80-ci.md)).
- **SonarCloud**: com o secret `SONAR_TOKEN` configurado, o workflow roda `./mvnw -B sonar:sonar`. Sem o token, o job registra que a análise foi pulada e segue. O quality gate do PR aparece no check do SonarCloud ([ADR 0005](docs/adr/0005-sonarcloud-dependency-check.md)).

## Build e testes locais

Pré-requisitos:

- JDK 21 (Temurin)
- Docker e Docker Compose. Os testes de integração usam Testcontainers e sobem um PostgreSQL real; sem o Docker esses testes não rodam.
- Maven instalado na máquina não é necessário: o wrapper `./mvnw` baixa o que precisa.

```bash
docker compose --profile dev up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

./mvnw test     # testes unitários e de integração (Testcontainers)
./mvnw verify   # o mesmo conjunto + gate de cobertura de 80%
```

Comandos opcionais, os mesmos de [AGENTS.md](AGENTS.md):

```bash
./mvnw sonar:sonar              # envia a análise ao SonarCloud (requer SONAR_TOKEN)
./mvnw dependency-check:check   # relatório CVE local
```

Aplicação, banco e demais serviços juntos: `docker compose --profile full up -d --build`.

## Definição de pronto

Resumo do que [AGENTS.md](AGENTS.md) exige antes do merge:

- Testes de unidade (`domain`/`application`, sem Spring) e de integração (estendem `AbstractIntegrationTest`).
- Cobertura mínima de 80% de linha em `domain` e `application`.
- APIs novas documentadas via springdoc e visíveis no Swagger.
- `@Transactional` nos casos de uso que mexem em mais de um agregado.
- Mudança de arquitetura ou de banco: abrir um ADR em `docs/adr/` antes de mergear.
