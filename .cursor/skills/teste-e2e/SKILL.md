---
name: teste-e2e
description: Roda o teste ponta a ponta do sistema da oficina contra a API no ar e grava evidências com screenshots. Use quando pedirem teste e2e, teste ponta a ponta, evidências do Swagger ou do MailPit, ou repetir a demonstração da ordem de serviço.
---

# Teste ponta a ponta da oficina

Exercita o sistema da oficina em execução: atendimento, catálogo e estoque, identidade e notificação. O objeto é o projeto no commit em que a API subiu. Deixa um relatório versionado por data e hora. A evidência é o sistema no ar; não crie teste JUnit novo. Não substitua o Keycloak por segredo HMAC. Se o issuer divergir ou o Keycloak não ficar saudável, registre a falha no relatório e pare.

## Quando gravar

No início, anote o commit (`git rev-parse HEAD`) e o instante em America/Sao_Paulo:

```bash
TZ=America/Sao_Paulo date +%Y-%m-%dT%H-%M-%S-03
TZ=America/Sao_Paulo date +'%d/%m/%Y %H:%M:%S, America/Sao_Paulo'
```

Crie `docs/e2e/<pasta>/evidencias.md` e `docs/e2e/<pasta>/imagens/`. Não apague pastas antigas. Inclua a execução no topo de `docs/e2e/README.md` (data/hora, commit, link). Abra PR só se a pessoa pedir para publicar.

## Ambiente

A JVM fica no host para o claim `iss` coincidir com `http://localhost:8081/realms/oficina`.

```bash
docker compose --profile dev up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Keycloak efêmero na porta 8081, importando `docs/e2e/keycloak/oficina-realm.json`:

```bash
docker run -d --name keycloak \
  -p 8081:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  -e KC_HOSTNAME=http://localhost:8081 \
  -e KC_HOSTNAME_STRICT=false \
  -e KC_HTTP_ENABLED=true \
  -e KC_HEALTH_ENABLED=true \
  -v "$PWD/docs/e2e/keycloak:/opt/keycloak/data/import:ro" \
  quay.io/keycloak/keycloak:26.4.0 \
  start-dev --import-realm --http-port=8080
```

Espere `GET http://localhost:8080/api/health` e o documento OpenID em `http://localhost:8081/realms/oficina/.well-known/openid-configuration`. Confira que `issuer` é `http://localhost:8081/realms/oficina`. Depois do Flyway:

```bash
./scripts/popular-catalogo-demo.sh
```

Token (password grant, client público `oficina-api`, senha `demo123`):

```bash
curl -s -X POST "http://localhost:8081/realms/oficina/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=oficina-api&username=administrador&password=demo123"
```

Repita para `atendente` e `mecanico`. No Swagger (`http://localhost:8080/swagger-ui.html`), Authorize com `Bearer <access_token>`.

## Como evidenciar

Cada cenário no Swagger ou no MailPit (`http://localhost:8025`). No markdown: papel, rota, esperado, obtido e o print em `imagens/`.

Se expandir uma operação POST no Swagger travar o navegador, faça a chamada na API em execução (`localhost:8080`) com o JWT do Keycloak e fotografe status e corpo. Não troque o Keycloak por segredo HMAC. Leituras que o Swagger conseguir executar continuam nele. O e-mail continua sendo lido na UI do MailPit. Use CPF e placa válidos e distintos entre cenários que não podem compartilhar cadastro. IDs do catálogo demo estão em `docs/demo.md`.

