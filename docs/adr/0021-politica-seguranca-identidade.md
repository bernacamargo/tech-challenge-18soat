# ADR-0021: Política de segurança de identidade

- **Status**: Proposto
- **Data**: 2026-10-29
- **Participantes**: Grupo 117 (proposta: Caio Crevelaro)

## Contexto

A [ADR-0006](0006-keycloak-autenticacao-autorizacao.md) escolheu o Keycloak; a
[ADR-0019](0019-integracao-operacional-keycloak.md) registrou como a API valida o token e
responde 401/403/5xx; a [ADR-0020](0020-isolamento-dados-identidade.md) registrou onde o Keycloak
guarda os dados. O grupo decidiu adotar **todas** as configurações de segurança da identidade
(força bruta, política de senha conforme o NIST, hash padrão do Keycloak, eventos com retenção,
minimização de dados pessoais, RS256, issuer e audience obrigatórios, 401/403 sem detalhe,
consoles só em `127.0.0.1`). Esses controles ficaram espalhados no realm
(`keycloak/realm-oficina.json`), no `docker-compose.yml`, no `application.yml` e no código.

O enunciado pede que o repositório não traga credenciais, senhas, tokens nem informações
sensíveis, e que as decisões arquiteturais fiquem documentadas. Esta ADR junta os controles
numa tabela única, com o valor efetivo lido do repositório (ou observado na verificação ponta a
ponta), a referência normativa e o comando ou teste que prova cada um, para que a política seja
revisada e aceita pelo grupo de uma vez.

## Decisão

Adotar a política abaixo como a política de segurança da identidade. Os IDs `SEG-nn` são os
controles de segurança da identidade desta fatia (catálogo único, SEG-01 a SEG-32). "T25 E*n*"
é o item correspondente do roteiro de verificação ponta a ponta executado em 2026-10-29.

