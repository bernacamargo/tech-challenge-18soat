# ADR-0020: Persistência do Keycloak no Postgres da aplicação (schema e usuários segregados)

- **Status**: Proposto
- **Data**: 2026-10-29
- **Participantes**: Grupo 117 (decisão do time em 2026-10-26; proposta da ADR: Caio Crevelaro)

## Contexto

A [ADR-0002](0002-postgres-for-everything.md) (Aceita) definiu o PostgreSQL para todas as
necessidades de persistência. O Keycloak da [ADR-0006](0006-keycloak-autenticacao-autorizacao.md)
precisa de um banco próprio para o realm `oficina`, e a pergunta "onde o Keycloak guarda os
dados e por quê?" faz parte da entrega. Fatos que motivam a decisão:

- (a) **O Keycloak guarda dados sensíveis.** Os hashes de senha dos usuários ficam na tabela
  `credential`, e as chaves privadas de assinatura dos tokens ficam nas tabelas de componentes
  do realm (`component`/`component_config`). Quem lê essas tabelas consegue atacar as senhas
  offline ou forjar tokens aceitos pela API (modelo de dados do Keycloak `26.8.1-1`; a tabela
  `keycloak.credential` foi alcançada na verificação ponta a ponta só por `count(*)`, sem ler
  conteúdo).
- (b) **A aplicação conectava como superusuário.** No `docker-compose.yml` anterior a esta fatia,
  a aplicação usava a role `oficina`, que a imagem oficial do PostgreSQL cria como
  **superusuário** a partir de `POSTGRES_USER`. Um superusuário ignora qualquer permissão de
  schema; por isso, na primeira versão desta fatia (T15), o Keycloak ganhou um segundo
  PostgreSQL só para ele (`keycloak-db`, volume próprio).
- (c) **O time trocou o desenho em 2026-10-26.** A decisão D10 do plano da fatia (Postgres
  dedicado do Keycloak, T15) foi substituída por um **banco único com schemas e roles
  segregados**, decisão do time em 2026-10-26. A troca só é segura porque corrige junto o fato
  (b): a aplicação passa à role `oficina_app`, sem superusuário. Medido em 2026-10-29 no
  `pg_roles` (coluna `rolsuper`): `keycloak false`, `oficina true`, `oficina_app false`.
- (d) **A ADR-0002 trata da persistência como um todo.** Com o Keycloak no mesmo PostgreSQL, ela
  passa a valer também para os dados de identidade, sem exceção a registrar.
