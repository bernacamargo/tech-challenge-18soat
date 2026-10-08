# ADR-0006: Autenticação e autorização via Keycloak self-hosted

- **Status**: Aceito
- **Data**: 2026-10-02
- **Participantes**: Grupo 117

## Contexto

O [ADR-0004](0004-jwt-self-managed.md) definia autenticação JWT self-managed (usuários e emissão
de token pela própria aplicação). Requisitos do enunciado: JWT para as APIs administrativas,
credenciais de demonstração e procedimento de autenticação documentados. O grupo passou a
considerar que **autorização por papéis** (atendente, mecânico, administrador) e a gestão de
identidade (login, refresh, expiração) ficarão cada vez mais trabalhosos se implementados à mão,
e avaliou o uso do **Keycloak** (open source) em instância local para tornar autenticação e
autorização fáceis de implementar e evoluir.

## Decisão

Usar **Keycloak self-hosted** (docker-compose, instância local) como provedor de identidade:

- **Realm dedicado** `oficina`, com clientes, papéis (`ATENDENTE`, `MECANICO`, `ADMINISTRADOR`)
  e usuários de demonstração definidos por **realm export versionado no repositório**
  (config-as-code — reprodutível localmente e no CI).
- A API atua como **OAuth2 Resource Server**: valida os JWT emitidos pelo Keycloak via
  `spring-boot-starter-oauth2-resource-server` (JWKS), sem lógica de emissão de token na aplicação.
- **Autorização por papéis** mapeadas para authorities do Spring Security
  (`@PreAuthorize` / `hasRole` nos endpoints administrativos).
- O contexto `identidadeacesso` passa a conter a **integração** (config de segurança do contexto,
  mapeamento de roles) — não a gestão de credenciais.
- `keycloak` entra no `docker-compose.yml` (perfis `dev`/`full`) junto da fatia vertical de
  `identidadeacesso`, com o realm importado na inicialização.

## Consequências

- Positivas: login/logout/refresh e emissão de JWT prontos e padrão OIDC; autorização por papéis
  trivial e auditável; credenciais de demonstração fáceis de documentar (usuários do realm);
  self-hosted, gratuito e sem lock-in de cloud; preparado para SSO de outras ferramentas do grupo.
- Negativas: mais um serviço no ambiente (~0,5–1 GB de RAM); curva de configuração inicial
  (realm, clients, roles); o time precisa entender o fluxo OIDC.
- Alternativas descartadas: **JWT self-managed** ([ADR-0004](0004-jwt-self-managed.md) — reinvenção
  de gestão de identidade e papéis, sem SSO), **AWS Cognito** (dependência de cloud/conta externa,
  contrária à stack 100% dockerizada).
