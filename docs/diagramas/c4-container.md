# C4 — Container (nível 2): runtime da requisição autenticada

Decisões: [ADR 0002](../adr/0002-postgres-for-everything.md), [ADR 0006](../adr/0006-keycloak-autenticacao-autorizacao.md), [ADR 0007](../adr/0007-notificacoes-smtp-local.md), [ADR 0016](../adr/0016-metricas-tecnicas-prometheus.md), [ADR 0017](../adr/0017-notificacao-orcamento-email.md), [ADR 0019](../adr/0019-integracao-operacional-keycloak.md), [ADR 0020](../adr/0020-isolamento-dados-identidade.md), [ADR 0021](../adr/0021-politica-seguranca-identidade.md).

Banco único `oficina` com dois schemas e duas roles: `public` pertence à role `oficina_app` (app e
Flyway, sem superusuário) e `keycloak` pertence à role `keycloak` (tabelas criadas e migradas pelo
próprio Keycloak). O Postgres dedicado do Keycloak (`keycloak-db`) foi trocado por este banco único
em 2026-10-26 (ADR 0020).

O monólito é um único container (serviço `app` do compose); as caixas dentro dele são módulos
(pacotes por bounded context), mostrados para deixar claro quem fala com o Keycloak, com o banco e
com o MailPit.

```mermaid
flowchart TB
    cliente["<b>Cliente</b><br/>[Pessoa]<br/>Acompanha a OS e decide o orçamento"]
    equipe["<b>Atendente, Mecânico, Administrador</b><br/>[Pessoa]<br/>Equipe da oficina, perfis do realm oficina"]

    subgraph monolito["app: monólito Spring Boot 3.5 (Java 21), porta 8080"]
        api["<b>API REST</b><br/>[Módulo: Spring MVC + OAuth2 Resource Server]<br/>Controllers dos contextos e<br/>cadeia de segurança stateless"]
        identidade["<b>identidadeacesso</b><br/>[Módulo: Clean Architecture]<br/>AutenticarUsuarioAdministrativo valida o token:<br/>RS256, iss e aud=oficina-api; sem tabela de usuário"]
        atendimento["<b>atendimento</b><br/>[Módulo: Clean Architecture]<br/>Contexto core: OS, orçamento, cliente e veículo;<br/>notificação pelo NotificacaoPort"]
        catalogo["<b>catalogoestoque</b><br/>[Módulo: Clean Architecture]<br/>Serviços, peças, reserva e baixa de estoque"]
    end

        keycloak["<b>Keycloak 26.8.1-1</b><br/>[Sistema externo: OIDC]<br/>Realm oficina; emite o access token RS256;<br/>console em 127.0.0.1:8081"]
        mailpit["<b>MailPit</b><br/>[Sistema externo: SMTP local]<br/>SMTP 1025 e UI 127.0.0.1:8025,<br/>compartilhado pela app e pelo Keycloak"]
        subgraph banco["PostgreSQL 17: banco oficina"]
            schemaPublic[("<b>schema public</b><br/>[Banco: role oficina_app]<br/>Tabelas da oficina, migrations Flyway")]
            schemaKeycloak[("<b>schema keycloak</b><br/>[Banco: role keycloak]<br/>Tabelas do próprio Keycloak (Liquibase)")]
        end

    cliente -->|"consulta status e aprova orçamento<br/>HTTP, sem token"| api
    equipe -->|"chama a API<br/>HTTP, Bearer JWT"| api
    equipe -->|"obtém token<br/>password grant, OIDC"| keycloak
    api -->|"autentica o Bearer"| identidade
    api -->|"executa casos de uso"| atendimento
    api -->|"executa casos de uso"| catalogo
    atendimento -->|"consulta catálogo<br/>e reserva estoque"| catalogo
    identidade -->|"busca JWKS<br/>HTTP keycloak:8080"| keycloak
    atendimento -->|"JDBC como oficina_app"| schemaPublic
    catalogo -->|"JDBC como oficina_app"| schemaPublic
    atendimento -->|"envia e-mail de orçamento<br/>SMTP 1025"| mailpit
    keycloak -->|"JDBC como keycloak"| schemaKeycloak
    keycloak -->|"envia e-mail de conta<br/>SMTP 1025"| mailpit

    classDef pessoa fill:#08427b,stroke:#052e56,color:#ffffff
    classDef modulo fill:#85bbf0,stroke:#5d82a8,color:#000000
    classDef banco fill:#438dd5,stroke:#2e6295,color:#ffffff
    classDef externo fill:#999999,stroke:#6b6b6b,color:#ffffff
    class cliente,equipe pessoa
    class api,identidade,atendimento,catalogo modulo
    class schemaPublic,schemaKeycloak banco
    class keycloak,mailpit externo
    style monolito fill:none,stroke:#438dd5,stroke-width:3px
    style banco fill:none,stroke:#438dd5,stroke-width:2px
```