- (e) **Prática de mercado.** O fórum oficial e a documentação do Keycloak recomendam um banco
  ou schema próprio com usuário restrito; o mesmo servidor com schemas separados é aceito; o
  banco embutido `dev-file` serve só para desenvolvimento e não para produção
  ([Keycloak — Configuring the database](https://www.keycloak.org/server/db)); a aplicação guarda
  só o `sub` do usuário, nunca a senha. O risco citado é o mesmo do fato (b): um superusuário da
  aplicação enxergaria os dados de autenticação.

### Por que o Keycloak tem mais de 100 tabelas

- (a) O Keycloak **cria e migra sozinho** todo o próprio modelo de dados na primeira subida, via
  Liquibase: 101 tabelas e 237 changesets na versão `26.8.1-1`. O projeto não escreve, não
  versiona nem migra essas tabelas, e elas **não** entram no Flyway da aplicação.
- (b) O modelo é **fixo e completo**: não há como escolher um subconjunto de tabelas. O projeto
  controla só **onde** elas ficam (`KC_DB_SCHEMA=keycloak`), **quem** é o dono (role `keycloak`)
  e **quando** a migração roda (opção `migration-strategy` do provedor de conexões JPA do
  Keycloak, padrão `update`).
- (c) Só parte das tabelas é usada. Contagem por função, medida em 2026-10-26 no banco do
  Keycloak com o realm `oficina` importado (`pg_stat_user_tables`, só nomes e contagens de
  linhas, nenhum dado):

| Função | Tabelas | Com dados | Usamos |
|---|---|---|---|
| Federação LDAP/Active Directory | 14 | 0 | não |
| Credenciais verificáveis | 2 | 0 | não |
| Login social (Google, GitHub...) | 7 | 0 | não |
| Organizações (multiempresa) | 3 | 0 | não |
| Autorização fina por recurso (UMA) | 12 | 0 | não |
| Sessões, força bruta, eventos | 9 | 0 | sim (enche em uso) |
| Fluxos de login | 6 | 5 | sim |
| Clients, escopos e claims do token | 15 | 12 | sim |
| Perfis (roles) e grupos | 10 | 3 | sim |
| Usuários e senhas | 6 | 3 | sim |
| Configuração do realm e chaves de assinatura | 12 | 10 | sim |
| Cluster e controle de versão (Liquibase) | 5 | 2 | interno |
| **Total** | **101** | **35** | |

  Cerca de 38 tabelas são de recursos que a oficina não liga (LDAP/AD, login social,
  autorização fina UMA, organizações, credenciais verificáveis) e ficam vazias:
  são tabelas não usadas, mas obrigatórias no modelo do produto.
- (d) Por isso "mais de 100 tabelas" não é complexidade do projeto: o modelo da oficina continua
  com as tabelas de negócio no schema `public`, e o schema `keycloak` é dado de um produto de
  terceiros, isolado por permissão.
- (e) Os números valem para a versão citada (`26.8.1-1`) e mudam com as atualizações do Keycloak.

## Decisão

Persistir o Keycloak **no PostgreSQL da aplicação**, num schema próprio e com usuários do banco
segregados. É a aplicação da [ADR-0002](0002-postgres-for-everything.md) **sem exceção**:

- **Um banco só**, `oficina`, no serviço `postgres` (`postgres:17-alpine`). As tabelas da oficina
  ficam no schema `public`; as do Keycloak (cerca de 101, criadas e migradas por ele) ficam no
  schema `keycloak` (`KC_DB_SCHEMA=keycloak`).
- **Três roles**: `oficina` (superusuário da imagem, só administração e init; a aplicação não o
  usa), `oficina_app` (sem superusuário, dona do schema `public`, usuária da aplicação e do
  Flyway) e `keycloak` (sem superusuário, dona do schema `keycloak`). O script aplica
  `REVOKE ALL ON SCHEMA public FROM PUBLIC`, para que nenhuma role alcance o `public` por padrão.
- **Usuários e permissões em script versionado**: `keycloak/postgres-init.sql`, executado pelo
  serviço one-shot `postgres-init` a cada subida, antes do Keycloak e da aplicação. É idempotente
  (cria se não existe, reafirma atributos, passa a posse das tabelas antigas do `public` para
  `oficina_app`) e lê as senhas das variáveis `APP_DB_PASSWORD` e `KEYCLOAK_DB_PASSWORD`, com
  default local documentado no [README](../../README.md); nenhuma senha fica no script.
- **Migrations**: as migrations Flyway da oficina (`V1` a `V10` na data desta ADR, incluindo as que vieram da `main`) não mudam e passam a rodar como
  `oficina_app`. As migrations do Keycloak (Liquibase) continuam sendo dele; nada é copiado
  para o Flyway.
- **Compose**: o serviço `keycloak` recebe `KC_DB=postgres`,
  `KC_DB_URL=jdbc:postgresql://postgres:5432/oficina` e `KC_DB_USERNAME=keycloak`; o `postgres`
  é publicado só em `127.0.0.1:5432`; a aplicação recebe `DB_USERNAME=oficina_app` e **nenhuma**
  variável `KC_*`/`KEYCLOAK_DB*`; `postgres`, `postgres-init` e `keycloak` sobem nos perfis de
  execução `[dev, full]`.
- **Configuração da aplicação**: o `application.yml` usa `oficina_app` como usuário padrão do
  datasource.
- **Reset do realm**: mudou o JSON do realm, apaga-se só o schema do Keycloak
  (`docker compose stop keycloak`, depois `DROP SCHEMA keycloak CASCADE` pelo `psql -U oficina`,
  depois `docker compose up -d --wait postgres-init keycloak`), sem `down -v` nem `volume rm` do
  `pgdata` (passo a passo no [README](../../README.md)).

Débito de menor privilégio **resolvido nesta fatia** (T15c): a aplicação conecta como
`oficina_app`, sem superusuário. Nenhuma issue de débito é aberta para isso.

Como verificar (resultados da T15c e da verificação ponta a ponta, sem nenhum valor sensível):

- `0` tabelas do Keycloak no schema `public` (`information_schema`), e 101 no schema `keycloak`;
- `keycloak.user_entity` com 3 usuários do realm `oficina` (só `count(*)`);
- a role `oficina_app` recebe `permission denied for schema keycloak` ao contar
  `keycloak.credential`;
- a role `keycloak` recebe `permission denied for schema public` ao contar `public.cliente`;
- nenhuma variável `KC_*`/`KEYCLOAK_DB*` no container `app`, e as conexões da aplicação são
  todas como `oficina_app`;
- `rolsuper` das três roles: `keycloak false`, `oficina true`, `oficina_app false`.

## Consequências

- Positivas:
  - a ADR-0002 é seguida sem exceção: um só motor e um só servidor de banco;
  - um container a menos e menos RAM (o `postgres` único usou cerca de 68 MiB na verificação
    ponta a ponta);
  - um backup só (o volume `pgdata`), que agora inclui o schema `keycloak`;
  - a aplicação deixa de ser superusuária: menor privilégio (CIS PostgreSQL Benchmark; ISO/IEC
    27002, controle de acesso privilegiado), com isolamento nos dois sentidos entre `oficina_app`
    e `keycloak`; a aplicação não alcança os hashes de senha nem as chaves de assinatura.
- Negativas / trade-offs aceitos:
  - mais uma role e mais uma senha local para documentar;
  - um script de init a manter: uma migration futura que crie objeto que não seja tabela nem
    sequência exige revisar a passagem de posse do script;
  - o superusuário `oficina` continua enxergando tudo; fica só para administração e init, a
    aplicação não o usa e o `postgres` só escuta em `127.0.0.1`;
  - o reset do realm passa a ser um `DROP SCHEMA` em vez de remover um volume.
- Alternativas consideradas e por que foram descartadas:
  - **(a) Postgres dedicado para o Keycloak** (desenho da T15, revertido pelo time): exigia uma exceção à ADR-0002, um container e um backup a mais, e não corrigia o superusuário da aplicação no banco da oficina.
  - **(b) Tudo no schema `public`**: num banco novo, se o Keycloak sobe antes da aplicação, o Flyway encontra o `public` cheio e sem o histórico dele e recusa migrar (`non-empty schema without schema history table`); além disso, o `\dt` da oficina ficaria com cerca de 110 tabelas misturadas.
  - **(c) H2 `dev-file`** (padrão do `start-dev`): a documentação oficial o restringe a desenvolvimento; fica fora do motor da ADR-0002 e não permite verificar a segregação por SQL.
  - **(d) Sem persistência (`dev-mem`)**: bloqueios de força bruta, eventos de login e alterações do realm somem a cada reinício, o que contradiz a retenção de eventos da [ADR-0021](0021-politica-seguranca-identidade.md).
  - **(e) Tirar o Keycloak e guardar a identidade na aplicação**: contraria a ADR-0006 e traria de volta tabela de usuário e senha na aplicação.

## Referências

- [ADR-0002](0002-postgres-for-everything.md), [ADR-0006](0006-keycloak-autenticacao-autorizacao.md),
  [ADR-0019](0019-integracao-operacional-keycloak.md) (integração operacional com o Keycloak),
  [ADR-0021](0021-politica-seguranca-identidade.md) (política de segurança de identidade).
- [README](../../README.md): credenciais de demonstração (somente local), reset do realm.
- `docker-compose.yml` (serviços `postgres`, `postgres-init`, `keycloak`, `app`) e
  `keycloak/postgres-init.sql`.
- [Keycloak — Configuring the database](https://www.keycloak.org/server/db); CIS PostgreSQL
  Benchmark; ISO/IEC 27002.