| ID | Controle | Valor efetivo | Referência | Como verificar |
|---|---|---|---|---|
| SEG-01 | Proteção contra força bruta, sem lockout permanente | `bruteForceProtected=true`, `failureFactor=5`, `waitIncrementSeconds=60`, `maxFailureWaitSeconds=900`, `maxDeltaTimeSeconds=43200`, `quickLoginCheckMilliSeconds=1000`, `minimumQuickLoginWaitSeconds=60`, `permanentLockout=false`, `maxTemporaryLockouts=0` | NIST SP 800-63B (limitação de tentativas); OWASP ASVS 4.0.3 V2 | T25 E19 (5 falhas, senha certa recusada, `attack-detection` com `disabled:true`, `DELETE` 204, login volta); `RealmOficinaConsistenciaTest#hardeningD12b` |
| SEG-02 | Política de senha: mínimo de 8, diferente do username, sem composição | `passwordPolicy=length(8) and notUsername` | NIST SP 800-63B (memorized secrets); ASVS V2.1 | T25 E14 (`reset-password` com senha curta e com o username: 400 e 400); `RealmOficinaConsistenciaTest#usuariosDeDemoCumpremPoliticaDeSenha` |
| SEG-03 | Hash de senha padrão do Keycloak 26, sem rebaixar | `algorithm=argon2`, `hashIterations=5` (observado; padrão mantido (D12b)) | OWASP Password Storage Cheat Sheet | T25 E13 (só os parâmetros de `credential_data`, nunca o hash) |
| SEG-04 | Senhas de demo só no import, conformes, `temporary=false`; nenhum `hashedSaltedValue` versionado | `type=password`, `temporary=false` nos 3 usuários; senha em texto só no JSON de import (o Keycloak grava o hash); nenhum campo de hash no realm | `m_tech.txt:102-103`; OWASP ASVS V2 | `RealmOficinaConsistenciaTest#usuariosDeDemoCumpremPoliticaDeSenha`; `RealmOficinaConsistenciaTest#naoVersionaSegredos` |
| SEG-05 | Eventos `LOGIN`/`LOGIN_ERROR` com retenção de 7 dias; admin events desligados | `eventsEnabled=true`, `enabledEventTypes=LOGIN,LOGIN_ERROR`, `eventsExpiration=604800`, `adminEventsEnabled=false`, `adminEventsDetailsEnabled=false` | OWASP ASVS V7 (log); LGPD (retenção mínima) | T25 E15 (5 `LOGIN_ERROR` e 5 `LOGIN`, nenhuma senha no conteúdo); `RealmOficinaConsistenciaTest#hardeningD12b` |
| SEG-06 | Minimização do access token: `sub`, `preferred_username`, perfis (`realm_access.roles`); sem `email`/`name` | client `oficina-api`: `defaultClientScopes=basic`, `optionalClientScopes=[]`, `fullScopeAllowed=false`, mappers `audience-oficina-api`, `username`, `realm-roles` só no access token | LGPD art. 6º, III (necessidade) | T25 E5 (claims sem `email`/`name`); `RealmOficinaConsistenciaTest#tokenMantemSubEPreferredUsername` |
| SEG-07 | Vida curta do access token (`900` s); `exp` obrigatório se T19 | `accessTokenLifespan=900`; `exp` obrigatório em `JwtValidacaoConfig` (T19 adotado) | OWASP ASVS V3 (sessão); RFC 9700 | T25 E5 (`exp-iat=900`); `ValidacaoTokenJwtIntegrationTest#tokenSemExpiracaoDa401` |
| SEG-08 | Algoritmo fixo RS256; HS256 e `alg=none` recusados; HMAC fora de `src/main` | `jws-algorithms=RS256`, `defaultSignatureAlgorithm=RS256` | RFC 8725 (JWT BCP) | `ValidacaoTokenJwtIntegrationTest#tokenInvalidoDa401`; `JwtTestSupportTest#tokenHs256NaoVerificaComJwks`; `grep -rnE 'Hmac\|HS256\|MacAlgorithm\|SecretKeySpec\|jwt-secret' src/main` → exit=1 |
| SEG-09 | Issuer exato e audience obrigatória (`oficina-api`); tokens do realm `master` e de outro client recusados | `issuer-uri=http://localhost:8081/realms/oficina` (padrão e perfil `full`), `audiences=oficina-api` | RFC 8725; RFC 6750 | T25 E9 (`admin-cli` → 401), T25 E10 (`master` → 401); `RealmOficinaConsistenciaTest#audienceDoMapperIgualAoAudiencesDoApplicationYml` |
| SEG-10 | Consoles administrativos, banco e SMTP só em `127.0.0.1` (Keycloak `8081`, MailPit UI `8025` e SMTP `1025`, `postgres` `5432`); `9000` sem porta publicada; MailPit compartilhado pela app e pelo Keycloak | `keycloak=127.0.0.1:8081`, `mailpit=127.0.0.1:1025,127.0.0.1:8025`, `postgres=127.0.0.1:5432`; `9000` não publicada | OWASP ASVS V14 (configuração); CIS Docker (exposição de portas) | T25 E4; `docker compose --profile full config --format json` com `jq` das portas |
| SEG-11 | `sslRequired=external`; app e testes nunca usam o realm `master` | `sslRequired=external` | OWASP ASVS V9 (comunicação) | `jq -r .sslRequired keycloak/realm-oficina.json` → `external`; T25 E10 |
| SEG-12 | 401 e 403 sem detalhe: `error_description="Token invalido"` fixo e corpo vazio | `error_description=Token invalido` fixo, corpo vazio (0 bytes) | RFC 6750 §3; OWASP ASVS V7.4 (tratamento de erro) | T25 E7; `ValidacaoTokenJwtIntegrationTest#respostaDe401NaoDetalhaOMotivo`; `SegurancaTransversalIntegrationTest#respostaDe403NaoDetalhaPerfilExigido` |
| SEG-13 | Negar por padrão: só perfis da oficina viram authority; piso `hasAnyRole`; `@EnableMethodSecurity` único | `Perfil.reconhecer` aceita só `ATENDENTE`, `MECANICO`, `ADMINISTRADOR`; `anyRequest().hasAnyRole("ATENDENTE", "MECANICO", "ADMINISTRADOR")`; um `@EnableMethodSecurity` (`SecurityConfig`) | OWASP ASVS V4.1 (controle de acesso) | `AutorizacaoPorPerfilIntegrationTest#respeitaAMatrizDeAutorizacao`; `AutenticacaoIntegrationTest#somenteRolesTecnicosDa403EmRotasAdministrativas`; T25 E6 |
| SEG-14 | Token, header `Authorization` e senha fora de logs, evidências, commits e `toString` | `Credencial.toString` mascarado; adapter e manager sem log | OWASP ASVS V7.1; `m_tech.txt:102-103` | T25 E17 (0 em tudo); `SegurancaTransversalIntegrationTest#tokenNaoApareceNoLog`; `CredencialTest#toStringNaoExpoeOValor` |
| SEG-15 | Nenhum segredo versionado: realm sem `keys`/`secret`/`hashedSaltedValue`, chaves RSA de teste geradas em runtime | realm sem chaves, sem segredo de client e sem campo de hash; chaves de teste geradas por execução | `m_tech.txt:102-103`; OWASP ASVS V6.4 | `RealmOficinaConsistenciaTest#naoVersionaSegredos`; `JwtTestSupportTest#jwksPublicaApenasChavePublica` |
| SEG-16 | Segregação da identidade em schema `keycloak` com role própria, no banco único `oficina`: isolamento nos dois sentidos entre as roles `oficina_app` e `keycloak`; o superusuário `oficina` fica só para administração e init | `KC_DB_SCHEMA=keycloak`, `KC_DB_USERNAME=keycloak`; app `DB_USERNAME=oficina_app` | CIS PostgreSQL Benchmark; ISO/IEC 27002 (menor privilégio) | T25 E12 (`a=0 b=3 c=1 d=1`); T15c C17–C21; [ADR-0020](0020-isolamento-dados-identidade.md) |
| SEG-17 | Password grant habilitado só para demonstração; client público sem client secret; `standardFlowEnabled=false` | `publicClient=true`, `directAccessGrantsEnabled=true`, `standardFlowEnabled=false` | RFC 9700 (desaconselha ROPC) | `RealmOficinaConsistenciaTest#clienteOficinaApiPublicoComPasswordGrantSemSegredo` |
| SEG-18 | Superfície mínima do realm: registro, reset de senha, remember-me, login por e-mail e edição de username desligados | `registrationAllowed=false`, `resetPasswordAllowed=false`, `rememberMe=false`, `loginWithEmailAllowed=false`, `editUsernameAllowed=false`, `duplicateEmailsAllowed=false` | OWASP ASVS V1/V14 (redução de superfície) | `jq '{registrationAllowed,resetPasswordAllowed,rememberMe,loginWithEmailAllowed,editUsernameAllowed,duplicateEmailsAllowed}' keycloak/realm-oficina.json` → todos `false` |
| SEG-19 | Admin de bootstrap por variável com default local; troca só com reset | `KC_BOOTSTRAP_ADMIN_USERNAME` e `KC_BOOTSTRAP_ADMIN_PASSWORD` lidas do ambiente, com default local documentado no README (valor não repetido aqui) | OWASP ASVS V2.10 (credenciais de serviço) | `docker compose --profile full config --format json` com `jq` das chaves `BOOTSTRAP` do `keycloak` |
| SEG-20 | A app não depende do Keycloak para subir (JWKS lazy, sem `depends_on`) e não usa o realm `master` | `app.depends_on=postgres,postgres-init` (sem `keycloak`) | disponibilidade | `jq -e '.services.app.depends_on.keycloak == null'` sobre o `docker compose config` → `true` |
| SEG-21 | A app não tem variáveis `KC_*`/`KEYCLOAK_DB*` nem credenciais da role `keycloak` | nenhuma chave `KC_*`/`KEYCLOAK_DB*` em `app.environment` | menor privilégio | T25 E12 (`h=0`); T15c C11 |
| SEG-22 | Imagens com tag fixa (sem `:latest`) | `keycloak:26.8.1-1`, `mailpit:v1.31.6`, `postgres:17-alpine`, `grafana:13.2.4`, `prometheus:v3.15.1` | supply chain (OWASP Top 10 A06/A08) | `docker compose --profile full config` sem `image: ...:latest` (grep → exit=1) |
| SEG-23 | Swagger sem `persistAuthorization` e sem senha na descrição; sem fluxo `oauth2`/PKCE | `persistAuthorization=false`; esquema `bearer-jwt` com `type=http`, `scheme=bearer`, `bearerFormat=JWT` | OWASP ASVS V3 (token no navegador) | T25 E18; `OpenApiSegurancaIntegrationTest#swaggerUiNaoPersisteAutorizacao`; `OpenApiSegurancaIntegrationTest#descricaoNaoTemSenhaNemScopeOpenid` |
| SEG-24 | ID token recusado como credencial (`typ` = `Bearer`), se T19 | `typ=Bearer` exigido em `JwtValidacaoConfig` (T19 adotado) | RFC 8725 (confusão de tipos de token) | T25 E8; `ValidacaoTokenJwtIntegrationTest#idTokenDoKeycloakDa401` |
| SEG-25 | Comportamentos aceitos: Bearer inválido em rota pública → 401; Keycloak fora do ar → 5xx (falha fechada) | Bearer inválido em rota pública = 401; JWKS indisponível = `AuthenticationServiceException` (5xx) | RFC 6750 | T25 E11 (404 sem Bearer, 401 com Bearer inválido); `AutenticacaoIntegrationTest#bearerInvalidoEmRotaPublicaDa401`; `UsuarioAdministrativoAuthenticationManagerTest#falhaTecnicaDoProvedorViraAuthenticationServiceException` |
| SEG-26 | Desvio conhecido: `app` publicada em `0.0.0.0:8080`. Corrigidos: `postgres` só em `127.0.0.1:5432` e app como `oficina_app`, sem superusuário | `app=0.0.0.0:8080` (desvio); `postgres=127.0.0.1:5432`; `DB_USERNAME=oficina_app` | CIS Docker; CIS PostgreSQL | T25 E4; T15c C5, C11, C19 |
| SEG-27 | `Credencial.toString()` mascarado; exceções e mensagens nunca contêm o token | `Credencial.toString` mascarado; mensagem da `CredencialInvalidaException` sem o token | OWASP ASVS V7.1 (log sem dado sensível) | `CredencialTest#toStringNaoExpoeOValor`; `CredencialTest#mensagemDaExcecaoNaoContemOValor` |
| SEG-28 | Chaves e JWKS de teste: geradas em runtime, JWKS só com a parte pública, servidor só em loopback e porta efêmera; nenhum PEM/JWT estático versionado | JWKS de teste sem a parte privada; `JwksTestServer` em `127.0.0.1` com porta efêmera | RFC 7517; OWASP ASVS V6.4 | `JwtTestSupportTest#jwksPublicaApenasChavePublica`; `JwtTestSupportTest#servidorEscutaSoEmLoopbackEmPortaEfemera` |
| SEG-29 | `audiences` e `jws-algorithms` fixos no `application.yml`, sem variável de ambiente que os afrouxe | `audiences=oficina-api`, `jws-algorithms=RS256` (literais, sem `${`) | RFC 8725 | `grep -nE '^ *(audiences\|jws-algorithms):' src/main/resources/application.yml` sem `${` |
| SEG-30 | A app não cria tabela de usuário/senha: credencial só no Keycloak | nenhuma migration Flyway nova nesta fatia | OWASP Password Storage Cheat Sheet; LGPD (minimização) | `git diff --name-only origin/main...HEAD -- src/main/resources/db/migration` → vazio; T25 E12 |
| SEG-31 | Role `keycloak` do Postgres sem privilégios administrativos, dona só do schema `keycloak` e sem acesso ao `public` | `NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS`; `rolsuper=false` | CIS PostgreSQL Benchmark; ISO/IEC 27002 (acesso privilegiado) | T25 E12 (`c=1`, `e=0`); T15c C19, C21 |
| SEG-32 | Role `oficina_app` (app e Flyway) sem superusuário nem outro privilégio administrativo e sem acesso ao schema `keycloak`; dona do schema `public` | `NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS`; `rolsuper=false`; dona do `public` | CIS PostgreSQL Benchmark; ISO/IEC 27002 (menor privilégio) | T25 E12 (`d=1`, `e=0`, `g=t`); T15c C19, C20 |

