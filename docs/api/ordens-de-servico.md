# API de Ordens de Serviço — base da issue #36

O detalhe administrativo inclui itens, orçamento, início da execução e rejeição quando registrados.
A inclusão de itens consulta o catálogo e reserva peças no estoque; a aprovação baixa as reservas e inicia a execução.

## Endpoints

| Método | Rota | Perfis | Comportamento |
|---|---|---|---|
| POST | `/api/ordens-de-servico` | ATENDENTE, ADMINISTRADOR | Abre OS em Recebida; pode apontar um serviço ou uma peça |
| GET | `/api/ordens-de-servico` | ATENDENTE, MECANICO, ADMINISTRADOR | Lista ordenada pelo UUID |
| GET | `/api/ordens-de-servico/{id}` | ATENDENTE, MECANICO, ADMINISTRADOR | Detalhe com itens/orçamento ou 404 |
| POST | `/api/ordens-de-servico/{id}/diagnostico` | MECANICO, ADMINISTRADOR | Recebida → Em diagnóstico |
| POST | `/api/ordens-de-servico/{id}/diagnostico/conclusao` | MECANICO, ADMINISTRADOR | Com itens, gera orçamento e vai para Aguardando aprovação |
| POST | `/api/ordens-de-servico/{id}/submissao` | MECANICO, ADMINISTRADOR | De Recebida ou Em diagnóstico, com itens, gera orçamento e envia para aprovação |
| POST | `/api/ordens-de-servico/{id}/conclusao` | MECANICO, ADMINISTRADOR | Em execução → Finalizada; registra horário |
| POST | `/api/ordens-de-servico/{id}/entrega` | ATENDENTE, ADMINISTRADOR | Finalizada → Entregue; preserva horário |
| POST | `/api/ordens-de-servico/{id}/servicos` | MECANICO, ADMINISTRADOR | Inclui serviço com nome/preço do catálogo |
| POST | `/api/ordens-de-servico/{id}/pecas` | ATENDENTE, MECANICO, ADMINISTRADOR | Inclui peça e reserva estoque |
| POST | `/api/acompanhamento/ordens-de-servico/{id}/aprovacao` | público | Cliente aprova, baixa reservas e inicia execução |
| POST | `/api/acompanhamento/ordens-de-servico/{id}/rejeicao` | público | Cliente rejeita e devolve as reservas ao estoque |

Requisição de criação:

```json
{
  "documento": "52998224725",
  "nome": "Maria Souza",
  "placa": "ABC1D23",
  "marca": "Fiat",
  "modelo": "Uno",
  "ano": 2012
}
```

A resposta contém `id`, `clienteId` (documento normalizado), `veiculoId` (placa normalizada),
`status`, `finalizadaEm` (nulo até a conclusão), `itens`, `orcamento`, `iniciadaEm` e
`rejeicaoOrcamento`.
Cada item contém UUID, referência ao catálogo, tipo, descrição, quantidade, preço unitário e
subtotal. O orçamento contém o total; antes da geração é nulo e OS nova tem itens vazios. Criação retorna 201 e cabeçalho Location.
Veículo já cadastrado deve pertencer ao cliente informado. As três ações da #35 não recebem corpo e retornam 200 com a OS atualizada.
UUID inválido retorna 400, OS inexistente 404, ausência de JWT 401 e perfil não autorizado 403.
Transição inválida, incluindo repetição, retorna 409 com `mensagem`, preservando o estado.
Conclusão usa o relógio UTC do servidor com precisão de microssegundos (PostgreSQL),
e entrega preserva esse momento. Aprovação registra o início na mesma precisão.

## Persistência e transação

A migration V4 cria `ordem_de_servico`, com FKs para cliente/veículo, os seis status permitidos
e consistência entre status e finalização. O agregado é reconstituído sem disparar transições.

`OrdemDeServicoService` envolve a criação inteira em transação. Falha ao cadastrar o veículo
ou ao gravar a OS desfaz os cadastros anteriores. Consultas usam transações read-only.
Exclusão física de cliente/veículo referenciado fica impedida pelas FKs (ADR 0008).

## Segurança e verificação

Swagger em `/swagger-ui.html`, usando o esquema `bearer-jwt` existente. As rotas exigem JWT
e os perfis são verificados por `@PreAuthorize`, sem alterações nos controllers dos cadastros.
Desde a #6, Keycloak e MailPit sobem no compose (perfis de execução `dev`/`full`). Os testes de
integração usam JWT RS256 assinado por chave de teste e servido por JWKS local (ADR 0019); a
validação ponta a ponta com o Keycloak está no procedimento do README.

```bash
./mvnw -B clean verify
```

