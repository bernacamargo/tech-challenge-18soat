# ADR-0019: Integração operacional com Keycloak

- **Status**: Proposto
- **Data**: 2026-10-29
- **Participantes**: Grupo 117 (proposta: Caio Crevelaro)

## Contexto

A [ADR-0006](0006-keycloak-autenticacao-autorizacao.md) (Aceita) escolheu o Keycloak self-hosted
como provedor de identidade e a API como OAuth2 Resource Server. Ela não registrou **como** a
aplicação se integra a ele: qual é a credencial, onde fica o contrato de autenticação, como o
token é validado, que resposta HTTP sai de cada falha e como o ambiente local fica de pé. A
[ADR-0007](0007-notificacoes-smtp-local.md) (Aceita) escolheu o MailPit como SMTP local.

Estado encontrado na `main` `1f01220`, antes desta fatia:

- `infra/config/JwtDecoderConfig` montava o decoder e aceitava um modo HMAC (segredo
  compartilhado) ativável fora de teste;
- o issuer configurado era `localhost:8081`, inalcançável de dentro do container da aplicação
  no perfil de execução `full`;
- não havia validação da claim `aud`: qualquer token do realm servia para a API;
- `@EnableMethodSecurity` estava declarado duas vezes (`OrdemDeServicoConfig` e
  `CatalogoEstoqueConfig`);
- o conversor de authorities (`KeycloakRealmRoleConverter`) promovia a authority qualquer realm
  role do token, inclusive os técnicos do Keycloak (`default-roles-oficina`, `offline_access`, `uma_authorization`);
- os pacotes `identidadeacesso/domain` e `identidadeacesso/application` estavam vazios;
- não havia Keycloak nem MailPit no `docker-compose.yml`.

