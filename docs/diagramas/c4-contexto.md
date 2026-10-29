# C4 — System Context

Sistema Integrado de Atendimento e Execução de Serviços da oficina.

Decisões: [ADR 0006](../adr/0006-keycloak-autenticacao-autorizacao.md), [ADR 0019](../adr/0019-integracao-operacional-keycloak.md), [ADR 0020](../adr/0020-isolamento-dados-identidade.md), [ADR 0021](../adr/0021-politica-seguranca-identidade.md).

```mermaid
C4Context
    title System Context — Oficina (Grupo 117)

    Person(cliente, "Cliente", "Acompanha a OS e aprova orçamentos via API")
    Person(atendente, "Atendente", "Recebe cliente/veículo, cria OS, envia orçamento")
    Person(mecanico, "Mecânico", "Diagnostica, executa serviços, registra peças")
    Person(admin, "Administrador", "Administra catálogo e estoque; contas e perfis ficam no Keycloak")

    System(oficina, "Sistema Integrado da Oficina", "API REST — ordens de serviço, orçamentos, catálogo e estoque")
    System_Ext(keycloak, "Keycloak", "Provedor de identidade OIDC (realm oficina)")
    System_Ext(mailpit, "MailPit", "SMTP local da aplicação (orçamento) e do Keycloak")

    Rel(cliente, oficina, "Consulta status / aprova orçamento", "HTTPS")
    Rel(atendente, oficina, "Cria e gerencia OS", "HTTPS / JWT")
    Rel(mecanico, oficina, "Registra diagnóstico e execução", "HTTPS / JWT")
    Rel(admin, oficina, "CRUD de serviços, peças e estoque", "HTTPS / JWT")
    Rel(atendente, keycloak, "obtém token", "HTTP")
    Rel(mecanico, keycloak, "obtém token", "HTTP")
    Rel(admin, keycloak, "gerencia contas e perfis", "console 127.0.0.1:8081")
    Rel(oficina, keycloak, "valida token via JWKS", "HTTP")
    Rel(keycloak, mailpit, "envia e-mail", "SMTP")
    Rel(oficina, mailpit, "envia orçamento", "SMTP")

    SystemDb(pg, "PostgreSQL", "Banco único oficina: contextos da oficina (schema public) e Keycloak (schema keycloak)")
    Rel(oficina, pg, "Lê e escreve", "TCP 5432")
    Rel(keycloak, pg, "persiste contas", "JDBC / schema keycloak")
```
