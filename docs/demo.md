# Catálogo opcional para demonstração

> **Mudança em relação ao roteiro anterior (#91/E2E):** o Keycloak sobe pelo compose (`quay.io/keycloak/keycloak:26.8.1-1`, realm único `keycloak/realm-oficina.json`; o `docs/e2e/keycloak/oficina-realm.json` foi removido no PR #79) e as senhas de demonstração mudaram (`demo123` deixou de valer); valores e token na seção "Credenciais de demonstração (somente local)" do [README](../README.md).

A carga facilita a avaliação sem precisar cadastrar todo o catálogo manualmente.
Ela insere apenas serviços e peças/insumos de exemplo; não simula login, clientes, OS,
reservas, decisões ou conclusão de serviços. Esses fluxos continuam sendo demonstrados pelas APIs.

## Ordem de execução

1. Suba o banco, o Keycloak e o MailPit: `docker compose --profile dev up -d --wait`.
2. Inicie a aplicação: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`.
3. Aguarde a inicialização, incluindo o Flyway. Em outro terminal, execute `./scripts/popular-catalogo-demo.sh` na raiz do projeto.
4. Acesse o Swagger em `http://localhost:8080/swagger-ui.html`, autorize com JWT e consulte `/api/servicos` e `/api/pecas`.

Se preferir app e banco no Docker, use `docker compose --profile full up -d --build`, aguarde
`http://localhost:8080/api/health` responder e execute o mesmo script. A autenticação usa o
Keycloak do compose (realm `oficina`, issuer `http://localhost:8081/realms/oficina`). Obtenha
o access token como descrito no README, seção "Procedimento de autenticação", com um usuário
cujo Perfil a rota exige.

O banco sozinho ainda não tem as tabelas: elas são criadas pelo Flyway ao iniciar a aplicação.
Se a carga for executada antes disso, termina com uma mensagem de erro sem inserir dados.

## Dados e IDs

| ID | Serviço | Preço |
|---|---|---:|
| `11700000-0000-4000-8000-000000000101` | [DEMO] Troca de óleo | R$ 80,50 |
| `11700000-0000-4000-8000-000000000102` | [DEMO] Alinhamento e balanceamento | R$ 120,00 |
| `11700000-0000-4000-8000-000000000103` | [DEMO] Revisão de freios | R$ 180,00 |
| `11700000-0000-4000-8000-000000000104` | [DEMO] Diagnóstico eletrônico | R$ 90,00 |

| ID | Peça/insumo | Tipo | Preço unitário | Saldo físico inicial |
|---|---|---|---:|---:|
| `11700000-0000-4000-8000-000000000201` | [DEMO] Óleo de motor 5W30 - 1 litro | INSUMO | R$ 25,25 | 20 |
| `11700000-0000-4000-8000-000000000202` | [DEMO] Filtro de óleo | PECA | R$ 35,00 | 10 |
| `11700000-0000-4000-8000-000000000203` | [DEMO] Jogo de pastilhas de freio | PECA | R$ 240,00 | 5 |
| `11700000-0000-4000-8000-000000000204` | [DEMO] Filtro de ar | PECA | R$ 60,00 | 8 |
| `11700000-0000-4000-8000-000000000205` | [DEMO] Correia sem estoque | PECA | R$ 100,00 | 0 |

## Exemplo de demonstração da OS

Abra uma OS e inclua o serviço de troca de óleo (uma unidade):

```http
POST /api/ordens-de-servico/{id}/servicos
Content-Type: application/json
Authorization: Bearer <JWT de MECANICO ou ADMINISTRADOR>

{"servicoId":"11700000-0000-4000-8000-000000000101","quantidade":1}
```

Inclua três litros de óleo pela rota de peças/insumos:

```http
POST /api/ordens-de-servico/{id}/pecas
Content-Type: application/json
Authorization: Bearer <JWT de ATENDENTE, MECANICO ou ADMINISTRADOR>

{"pecaId":"11700000-0000-4000-8000-000000000201","quantidade":3}
```

Na primeira execução, o óleo ficará com físico 20, reservado 3 e livre 17. Inicie e conclua o diagnóstico para gerar o orçamento: total de **R$ 156,25**. Registre aprovação: execução iniciada e saldo físico
17, reservado 0, livre 17. Conclua os serviços e registre a entrega. Para conferir perfis,
rotas e respostas, veja a [documentação da API](api/ordens-de-servico.md).

Antes de gerar o orçamento, tente incluir a correia (ID terminado em `205`) com quantidade 1:
deve retornar 409, pois tem saldo zero. Essa tentativa não deve adicionar item nem alterar o total.

## Reexecução e isolamento

A carga usa `INSERT ... ON CONFLICT (id) DO NOTHING`, em uma transação. Não sobrescreve
registros existentes, não repõe estoque e não remove reservas. Se editar um item de demonstração
pela API, uma nova execução preserva suas alterações. Nomes iguais com IDs diferentes não são
mesclados; somente os IDs fixos da carga identificam esses registros.

O SQL está em `scripts/demo/catalogo.sql`, fora de `db/migration` e do init do PostgreSQL.
Assim, o avaliador escolhe se quer os dados de exemplo; não existe carga automática em
produção nem dependência desse catálogo nos testes. Não há alteração de schema ou de regras
de negócio. O script usa o serviço `postgres` e as credenciais já configuradas dentro do
container; não exige cliente PostgreSQL instalado na máquina.

## Verificação da carga

Validado em 2026-10-24 com PostgreSQL 17 em Compose temporário, sem usar o banco de trabalho:

- ausência de tabelas retorna erro com instrução para executar Flyway;
- migrations atuais V1–V8 aplicadas e carga executada duas vezes: 4 serviços e 5 peças/insumos;
- nova execução preserva preço alterado, estoque consumido, reserva e versão otimista;
- peça destinada ao cenário de falta de saldo permanece com quantidade zero;
- constraint temporária que impede inserir uma peça provoca rollback também dos serviços;
- sintaxe Bash verificada com `bash -n`; containers temporários removidos após a validação.