Detalhes que a tabela resume:

- **Força bruta (SEG-01).** Depois de 5 falhas, a conta fica temporariamente bloqueada; a espera
  cresce 60 s a cada bloqueio, até 900 s, e o contador zera depois de 12 h sem falhas. O
  *quick login* bloqueia por 60 s quem erra duas vezes em menos de 1 s (robôs). Não há lockout
  permanente (`permanentLockout=false`, `maxTemporaryLockouts=0`): a conta volta sozinha. O
  desbloqueio antecipado é feito pela API administrativa do Keycloak
  (`attack-detection/brute-force/users/{id}`) ou pelo console, como descrito no
  [README](../../README.md), seção "Credenciais de demonstração (somente local)".
- **Política de senha (SEG-02).** Comprimento mínimo de 8 e diferente do username, **sem regra
  de composição** (maiúscula, número, símbolo) e **sem expiração periódica**, como recomenda o
  NIST SP 800-63B. A política não é aplicada pelo Keycloak às senhas que chegam pelo import do
  realm; por isso as senhas de demonstração já nascem conformes, o que o teste
  `RealmOficinaConsistenciaTest#usuariosDeDemoCumpremPoliticaDeSenha` e o T25 E14 conferem.
- **Hash (SEG-03).** O algoritmo é o observado no banco (T25 E13): `argon2` com 5 iterações,
  que é o padrão do Keycloak 26; nenhuma configuração o altera.
