# ADR-0004: Autenticação JWT self-managed (sem provedor externo)

- **Status**: Substituído por [ADR-0006](0006-keycloak-autenticacao-autorizacao.md)
- **Data**: 2026-10-01
- **Participantes**: Grupo 117 (alinhado em reunião)

## Contexto

O enunciado exige autenticação JWT para as APIs administrativas, com credenciais de demonstração
e procedimento de autenticação documentados. O grupo avaliou AWS Cognito e implementação própria.

## Decisão

Implementar autenticação **self-managed**: usuários administrativos no próprio PostgreSQL
(contexto `identidadeacesso`), emissão de **JWT** pela aplicação (Spring Security + biblioteca JWT),
com credenciais de demonstração documentadas no README.

## Consequências

- Positivas: zero dependência de cloud/contas externas; alinhado com a stack 100% dockerizada;
  escopo simples para o MVP; controle total para fins didáticos.
- Negativas: o grupo é responsável por hashing de senha (BCrypt), expiração e refresh — sem
  MFA/recuperação de senha de provedor (fora do escopo da fase).
- Alternativas descartadas: AWS Cognito (complexidade e dependência externa desnecessárias para o
  escopo; hipótese de SMTP/cloud só volta na questão de notificações).
