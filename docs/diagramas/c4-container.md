# C4 — Container

Decisões: [ADR 0006](../adr/0006-keycloak-autenticacao-autorizacao.md), [ADR 0019](../adr/0019-integracao-operacional-keycloak.md), [ADR 0020](../adr/0020-isolamento-dados-identidade.md), [ADR 0021](../adr/0021-politica-seguranca-identidade.md).

Banco único `oficina` ([ADR 0002](../adr/0002-postgres-for-everything.md)), com dois schemas e duas roles:
o schema `public` guarda os contextos da oficina e pertence à role `oficina_app` (app e Flyway, sem
superusuário); o schema `keycloak` guarda os dados do Keycloak e pertence à role `keycloak`.
Nota: o Postgres dedicado do Keycloak (`keycloak-db`) foi trocado por este banco único em 2026-10-26,
por decisão do time (ADR 0020). O MailPit é o SMTP local compartilhado pela aplicação (notificação
de orçamento, [ADR 0017](../adr/0017-notificacao-orcamento-email.md)) e pelo Keycloak (e-mails de ação de conta).

```mermaid
C4Container
    title Containers — Oficina (Grupo 117)

    Person(cliente, "Cliente")
    Person(equipe, "Atendente / Mecânico / Admin")

    System_Boundary(monolito, "oficina (monólito Spring Boot 3.5 — Java 21)") {
        Container(api, "API REST", "Spring MVC + OAuth2 Resource Server", "Endpoints de OS, orçamento, catálogo e estoque")
        Container(atendimento, "atendimento", "Clean Architecture", "Contexto core: OS, orçamento, status")
        Container(catalogo, "catalogoestoque", "Clean Architecture", "Serviços, peças, reserva de estoque")
        Container(identidade, "identidadeacesso", "Clean Architecture", "Identidade por requisição: valida o access token (port + caso de uso + adapter); sem cadastro de usuários")
        ContainerDb(db, "PostgreSQL 17", "Flyway + Liquibase do Keycloak", "Banco único oficina (ADR 0002): schema public com os contextos da oficina (role oficina_app) e schema keycloak com os dados do Keycloak (role keycloak, ADR 0020); 127.0.0.1:5432")
    }

    System_Boundary(idp, "Keycloak — realm oficina (produto de terceiros)") {
        Container(keycloak, "Keycloak", "quay.io/keycloak/keycloak:26.8.1-1", "OIDC: emite access tokens RS256; console em 127.0.0.1:8081")
    }

    System_Ext(mailpit, "MailPit", "SMTP local 127.0.0.1:1025 + UI 127.0.0.1:8025 (app e Keycloak)")

    Rel(cliente, api, "Consulta status / aprova orçamento", "HTTPS")
    Rel(equipe, keycloak, "obtém token", "HTTP / password grant (demo)")
    Rel(equipe, api, "Operações administrativas", "HTTPS / Bearer JWT")
    Rel(api, atendimento, "usa casos de uso")
    Rel(api, catalogo, "usa casos de uso")
    Rel(api, identidade, "autentica")
    Rel(api, keycloak, "busca JWKS", "HTTP")
    Rel(atendimento, db, "JPA")
    Rel(catalogo, db, "JPA")
    Rel(keycloak, db, "JDBC", "schema keycloak, role keycloak")
    Rel(keycloak, mailpit, "SMTP")
    Rel(atendimento, mailpit, "envia orçamento", "SMTP")
```