- **Minimização (SEG-06, LGPD art. 6º, III).** O access token leva só `sub`,
  `preferred_username`, `realm_access.roles` e as claims de protocolo (`iss`, `aud`, `azp`,
  `typ`, `exp`, `iat`); não leva `email` nem `name`. Os e-mails de demonstração
  (`@oficina.local`) ficam só no realm. Os eventos guardados são só `LOGIN` e `LOGIN_ERROR`,
  retidos por 7 dias (`604800` s); admin events ficam desligados.

## Consequências

- Positivas:
  - a segurança da identidade fica rastreável num documento só: cada controle tem valor
    efetivo, referência e prova reproduzível;
  - os valores do realm são protegidos por teste (`RealmOficinaConsistenciaTest`), e a tabela
    pode ser reconferida pelos comandos da última coluna;
  - nenhuma configuração rebaixa o padrão do Keycloak (hash, algoritmo de assinatura);
  - a minimização e a retenção curta reduzem o dado pessoal guardado.
- Negativas / trade-offs aceitos (desvios conhecidos e riscos residuais, fora desta fatia):
  - a `app` continua publicada em `0.0.0.0:8080` (SEG-26); o `postgres` já está só em `127.0.0.1:5432`;
  - o superusuário `oficina` do Postgres é mantido só para administração e init; a app usa a role `oficina_app`, sem superusuário ([ADR-0020](0020-isolamento-dados-identidade.md); débito resolvido na fatia, sem issue);
  - o Keycloak roda em `start-dev`: sem TLS e sem o cache de produção; `sslRequired=external` não exige HTTPS em `localhost`;
  - o password grant está ativo (só para a demonstração com curl e Swagger);
  - as senhas de demonstração são públicas no README (somente local);
  - um funcionário com perfil da oficina, mas sem o perfil exigido pelo `@PreAuthorize`, recebe 400 com erros de campo quando o corpo da requisição é inválido, porque a validação do corpo (`@Valid`) roda antes do `@PreAuthorize`; com corpo válido recebe 403; a regra de URL que nega antes da validação cobre só clientes e veículos;
  - cada token com `kid` desconhecido força uma nova busca do JWKS no Keycloak (amplificação por token forjado; o decoder do Spring não limita a taxa de busca); risco aceito para o ambiente local;
  - o client `admin-cli` também emite token no realm `oficina` pelo password grant, mas a API recusa esse token por `aud` (T25 E9: `aud` ausente, 401);
  - a recusa do token do realm `master` (T25 E10) é defesa em profundidade: o token cai também por `aud` e pela assinatura; a prova isolada de `iss` é o teste de integração do decoder;
  - `exp` obrigatório e `typ=Bearer` foram adotados (SEG-07, SEG-24); não há risco residual de ID token ou de token sem expiração aceito;
  - a varredura de vulnerabilidades (`trivy` ou relatório equivalente) das imagens novas do Keycloak e do MailPit está pendente;
  - o MailPit é compartilhado entre o Keycloak e a app, com SMTP `1025` sem autenticação, publicado só em `127.0.0.1` (SEG-10).