`OrdemDeServicoApiIntegrationTest` estende `AbstractIntegrationTest` e verifica criação,
consultas, preservação da finalização, 400/401/403/404, Swagger e rollback com PostgreSQL real.

## Consulta pública — issue #37

`GET /api/acompanhamento/ordens-de-servico/{id}` não exige autenticação. A oficina informa ao
cliente o UUID retornado na criação administrativa. A resposta contém somente `status` e
`finalizadaEm`; não expõe documento, placa, cliente, itens ou valores. Não verifica propriedade
da OS (ADR 0009). UUID inexistente retorna 404; formato inválido retorna 400.

A liberação pública cobre a consulta e os comandos de aprovação e rejeição do cliente (ADR 0015).
Criação, listagem e detalhe administrativo continuam exigindo JWT e os perfis existentes.

A migration V5 persiste os itens ordenados e o total do orçamento (ADR 0011).
`ItensOrcamentoPersistenceIntegrationTest` verifica gravações sucessivas, valores e identidade
dos itens, detalhe administrativo, isolamento da consulta pública e rollback de gravação.

## Transições — issue #35

A fronteira transacional `OrdemDeServicoService` envolve busca, transição e gravação, utilizando
os casos de uso existentes. Nenhum endpoint recebe status arbitrário. Não há mudanças de schema.
Os perfis seguem o Event Storming, com ADMINISTRADOR autorizado nas três ações.

`TransicoesOrdemDeServicoApiIntegrationTest` valida respostas, permissões, OS ausente, UUID
inválido, transições inválidas/repetidas, preservação de itens/orçamento, horário de conclusão,
Swagger e rollback real de falha na gravação. O estado Em execução é preparado via repositório
nessa suíte; o fluxo completo com aprovação é coberto por `ItensAprovacaoOSApiIntegrationTest`.

## Geração de orçamento — issue #38

`POST /api/ordens-de-servico/{id}/diagnostico/conclusao` não recebe corpo. Mecânico ou administrador
aciona a política que calcula o orçamento a partir dos itens persistidos e muda o status para
Aguardando aprovação. `POST /api/ordens-de-servico/{id}/submissao` faz o mesmo a partir de
Recebida ou Em diagnóstico. O serviço envolve leitura, geração e gravação em uma transação.

OS sem itens retorna 400; estado incompatível (incluindo repetição) retorna 409. OS ausente
retorna 404, UUID inválido 400, JWT ausente 401 e perfil não permitido 403.
Falhas de validação/gravação preservam o estado anterior. A consulta pública continua exibindo
somente andamento. Esses comandos não enviam e-mail nem aprovam a execução.

`GerarOrcamentoApiIntegrationTest` verifica total com serviços/peças, persistência e detalhe,
consulta pública, perfis, estados inválidos, repetição, OS vazia, identificador e rollback real.
Nessa suíte, os itens são preparados via repositório. A suíte `ItensAprovacaoOSApiIntegrationTest`
complementa a verificação pelo fluxo HTTP completo, desde o cadastro no catálogo até a entrega.

## Início da execução e rejeição no detalhe administrativo

O DTO compartilhado das respostas administrativas inclui:

- `iniciadaEm`: instante UTC de início, ou nulo antes da aprovação e em OS legadas sem início conhecido.
- `rejeicaoOrcamento`: objeto com `momento` (UTC) e `motivo`, ou nulo quando não houve rejeição.

Os campos aparecem no detalhe, na listagem e nas respostas dos comandos que já utilizam
`OrdemDeServicoResponse`. Uma OS nova retorna ambos nulos. Rejeição mantém Aguardando aprovação
e não preenche início; conclusão/entrega preservam o início registrado. O motivo segue o valor
normalizado e persistido pelo domínio, sem recalcular ou alterar a decisão na consulta.

Exemplo dos novos campos em uma OS rejeitada:

```json
{
  "iniciadaEm": null,
  "rejeicaoOrcamento": {
    "momento": "2026-10-20T12:00:00.123456Z",
    "motivo": "Preço acima do esperado"
  }
}
```

Continuam valendo JWT e os perfis ATENDENTE, MECANICO e ADMINISTRADOR para as consultas
administrativas. A resposta pública mantém exclusivamente `status` e `finalizadaEm`, inclusive
quando há início ou rejeição registrados. Não há novo comando público de aprovação/rejeição.

`DecisaoOrcamentoApiIntegrationTest` verifica respostas autenticadas de detalhe/listagem,
campos nulos na criação e em OS legadas, preservação após entrega, 401/403 e schemas Swagger.
Nessa suíte, os estados são preparados pelo domínio/repositório. A aprovação com estoque real
é coberta pelas suítes de integração de estoque e de inclusão/aprovação pela API. Rejeição ainda não possui endpoint.

