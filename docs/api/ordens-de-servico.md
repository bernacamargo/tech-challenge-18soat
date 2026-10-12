# API de Ordens de Serviço — base da issue #36

O detalhe administrativo inclui itens persistidos e orçamento, quando gerado. A inclusão de
itens e a geração ainda aguardam endpoints próprios e integração com catálogo/estoque.

## Endpoints

| Método | Rota | Papéis | Comportamento |
|---|---|---|---|
| POST | `/api/ordens-de-servico` | ATENDENTE, ADMINISTRADOR | Abre OS em Recebida; cadastra cliente/veículo ausentes |
| GET | `/api/ordens-de-servico` | ATENDENTE, MECANICO, ADMINISTRADOR | Lista ordenada pelo UUID |
| GET | `/api/ordens-de-servico/{id}` | ATENDENTE, MECANICO, ADMINISTRADOR | Detalhe com itens/orçamento ou 404 |

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
`status`, `finalizadaEm` (nulo até a conclusão), `itens` e `orcamento`.
Cada item contém UUID, referência ao catálogo, tipo, descrição, quantidade, preço unitário e
subtotal. O orçamento contém o total; antes da geração é nulo e OS nova tem itens vazios. Criação retorna 201 e cabeçalho Location.
Veículo já cadastrado deve pertencer ao cliente informado. Não há endpoint de transição nesta
base; os casos de uso da #35 ainda não estão expostos via REST.

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