- Alternativas consideradas e por que foram descartadas:
  - **(a) Regra de composição e expiração periódica de senha**: o NIST SP 800-63B as desaconselha, porque levam a senhas previsíveis e reaproveitadas.
  - **(b) Lockout permanente**: vira negação de serviço contra qualquer conta conhecida e travaria a demonstração; o bloqueio temporário com espera crescente basta.
  - **(c) Mudar o algoritmo de hash ou rebaixar as iterações para acelerar o login**: a OWASP Password Storage Cheat Sheet recomenda manter um algoritmo forte e lento (Argon2id); o ganho de tempo não compensa.
  - **(d) Incluir os escopos `email`/`profile` no token para exibir o nome**: a API não precisa desses dados (LGPD, necessidade); o `preferred_username` basta.
  - **(e) Admin events com representação**: gravariam dados pessoais e, em algumas operações, credenciais nos eventos do Keycloak.

## Referências

- [ADR-0006](0006-keycloak-autenticacao-autorizacao.md), [ADR-0019](0019-integracao-operacional-keycloak.md),
  [ADR-0020](0020-isolamento-dados-identidade.md); [README](../../README.md), seção "Credenciais de
  demonstração (somente local)".
- OWASP ASVS 4.0.3: https://owasp.org/www-project-application-security-verification-standard/
- OWASP Password Storage Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
- NIST SP 800-63B: https://pages.nist.gov/800-63-3/sp800-63b.html
- LGPD (Lei 13.709/2018): https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm
- CIS PostgreSQL Benchmark e CIS Docker Benchmark: https://www.cisecurity.org/cis-benchmarks
- ISO/IEC 27002: https://www.iso.org/standard/75652.html
- RFC 6750: https://www.rfc-editor.org/rfc/rfc6750 · RFC 7517: https://www.rfc-editor.org/rfc/rfc7517 ·
  RFC 8725: https://www.rfc-editor.org/rfc/rfc8725 · RFC 9700: https://www.rfc-editor.org/rfc/rfc9700
- OWASP Top 10: https://owasp.org/www-project-top-ten/
