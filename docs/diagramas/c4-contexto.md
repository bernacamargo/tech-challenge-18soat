# C4 — System Context (nível 1)

Sistema Integrado de Atendimento e Execução de Serviços da oficina.

Decisões: [ADR 0002](../adr/0002-postgres-for-everything.md), [ADR 0006](../adr/0006-keycloak-autenticacao-autorizacao.md), [ADR 0007](../adr/0007-notificacoes-smtp-local.md), [ADR 0017](../adr/0017-notificacao-orcamento-email.md), [ADR 0019](../adr/0019-integracao-operacional-keycloak.md), [ADR 0020](../adr/0020-isolamento-dados-identidade.md), [ADR 0021](../adr/0021-politica-seguranca-identidade.md).

O sistema da oficina aparece como uma caixa só; o que existe dentro dele está no
[C4 de Container](c4-container.md). O PostgreSQL aparece neste nível porque é compartilhado:
guarda os dados da oficina e, em schema próprio, os do Keycloak (ADR 0020).

```mermaid
flowchart TB
    cliente["<b>Cliente</b><br/>[Pessoa]<br/>Acompanha a OS e decide o orçamento"]

    subgraph equipe["Equipe da oficina (perfis do realm oficina)"]
        direction LR
        atendente["<b>Atendente</b><br/>[Pessoa]<br/>Cadastra cliente e veículo,<br/>abre a OS e envia o orçamento"]
        mecanico["<b>Mecânico</b><br/>[Pessoa]<br/>Diagnostica e executa os serviços"]
        admin["<b>Administrador</b><br/>[Pessoa]<br/>Mantém serviços, peças e estoque"]
        atendente ~~~ mecanico ~~~ admin
    end

    oficina["<b>Sistema Integrado da Oficina</b><br/>[Sistema de software]<br/>API REST de ordens de serviço,<br/>orçamento, catálogo e estoque"]

    subgraph dependencias["Dependências locais (Docker Compose)"]
        keycloak["<b>Keycloak</b><br/>[Sistema externo: provedor de identidade OIDC]<br/>Guarda as contas e emite o access token"]
        mailpit["<b>MailPit</b><br/>[Sistema externo: SMTP local]<br/>Captura os e-mails da app e do Keycloak"]
        postgres[("<b>PostgreSQL 17</b><br/>[Banco de dados]<br/>Banco único oficina,<br/>schemas public e keycloak")]
    end

    cliente -->|"consulta status e aprova orçamento<br/>(HTTP, sem token)"| oficina
    equipe -->|"obtém token<br/>(password grant, OIDC)"| keycloak
    equipe -->|"chama a API<br/>(HTTP, Bearer JWT)"| oficina
    oficina -->|"busca chaves públicas<br/>(JWKS, HTTP)"| keycloak
    oficina -->|"envia e-mail de orçamento<br/>(SMTP)"| mailpit
    oficina -->|"lê e grava<br/>(JDBC, role oficina_app)"| postgres
    keycloak -->|"envia e-mail de conta<br/>(SMTP)"| mailpit
    keycloak -->|"persiste contas<br/>(JDBC, role keycloak)"| postgres

    classDef pessoa fill:#08427b,stroke:#052e56,color:#ffffff
    classDef sistema fill:#1168bd,stroke:#0b4884,color:#ffffff
    classDef externo fill:#999999,stroke:#6b6b6b,color:#ffffff
    classDef banco fill:#438dd5,stroke:#2e6295,color:#ffffff
    class cliente,atendente,mecanico,admin pessoa
    class oficina sistema
    class keycloak,mailpit externo
    class postgres banco
    style equipe fill:none,stroke:#438dd5,stroke-dasharray:5 5
    style dependencias fill:none,stroke:#999999,stroke-dasharray:5 5
```

Legenda (cores do C4):

| Cor | Tipo de elemento |
|---|---|
| Azul escuro `#08427b` | Pessoa |
| Azul `#1168bd` | Sistema interno (o sistema da oficina) |
| Azul claro `#438dd5`, cilindro | Banco de dados |
| Cinza `#999999` | Sistema externo |
| Borda tracejada | Fronteira: equipe da oficina ou serviços do compose |

### Relações

| De | Para | Relação | Protocolo |
|---|---|---|---|
| Cliente | Oficina | consulta o status da OS e aprova ou rejeita o orçamento pelo identificador da OS | HTTP, sem token (rotas públicas de `/api/acompanhamento`) |
| Atendente, Mecânico, Administrador | Keycloak | obtém o access token no realm `oficina`, client público `oficina-api` | OIDC, password grant (demo local) |
| Atendente, Mecânico, Administrador | Oficina | chama as rotas administrativas | HTTP, `Authorization: Bearer <JWT>` |
| Oficina | Keycloak | busca as chaves públicas para validar a assinatura RS256 | JWKS por HTTP |
| Oficina | MailPit | envia o e-mail de orçamento ao cliente (`NotificacaoPort`, #82) | SMTP |
| Oficina | PostgreSQL | lê e grava os dados da oficina no schema `public` | JDBC, role `oficina_app` |
| Keycloak | MailPit | envia e-mails de ação de conta | SMTP |
| Keycloak | PostgreSQL | persiste o realm, as contas e os perfis no schema `keycloak` | JDBC, role `keycloak` |

Notas:

- Os perfis `ATENDENTE`, `MECANICO` e `ADMINISTRADOR` são realm roles do Keycloak; a oficina não
  tem tabela de usuário. Contas e perfis são mantidos no console do Keycloak (`127.0.0.1:8081`),
  com o admin do realm `master`.
- No ambiente local o MailPit só captura as mensagens (UI em `127.0.0.1:8025`); nada sai para a internet.
- A API é publicada em HTTP na porta `8080` pelo compose; não há TLS no ambiente local.