A issue [#4](https://github.com/bernacamargo/tech-challenge-18soat/issues/4) pede, no domínio,
"o contrato de autenticação (interface) que valida credenciais"; a
[#5](https://github.com/bernacamargo/tech-challenge-18soat/issues/5) pede o caso de uso que o
usa como port de saída, testado com um port fake; a
[#6](https://github.com/bernacamargo/tech-challenge-18soat/issues/6) pede a integração real.
O enunciado exige autenticação JWT nas APIs administrativas, Swagger e a autenticação
demonstrada no vídeo. A [ADR-0001](0001-clean-architecture-ddd.md) e o `AGENTS.md` (linha 40)
põem os ports de saída na camada `application`.

## Decisão

1. A credencial é o **access token Bearer** (RFC 6750) emitido pelo Keycloak (`Credencial`); a senha do funcionário nunca chega à API, só ao Keycloak.
2. O port `ValidadorDeCredencial` (com `DadosDaCredencial` e `CredencialInvalidaException`) fica em `identidadeacesso/domain`, por definição da issue #4: é um **desvio consciente** da [ADR-0001](0001-clean-architecture-ddd.md) e do `AGENTS.md` (linha 40, "ports de saída na application"); o domínio continua sem nenhuma dependência de framework (sem Spring, Nimbus ou Keycloak nos imports).
3. O caso de uso `AutenticarUsuarioAdministrativo` (`identidadeacesso/application/usecase`) orquestra e o domínio decide: `UsuarioAdministrativo.de` exige `sub` e `preferred_username`, e `Perfil.reconhecer` aceita só `ATENDENTE`, `MECANICO` e `ADMINISTRADOR` por nome exato (caixa exata), descartando os realm roles técnicos; sem nenhum perfil da oficina sai `UsuarioSemPerfilDaOficinaException`.
4. O resource server delega a autenticação ao caso de uso: `oauth2ResourceServer(o -> o.jwt(j -> j.authenticationManager(...)))` no `SecurityConfig`, com o `UsuarioAdministrativoAuthenticationManager` (que **não** é `@Bean`, só entra nessa cadeia) produzindo `UsuarioAdministrativoAuthentication`; o adapter `KeycloakValidadorDeCredencial` (registrado em `IdentidadeAcessoConfig`) usa o `JwtDecoder` do Boot, e há uma única decodificação por requisição; o `KeycloakRealmRoleConverter` foi apagado; o fallback por `JwtAuthenticationConverter` previsto no plano não foi necessário (não usado).
5. O decoder vem só das propriedades do Spring Boot (trecho abaixo): `JwtDecoderConfig` foi apagado; com `jwk-set-uri` o Boot aplica issuer, audience e algoritmo sem discovery na subida, e a aplicação não tem `depends_on` do Keycloak (o JWKS é buscado na primeira requisição autenticada).
6. HMAC existe só em teste, como caso negativo (`JwtTestSupport.tokenHs256`, recusado com 401); os testes de integração assinam tokens RS256 reais com chave gerada em runtime e servem o JWKS por `JwksTestServer`; o `AbstractIntegrationTest` registra **só** `jwk-set-uri`, e issuer e audience vêm do `application.yml` de produção (diverge da letra do plano, que registrava as três propriedades no teste, para evitar um teste tautológico que valida a configuração que ele mesmo escreveu).
7. As falhas são mapeadas como na tabela "Mapeamento de falhas" logo depois desta lista: credencial inválida dá 401 com descrição fixa, credencial válida sem perfil da oficina dá 403 pelo piso, e falha técnica do provedor dá 5xx (falha fechada).
8. Um Bearer inválido enviado a uma rota pública responde 401 (RFC 6750: o resource server avalia todo header `Authorization` recebido); o comportamento é aceito e documentado no [README](../../README.md).
9. Os validadores adicionais foram **adotados (T19)**: `JwtValidacaoConfig` exige `exp` presente e `typ == Bearer` (caixa exata), de modo que token sem expiração e ID token do Keycloak (`typ=ID`) são recusados com 401.
10. O password grant (`grant_type=password`) fica habilitado **só para demonstração** com curl e Swagger (a RFC 9700 o desaconselha): o client `oficina-api` é público, sem client secret, com `directAccessGrantsEnabled=true` e `standardFlowEnabled=false`; em produção o fluxo seria Authorization Code com PKCE.
11. O Swagger mantém o esquema `bearer-jwt` (`http`/`bearer`/`JWT`) no `OpenApiConfig`, com o curl de obtenção do token na descrição e as respostas 401 e 403 documentadas em toda operação protegida; não há fluxo `oauth2` nem PKCE no Swagger.
12. O MailPit é o SMTP local **compartilhado**: o Keycloak usa `smtpServer` → `mailpit:1025` (e-mails de ação de conta) e a aplicação envia o orçamento pelo mesmo MailPit (`NotificacaoPort`, [ADR-0017](0017-notificacao-orcamento-email.md), #82); SMTP `1025` e UI `8025` publicados só em `127.0.0.1`; a [ADR-0007](0007-notificacoes-smtp-local.md) segue Aceita.
13. Terminologia: o "papel" da ADR-0006 (e das issues #4/#5) corresponde a **Perfil** na linguagem ubíqua ([glossário](../ddd/linguagem-ubiqua.md)); no Keycloak é a *realm role* e no Spring a authority `ROLE_*`; a ADR-0006 não é editada.

Propriedades do decoder (`src/main/resources/application.yml`, copiadas do arquivo):

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Issuer publico (o "iss" que o Keycloak emite), comparado por igualdade exata; sem barra final
          issuer-uri: ${OAUTH2_ISSUER_URI:http://localhost:8081/realms/oficina}
          # JWKS: no perfil full o compose aponta para o endereco interno (T16); local usa o default
          jwk-set-uri: ${OAUTH2_JWK_SET_URI:http://localhost:8081/realms/oficina/protocol/openid-connect/certs}
          # FIXO, sem env: lista vazia desliga a validacao de aud em silencio
          audiences: oficina-api
          # FIXO: so RS256 (nao usar jws-algorithm, singular)
          jws-algorithms: RS256
```

No perfil de execução `full`, o `docker-compose.yml` passa à aplicação
`OAUTH2_ISSUER_URI=http://localhost:8081/realms/oficina` (o mesmo `iss` público que o Keycloak
emite, fixado por `KC_HOSTNAME`) e
`OAUTH2_JWK_SET_URI=http://keycloak:8080/realms/oficina/protocol/openid-connect/certs` (rede
interna). Assim o issuer comparado é o público, e só a busca das chaves usa o endereço interno.

Mapeamento de falhas (conferido em `UsuarioAdministrativoAuthenticationManagerTest`,
`AutenticacaoIntegrationTest` e na verificação ponta a ponta):

| Situação | Exceção no domínio/adapter | Tradução na infraestrutura | HTTP |
|---|---|---|---|
| sem header `Authorization` em rota protegida | — | entry point do resource server | 401 |
| token malformado, expirado, `iss`/`aud` errados, assinatura inválida, `alg` diferente de RS256 | `CredencialInvalidaException` (o adapter converte `BadJwtException`) | `InvalidBearerTokenException("Token invalido")` | 401 |
| sem `sub` ou sem `preferred_username` | `CredencialInvalidaException` (domínio) | idem | 401 |
| autenticado sem perfil da oficina (só realm roles técnicos) | `UsuarioSemPerfilDaOficinaException` | autenticação **sem authorities**, barrada pelo piso `hasAnyRole("ATENDENTE", "MECANICO", "ADMINISTRADOR")` | 403 (rota pública continua respondendo normalmente) |
| perfil da oficina sem permissão na rota | — | `@PreAuthorize` ou regra de URL | 403 |
| JWKS indisponível (Keycloak fora do ar) | `JwtException` técnica, propagada sem conversão | `AuthenticationServiceException` | 5xx |

O 401 por credencial inválida tem corpo vazio e sempre o mesmo header, sem dizer o motivo
(observado na verificação ponta a ponta):

```
WWW-Authenticate: Bearer error="invalid_token", error_description="Token invalido", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1"
```

## Consequências

- Positivas:
  - as issues #4 e #5 ficam implementadas literalmente, e o domínio e o caso de uso são testáveis sem Spring, com port fake;
  - há uma única validação do token por requisição, feita pelo decoder do Boot;
  - HMAC deixa de ser possível em produção, porque o único decoder é o do Boot com `jws-algorithms: RS256`;
  - issuer, audience e algoritmo ficam fixos e são testados por mutantes (trocar ou remover cada propriedade derruba a suíte);
  - o 403 (autenticado sem perfil ou sem permissão) fica distinto do 401 (credencial inválida);
  - um termo único no domínio (Perfil), com a equivalência registrada no item 13;
  - um único realm versionado, `keycloak/realm-oficina.json`: o realm duplicado `docs/e2e/keycloak/oficina-realm.json` (Keycloak 26.4.0 subido por `docker run`, com a senha antiga) é removido neste PR, por decisão do grupo, para não haver dois realms nem duas senhas.
- Negativas / trade-offs aceitos:
  - o port no domínio é um desvio da ADR-0001 que precisa ser explicado na revisão; quem procurar os ports na `application` não o encontra ali;
  - o `AuthenticationManager` próprio substitui o provider padrão do Spring, por isso cada mapeamento da tabela exige um teste de integração;
  - um Bearer inválido em rota pública dá 401, em vez de ser ignorado;
  - o password grant é um antipadrão fora da demonstração;
  - com o Keycloak fora do ar, as rotas protegidas dão 5xx (falha fechada, sem fallback);
  - com o `AuthenticationManager` próprio deixa de valer a proteção do Spring contra downgrade de DPoP (`BearerTokenAuthenticationFilter.isDPoPBoundAccessToken`, Spring Security 6.5.11, só reconhece o token de autenticação padrão); o impacto é baixo porque o realm não emite tokens DPoP.
- Alternativas consideradas e por que foram descartadas:
  - **(a) `JwtAuthenticationConverter` chamando o caso de uso** (fallback do plano): funcionaria, mas o converter recebe o `Jwt` já decodificado, e o adapter do domínio teria de decodificar de novo ou receber claims prontas, sem o port validar a credencial; não foi necessário porque `jwt.authenticationManager(...)` entrou no caminho real.
  - **(b) Manter `JwtDecoderConfig` com o modo HMAC sob flag**: um segredo compartilhado ativável em produção é exatamente o furo a fechar; uma flag errada no ambiente aceitaria tokens forjados.
  - **(c) Port na `application`, seguindo a letra da ADR-0001**: contraria o texto das issues #4/#5, que definem o contrato no domínio; a pureza do domínio já é garantida sem imports de framework.
  - **(d) `issuer-uri` só com discovery (`fromIssuerLocation`)**: exige o Keycloak no ar na subida da aplicação e um issuer alcançável de dentro do container, o que o `localhost:8081` público não é.
  - **(e) `KC_HOSTNAME_BACKCHANNEL_DYNAMIC`**: faria o Keycloak emitir um `iss` interno nas chamadas pela rede do compose, diferente do público, e o mesmo token passaria a valer num perfil de execução e não no outro.
  - **(f) 401 para usuário autenticado sem perfil da oficina**: confunde "não sei quem é" com "sei quem é, mas não pode"; o 403 é a resposta correta para credencial válida sem autorização.
  - **(g) Lançar `AccessDeniedException` dentro do filtro de autenticação**: a exceção sai do filtro fora do tratamento do Spring Security e vira 500.
  - **(h) Fluxo `oauth2`/PKCE no Swagger**: exigiria redirect URI, client confidencial ou Authorization Code no realm e mais configuração no Keycloak, sem ganho para a demonstração com Bearer e curl.

## Referências

- [ADR-0001](0001-clean-architecture-ddd.md), [ADR-0002](0002-postgres-for-everything.md),
  [ADR-0006](0006-keycloak-autenticacao-autorizacao.md), [ADR-0007](0007-notificacoes-smtp-local.md),
  [ADR-0017](0017-notificacao-orcamento-email.md).
- [ADR-0020](0020-isolamento-dados-identidade.md) (persistência do Keycloak no Postgres da aplicação)
  e [ADR-0021](0021-politica-seguranca-identidade.md) (política de segurança de identidade).
- [README](../../README.md), seção "Credenciais de demonstração (somente local)" (usuários de
  demonstração, obtenção do token, reset do realm); [C4 de containers](../diagramas/c4-container.md).
- Issues [#4](https://github.com/bernacamargo/tech-challenge-18soat/issues/4),
  [#5](https://github.com/bernacamargo/tech-challenge-18soat/issues/5),
  [#6](https://github.com/bernacamargo/tech-challenge-18soat/issues/6).
- RFC 6750 (Bearer Token Usage), RFC 9700 (OAuth 2.0 Security Best Current Practice).
- Numeração: 0019–0021 decidida pelo grupo em 2026-10-29, porque a 0018 foi ocupada pelo #85 ([ADR-0018](0018-cancelamento-notificacao-orcamento-decidido.md)); a 0015 duplicada (#74/#76) é anterior e não é tratada aqui.