## Inclusão de serviços e peças — issue #34

As rotas `/servicos` e `/pecas` recebem somente o identificador do catálogo e a quantidade
inteira positiva. Não recebem preço ou descrição definidos pelo usuário:

```http
POST /api/ordens-de-servico/{id}/servicos
Content-Type: application/json
Authorization: Bearer <JWT>

{"servicoId": "<UUID do serviço>", "quantidade": 1}
```

```http
POST /api/ordens-de-servico/{id}/pecas
Content-Type: application/json
Authorization: Bearer <JWT>

{"pecaId": "<UUID da peça>", "quantidade": 3}
```

Retornam 200 com a OS atualizada. O nome do catálogo é copiado como descrição do item e o
preço vigente é preservado; alterações futuras no catálogo não mudam o orçamento existente.
A inclusão é permitida em Recebida ou Em diagnóstico, antes da geração do orçamento.
Cada chamada bem-sucedida cria um novo item: repetir um POST de inclusão pode adicionar outro
item/reserva. O contrato idempotente por item do adaptador de estoque não torna o POST idempotente.

Peça exige saldo livre suficiente. A inclusão reserva a quantidade e grava item e vínculo na
mesma transação. A quantidade física (`quantidadeDisponivel`) permanece igual; a reserva aumenta
`quantidadeReservada` e reduz `quantidadeLivre`. Falta de saldo retorna 409 sem incluir item.
Campos ausentes, UUID malformado, quantidade inválida ou referência ausente no catálogo retornam
400. OS ausente retorna 404; orçamento já gerado ou estado incompatível retorna 409.

## Aprovação e rejeição do cliente — issue #40

`POST /api/acompanhamento/ordens-de-servico/{id}/aprovacao` não recebe corpo e não exige JWT.
O cliente aprova a OS. Exige orçamento em Aguardando aprovação, sem rejeição.
Baixa integralmente as reservas correspondentes aos itens PECA, altera para Em execução e
registra `iniciadaEm` usando o relógio UTC do servidor. A resposta pública traz somente
`status` e `finalizadaEm`.

`POST /api/acompanhamento/ordens-de-servico/{id}/rejeicao` recebe `{ "motivo": "..." }`.
Mantém Aguardando aprovação, grava data e motivo e cancela as reservas ativas, devolvendo
a quantidade ao saldo livre. Motivo em branco retorna 400. Rejeição ou aprovação repetida retorna 409.

Exemplo: incluir três unidades de uma peça com saldo físico 10 deixa físico 10, reservado 3,
livre 7. Aprovar deixa físico 7, reservado 0, livre 7. Rejeitar devolve para físico 10, reservado 0,
livre 10. OS com apenas serviços é aprovada sem reservas. Aprovação repetida retorna 409 sem uma segunda baixa.

Reservas ausentes, extras, canceladas ou divergentes impedem aprovação (409).
Falha de persistência reverte a baixa, a devolução e a decisão juntas. Ver ADR 0014 e ADR 0015.

## Executar o fluxo no Swagger

1. Autorizar com JWT de administrador e cadastrar serviço em `/api/servicos` e peça em `/api/pecas`.
2. Abrir a OS, opcionalmente apontando `servicoId` ou `pecaId`, e incluir serviços com MECANICO.
3. Iniciar e concluir o diagnóstico com MECANICO/ADMINISTRADOR, ou submeter a OS recebida.
4. Sem JWT, aprovar ou rejeitar em `/api/acompanhamento/ordens-de-servico/{id}/aprovacao` ou `.../rejeicao`.
5. Consultar a OS e o saldo da peça; concluir a execução com MECANICO e entregar com ATENDENTE.

`IntegracaoEstoqueOSIntegrationTest` verifica rollback, correspondência de reservas,
idempotência por item e concorrência real. `ItensAprovacaoOSApiIntegrationTest` verifica o fluxo
HTTP completo, perfis, validações, aprovação sem peças, rejeição prévia, rollback da baixa e Swagger.

### Evidência local — 2026-10-23

`./mvnw -B clean verify`: BUILD SUCCESS, 481 testes, zero falhas/erros/skips.
JaCoCo: linhas de domain 99,58% (473/475), application 100% (300/300); gate de 80% aprovado.
Relatório reproduzível em `target/site/jacoco/index.html`.

O Swagger foi verificado por HTTP em `/v3/api-docs` nos testes. A exportação pelo plugin Maven
existente tenta acessar uma aplicação em localhost:8080; sem essa aplicação, registra
`Connection refused` e não gera o arquivo, embora o build e os testes sejam aprovados.
Isso não constitui evidência de exportação do JSON durante este comando.
