# Teste ponta a ponta — 29/10/2026 16:27:53, America/Sao_Paulo

Execução contra o código do [PR #85](https://github.com/bernacamargo/tech-challenge-18soat/pull/85), commit `1a51b79793d39620f435ad55844ae7be81d2be2f` (`fix/auditoria-fluxos-api`). As chamadas terminaram por volta de **17:16, America/Sao_Paulo** (o envio do orçamento ficou registrado às 20:16:12 UTC).

Resultado: **os nove cenários passaram.**

## Ambiente

```bash
docker compose --profile dev up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Keycloak 26.4.0 na porta 8081, com o realm importado de `docs/e2e/keycloak/oficina-realm.json`. O issuer conferido foi `http://localhost:8081/realms/oficina`. O catálogo demo foi carregado com `./scripts/popular-catalogo-demo.sh`.

Token (password grant, client `oficina-api`, senha `demo123`):

```bash
curl -s -X POST "http://localhost:8081/realms/oficina/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=oficina-api&username=administrador&password=demo123"
```

O mesmo comando, trocando o usuário, emitiu token para `atendente` e `mecanico`. Os três papéis de realm vieram em `realm_access.roles`.

Expandir uma operação POST no Swagger travou o renderer do Chrome neste ambiente. As chamadas abaixo foram feitas na API em `http://localhost:8080`, com o JWT do Keycloak, e o print mostra o status e o corpo recebidos. O health também foi executado dentro do Swagger. O e-mail foi lido na UI do MailPit.

![Lista de operações no Swagger](imagens/00-swagger.png)

## 1. Saúde e login

`GET /api/health`, sem token. Esperado 200 e `UP`. Obtido 200, `status` UP, app `oficina`.

![Health no Swagger](imagens/01-health-swagger.png)

`POST /api/ordens-de-servico` sem token. Esperado 401. Obtido 401.

![401 sem token](imagens/02-sem-token-401.png)

## 2. Fluxo feliz — troca de óleo

OS `99a0b38d-de08-4173-b204-50343b6e326b`, cliente `52998224725`, placa `ABC1D23`, e-mail `maria.e2e@exemplo.com`.

Atendente abre a OS. Esperado 201 e `RECEBIDA`. Obtido 201 e `RECEBIDA`.

![Abertura da OS](imagens/04-abrir-os.png)

Mecânico inclui o serviço `...0101`. Esperado 200 com um item. Obtido 200.

![Serviço incluído](imagens/05-incluir-servico.png)

Atendente inclui 3 litros do óleo `...0201`. Esperado 200 com dois itens. Obtido 200.

![Óleo incluído](imagens/06-incluir-oleo.png)

Mecânico inicia o diagnóstico. Esperado 200 e `EM_DIAGNOSTICO`. Obtido 200.

![Diagnóstico](imagens/15-diagnostico.png)

Mecânico conclui o diagnóstico. Esperado 200, `AGUARDANDO_APROVACAO` e total **156.25**. Obtido 200, total 156.25 (serviço 80,50 e óleo 75,75).

![Orçamento](imagens/16-orcamento.png)

O MailPit ficou com uma única mensagem, de `no-reply@oficina.local` para `maria.e2e@exemplo.com`, com veículo `ABC1D23` e total R$ 156,25.

![Caixa do MailPit](imagens/55-mailpit-caixa.png)

![Corpo do orçamento](imagens/56-mailpit-orcamento.png)

Consulta pública sem JWT. Esperado somente `status` e `finalizadaEm`, status `AGUARDANDO_APROVACAO`. Obtido isso.

![Consulta pública antes da aprovação](imagens/17-consulta-publica.png)

Estoque do óleo antes da aprovação: físico 20, reservado 3, livre 17.

![Estoque reservado](imagens/14-estoque-reservado.png)

Aprovação pública. Esperado 200 e `EM_EXECUCAO`, sem dados pessoais. Obtido 200.

![Aprovação](imagens/18-aprovacao.png)

Estoque depois da baixa: físico 17, reservado 0, livre 17.

![Estoque baixado](imagens/19-estoque-baixado.png)

Mecânico conclui. Esperado 200, `FINALIZADA` e `finalizadaEm` preenchido. Obtido 200.

![Conclusão](imagens/26-conclusao.png)

Atendente entrega. Esperado 200 e `ENTREGUE`, preservando `finalizadaEm`. Obtido 200.

![Entrega](imagens/27-entrega.png)

Consulta pública final. Esperado `ENTREGUE` com `finalizadaEm`. Obtido isso.

![Consulta pública entregue](imagens/28-consulta-entregue.png)

## 3. Estoque insuficiente

Antes do orçamento, a correia `...0205` com quantidade 1. Esperado 409 e a OS segue com dois itens. Obtido 409, mensagem de estoque insuficiente. O detalhe seguinte continua com dois itens e orçamento nulo.

![Correia sem saldo](imagens/07-correia-409.png)

![Itens preservados](imagens/08-itens-apos-correia.png)

## 4. Rejeição

Segunda OS `5fcec277-0844-4680-ad96-d360dec1dbc9`, placa `DEF2E34`, uma unidade do filtro `...0202`. Antes da rejeição o filtro estava físico 10, reservado 1, livre 9. A rejeição pública com motivo `Preço acima do esperado` manteve `AGUARDANDO_APROVACAO`. O detalhe administrativo gravou o motivo e deixou `iniciadaEm` nulo. O filtro voltou a físico 10, reservado 0, livre 10.

![OS da rejeição](imagens/29-os-rejeicao.png)

![Filtro reservado](imagens/30-filtro-reservado.png)

![Orçamento da segunda OS](imagens/31-orcamento-rejeicao.png)

![Filtro antes da rejeição](imagens/32-filtro-antes.png)

![Rejeição pública](imagens/33-rejeicao.png)

![Detalhe com motivo](imagens/34-detalhe-rejeicao.png)

![Filtro devolvido](imagens/35-filtro-devolvido.png)

## 5. Papéis do catálogo

Administrador cadastrou o serviço `E2E Consulta de catalogo` (201). Mecânico listou e o id apareceu (200). Atendente detalhou o mesmo id (200). Atendente recebeu 403 ao criar e ao alterar. Mecânico recebeu 403 ao excluir.

![Cadastro pelo administrador](imagens/36-cadastrar-servico-admin.png)

![Lista do mecânico](imagens/37-mecanico-lista-servicos.png)

![Detalhe do atendente](imagens/38-atendente-detalha-servico.png)

![Atendente não cria](imagens/39-atendente-cria-servico-403.png)

![Atendente não altera](imagens/40-atendente-altera-servico-403.png)

![Mecânico não remove](imagens/41-mecanico-remove-servico-403.png)

## 6. Vínculo na exclusão

Com a OS da troca de óleo e a reserva do óleo ainda ativas, administrador recebeu 409 ao excluir cliente `52998224725`, veículo `ABC1D23` e a peça do óleo. A mensagem foi `Cadastro possui vinculos com outros registros e nao pode ser removido ou alterado`. Os GET seguintes continuaram 200 e o reservado permaneceu 3 (print do estoque no cenário 2).

![Cliente com vínculo](imagens/09-delete-cliente-409.png)

![Veículo com vínculo](imagens/10-delete-veiculo-409.png)

![Peça com vínculo](imagens/11-delete-peca-409.png)

![Cliente preservado](imagens/12-cliente-preservado.png)

![Veículo preservado](imagens/13-veiculo-preservado.png)

Sem histórico, a exclusão segue a ordem dos vínculos. Cliente `39053344705` com o veículo `JKL4G67`: excluir o cliente primeiro responde 409. Excluir o veículo responde 204 e, em seguida, o cliente responde 204. A peça criada só para o limite, sem reserva, também responde 204.

![Cliente sem OS](imagens/49-cliente-sem-historico.png)

![Veículo sem OS](imagens/50-veiculo-sem-os.png)

![Cliente ainda vinculado ao veículo](imagens/51-cliente-com-veiculo-409.png)

![Veículo removido](imagens/52-veiculo-204.png)

![Cliente removido](imagens/53-cliente-204.png)

![Peça sem reserva removida](imagens/54-peca-204.png)

## 7. Limites

Abertura com nome de 151 caracteres, documento `39053344705` e placa `JKL4G67`. Esperado 400 e nenhum cliente, veículo ou OS. Obtido 400. A contagem no PostgreSQL ficou 0, 0 e 0 (`limites-contagem.sql.txt`).

![Nome longo](imagens/42-nome-longo-400.png)

Peça `Oleo e2e` criada com saldo 7 e preço 25.25. Preço `25.251` no cadastro e na alteração: 400. O GET seguinte manteve nome, preço 25.25, saldo 7 e reserva 0.

![Peça válida](imagens/43-peca-valida.png)

![Preço inválido no cadastro](imagens/44-preco-invalido-400.png)

![Preço inválido na alteração](imagens/45-preco-alteracao-400.png)

![Peça intacta](imagens/46-peca-intacta.png)

Entrada de `2147483647` sobre esse saldo. Esperado 400 e a mensagem `Entrada excede o limite de quantidade em estoque`, sem mudar saldo nem reserva. Obtido 400 e o GET seguinte com saldo 7 e reserva 0.

![Overflow de entrada](imagens/47-overflow-400.png)

![Estoque preservado](imagens/48-estoque-preservado.png)

## 8. E-mail cancelado após a decisão

OS `c09ebed6-3dfd-4fc6-a771-37a858e6cbb8`, cliente `11144477735`, sem e-mail. Atendente recebeu 403 ao iniciar o diagnóstico. Mecânico incluiu o serviço e submeteu: `AGUARDANDO_APROVACAO`. A fila ficou `AGUARDANDO_EMAIL`. A aprovação pública aconteceu antes do contato e só então o e-mail `paulo.lima@exemplo.com` foi gravado.

![OS sem e-mail](imagens/20-os-sem-email.png)

![Atendente não diagnostica](imagens/21-atendente-diagnostico-403.png)

![Serviço da OS sem e-mail](imagens/22-servico-os-sem-email.png)

![Submissão](imagens/23-submissao-sem-email.png)

![Aprovação antes do contato](imagens/24-aprovacao-antes-do-email.png)

![E-mail gravado depois](imagens/25-email-depois-da-decisao.png)

Na tentativa seguinte o dispatcher cancelou a mensagem. `cancelada_em` preenchido, `enviada_em` nulo, `tentativas` 0. O MailPit não tem assunto com esse UUID; a única mensagem é a do fluxo feliz.

```text
c09ebed6-3dfd-4fc6-a771-37a858e6cbb8 | enviada_em vazio | cancelada_em 2026-10-29 20:16:58+00 | tentativas 0
99a0b38d-de08-4173-b204-50343b6e326b | enviada_em 2026-10-29 20:16:12+00 | cancelada_em vazio | tentativas 1
```

A saída completa está em `fila-cancelada.sql.txt`.

## 9. Autorização

Sem token, `POST /api/ordens-de-servico` respondeu 401 (cenário 1). Atendente em `POST .../diagnostico` respondeu 403 (print no cenário 8).