1. **Saúde e login.** `GET /api/health` sem token. Os três tokens saem do Keycloak. Swagger lista as operações do sistema.
2. **Catálogo de demonstração.** `GET /api/servicos` traz os quatro serviços `11700000-0000-4000-8000-000000000101` a `...0104`. `GET /api/pecas` traz as cinco peças `...0201` a `...0205`.
3. **Clientes e veículos.** Atendente ou administrador cadastra um CPF e um CNPJ, lista, detalha e altera nome ou e-mail. Cadastra o veículo, altera marca/modelo/ano e consulta pela placa. Mecânico em `/api/clientes` recebe **403**. Repetir o mesmo documento recebe **409** (`Cliente ja cadastrado`).
4. **Abertura com cadastro existente.** Mecânico em `POST /api/ordens-de-servico` recebe **403**. Atendente abre a OS com documento e placa já cadastrados: **201**, `RECEBIDA`, e o nome gravado no cliente não muda. `GET /api/ordens-de-servico` inclui essa OS.
5. **Fluxo feliz — troca de óleo.** OS com e-mail. Serviço `...0101` (mecânico) e 3 unidades do óleo `...0201`. Diagnóstico e conclusão. Status `AGUARDANDO_APROVACAO` e total **R$ 156,25** quando o preço do catálogo demo estiver intacto. MailPit mostra a mensagem. A reserva reduz o livre e a aprovação pública baixa o físico. Consulta pública sem JWT só com `status` e `finalizadaEm`. Mecânico conclui; atendente entrega. Consulta pública `ENTREGUE` com `finalizadaEm`.
6. **Estoque insuficiente.** Antes do orçamento, correia `...0205` quantidade 1. Esperado **409**, item fora da OS.
7. **Rejeição.** Segunda OS com peça reservada, orçamento gerado, `POST /api/acompanhamento/ordens-de-servico/{id}/rejeicao` com motivo. Reserva devolvida e `rejeicaoOrcamento` no detalhe administrativo.
8. **Papéis.** Mecânico e atendente leem serviço e peça (**200**) e não criam, alteram nem excluem (**403**). Atendente não diagnostica nem conclui (**403**). Mecânico não entrega (**403**). Conclusão com a OS ainda `RECEBIDA`: **409**. Entrada de estoque só com administrador: uma quantidade válida soma `quantidadeDisponivel` e `quantidadeLivre`; mecânico recebe **403**.
9. **Vínculo na exclusão.** Com OS e reserva ativas, `DELETE` de cliente, veículo e peça **409** com a mensagem `Cadastro possui vinculos com outros registros e nao pode ser removido ou alterado`. `GET` seguinte **200** e a reserva permanece. Sem histórico: excluir veículo **204**, depois cliente **204**; peça sem reserva **204**.
10. **Limites.** Nome de 151 caracteres na abertura da OS: **400** e nenhum cliente, veículo ou OS novo. Preço `25.251` na peça: **400** no cadastro e na alteração, cadastro anterior intacto. `POST /api/pecas/{id}/entradas` com `2147483647` sobre saldo existente: **400** e mensagem `Entrada excede o limite de quantidade em estoque`; saldo e reserva iguais aos de antes.
11. **E-mail do orçamento.** No fluxo feliz a mensagem chega ao MailPit antes da decisão. Numa OS sem e-mail, espere a fila marcar espera de contato (próxima tentativa em 1 minuto). Aprove e só então grave o e-mail. Espere a tentativa seguinte. O MailPit não recebe essa OS. Confirme no SQL `cancelada_em` preenchido e `enviada_em` nulo, e inclua a saída junto com o print da caixa.
12. **Acompanhamento e autorização.** Sem token: **401** em `POST /api/ordens-de-servico`. Consulta pública de UUID inexistente: **404**. Identificador que não é UUID: **400**. A consulta de uma OS real continua só com `status` e `finalizadaEm`.

Observabilidade (Grafana/Prometheus) fica de fora.

Consulta da fila, sem conteúdo da mensagem:

```sql
SELECT ordem_de_servico_id, criada_em, enviada_em, cancelada_em, tentativas, ultimo_erro
FROM notificacao_orcamento
ORDER BY criada_em DESC;
```

## Cabeçalho do relatório

Inclua o instante por extenso, o commit testado, os comandos de subida e o comando do token. Diga que a execução valida o sistema da oficina e o resultado de cada cenário (passou ou falhou) antes dos prints. Se uma execução anterior no mesmo ambiente já cobriu parte do roteiro, aponte a pasta e não apague aquele relatório; complete nesta pasta o que faltou.