A coleta de métricas (Prometheus e Grafana, perfil `observability`) está na vista [Observabilidade](c4-container-observabilidade.md).

Legenda (cores do C4):

| Cor | Tipo de elemento |
|---|---|
| Azul escuro `#08427b` | Pessoa |
| Borda azul grossa | Container `app` (monólito Spring Boot) |
| Azul bem claro `#85bbf0` | Módulo do monólito (pacote por bounded context) |
| Azul `#438dd5`, cilindro | Schema do banco `oficina` |
| Cinza `#999999` | Sistema externo |
| Borda azul fina | Banco `oficina` (PostgreSQL 17) com os dois schemas |

Keycloak, MailPit e PostgreSQL são serviços do Docker Compose (perfis `dev` e `full`); a fronteira do
compose ficou fora do desenho porque empurrava esses serviços para a direita e dobrava a largura.
O job one-shot `postgres-init` (psql com `keycloak/postgres-init.sql`) cria as roles `oficina_app` e
`keycloak` e o schema `keycloak` antes da app e do Keycloak; ficou fora do desenho para não poluir e
está na tabela abaixo.

### Relações

| De | Para | Relação | Protocolo |
|---|---|---|---|
| Cliente | API REST | consulta a OS e aprova ou rejeita o orçamento (rotas públicas de `/api/acompanhamento`) | HTTP, sem token |
| Equipe | Keycloak | obtém o access token (client público `oficina-api`) | OIDC, password grant (demo local) |
| Equipe | API REST | chama as rotas administrativas | HTTP, Bearer JWT |
| API REST | identidadeacesso | o Resource Server delega a autenticação ao caso de uso `AutenticarUsuarioAdministrativo` | chamada em processo |
| API REST | atendimento, catalogoestoque | executa os casos de uso dos controllers | chamada em processo |
| atendimento | catalogoestoque | consulta serviços e peças e reserva estoque para a OS ([ADR 0014](../adr/0014-integracao-atendimento-catalogo-estoque.md)) | chamada em processo (adapters de integração) |
| identidadeacesso | Keycloak | busca as chaves públicas; o `iss` esperado é `http://localhost:8081/realms/oficina` | JWKS por HTTP interno, `keycloak:8080` |
| atendimento, catalogoestoque | schema `public` | lê e grava via JPA; o Flyway migra na subida | JDBC como `oficina_app` |
| atendimento | MailPit | envia o e-mail de orçamento pelo `NotificacaoPort` (#82, ADR 0017) | SMTP `mailpit:1025` |
| Keycloak | schema `keycloak` | persiste realm, contas e perfis; migra com o Liquibase dele | JDBC como `keycloak` |
| Keycloak | MailPit | envia e-mails de ação de conta | SMTP `mailpit:1025` |
| postgres-init | banco `oficina` | cria as roles `oficina_app` e `keycloak` e o schema `keycloak`, antes da app e do Keycloak | psql, job one-shot |

O `identidadeacesso` não acessa o banco: a identidade é montada por requisição a partir do token.
