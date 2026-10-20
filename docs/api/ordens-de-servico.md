# API de Ordens de Serviço — base da issue #36

O detalhe administrativo inclui itens, orçamento, início da execução e rejeição quando registrados.
A inclusão de itens ainda aguarda endpoints próprios e integração com catálogo/estoque.

## Endpoints

| Método | Rota | Papéis | Comportamento |
|---|---|---|---|
| POST | `/api/ordens-de-servico` | ATENDENTE, ADMINISTRADOR | Abre OS em Recebida; cadastra cliente/veículo ausentes |
| GET | `/api/ordens-de-servico` | ATENDENTE, MECANICO, ADMINISTRADOR | Lista ordenada pelo UUID |
| GET | `/api/ordens-de-servico/{id}` | ATENDENTE, MECANICO, ADMINISTRADOR | Detalhe com itens/orçamento ou 404 |
| POST | `/api/ordens-de-servico/{id}/diagnostico` | MECANICO, ADMINISTRADOR | Recebida → Em diagnóstico |
| POST | `/api/ordens-de-servico/{id}/conclusao` | MECANICO, ADMINISTRADOR | Em execução → Finalizada; registra horário |
| POST | `/api/ordens-de-servico/{id}/entrega` | ATENDENTE, ADMINISTRADOR | Finalizada → Entregue; preserva horário |
| POST | `/api/ordens-de-servico/{id}/orcamento` | ATENDENTE, ADMINISTRADOR | Calcula orçamento; Em diagnóstico → Aguardando aprovação |

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
UUID inválido retorna 400, OS inexistente 404, ausência de JWT 401 e papel não autorizado 403.
Transição inválida, incluindo repetição, retorna 409 com `mensagem`, preservando o estado.
Conclusão usa o relógio UTC do servidor com precisão de microssegundos (PostgreSQL),
e entrega preserva esse momento. Aprovação/início de
execução (#40) ainda não têm endpoints.

## Persistência e transação

A migration V4 cria `ordem_de_servico`, com FKs para cliente/veículo, os seis status permitidos
e consistência entre status e finalização. O agregado é reconstituído sem disparar transições.

`OrdemDeServicoService` envolve a criação inteira em transação. Falha ao cadastrar o veículo
ou ao gravar a OS desfaz os cadastros anteriores. Consultas usam transações read-only.
Exclusão física de cliente/veículo referenciado fica impedida pelas FKs (ADR 0008).

## Segurança e verificação

Swagger em `/swagger-ui.html`, usando o esquema `bearer-jwt` existente. As rotas exigem JWT
e os papéis são verificados por `@PreAuthorize`, sem alterações nos controllers dos cadastros.
O ambiente Keycloak/MailPit continua pendente da #6; os testes usam o JWT assinado do suporte
existente, sem afirmar validação ponta a ponta de login com Keycloak.

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

A liberação é exclusiva para leitura dessa rota. Criação, listagem e detalhe administrativo
continuam exigindo JWT e os papéis existentes. O Swagger documenta a consulta pública sem
esquema de autenticação.

A migration V5 persiste os itens ordenados e o total do orçamento (ADR 0011).
`ItensOrcamentoPersistenceIntegrationTest` verifica gravações sucessivas, valores e identidade
dos itens, detalhe administrativo, isolamento da consulta pública e rollback de gravação.

## Transições — issue #35

A fronteira transacional `OrdemDeServicoService` envolve busca, transição e gravação, utilizando
os casos de uso existentes. Nenhum endpoint recebe status arbitrário. Não há mudanças de schema.
Os papéis seguem o Event Storming, com ADMINISTRADOR autorizado nas três ações.

`TransicoesOrdemDeServicoApiIntegrationTest` valida respostas, permissões, OS ausente, UUID
inválido, transições inválidas/repetidas, preservação de itens/orçamento, horário de conclusão,
Swagger e rollback real de falha na gravação. O estado Em execução é preparado via repositório
nos testes, pois o fluxo de aprovação da #40 ainda está pendente.

## Geração de orçamento — issue #38

`POST /api/ordens-de-servico/{id}/orcamento` não recebe corpo. Atendente ou administrador
aciona o cálculo pelo sistema, exclusivamente a partir de quantidades/preços dos itens
persistidos. Retorna 200 com a OS atualizada, itens e total, em Aguardando aprovação.
O serviço envolve leitura, geração e gravação em uma transação.

OS sem itens retorna 400; estado diferente de Em diagnóstico (incluindo geração repetida)
retorna 409. OS ausente retorna 404, UUID inválido 400, JWT ausente 401 e papel não permitido 403.
Falhas de validação/gravação preservam o estado anterior. A consulta pública continua exibindo
somente andamento. O endpoint não envia e-mail nem aprova/inicia a execução.

`GerarOrcamentoApiIntegrationTest` verifica total com serviços/peças, persistência e detalhe,
consulta pública, papéis, estados inválidos, repetição, OS vazia, identificador e rollback real.
Nos testes, os itens são preparados via repositório: a integração de inclusão com catálogo e
reserva de estoque continua pendente da #34. Isso ainda não demonstra o fluxo completo da oficina.

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

Continuam valendo JWT e os papéis ATENDENTE, MECANICO e ADMINISTRADOR para as consultas
administrativas. A resposta pública mantém exclusivamente `status` e `finalizadaEm`, inclusive
quando há início ou rejeição registrados. Não há novo comando público de aprovação/rejeição.

`DecisaoOrcamentoApiIntegrationTest` verifica respostas autenticadas de detalhe/listagem,
campos nulos na criação e em OS legadas, preservação após entrega, 401/403 e schemas Swagger.
Os estados são preparados pelo domínio/repositório nos testes: a integração real com estoque
e os endpoints de decisão continuam pendentes.
