# API de Ordens de Serviço — base da issue #36

Esta implementação é parcial: itens e orçamento dependem das issues #34 e #38. A issue #36
permanece pendente até esses dados integrarem persistência e detalhamento. Não há campos
fictícios de itens/orçamento na resposta atual.

## Endpoints

| Método | Rota | Papéis | Comportamento |
|---|---|---|---|
| POST | `/api/ordens-de-servico` | ATENDENTE, ADMINISTRADOR | Abre OS em Recebida; cadastra cliente/veículo ausentes |
| GET | `/api/ordens-de-servico` | ATENDENTE, MECANICO, ADMINISTRADOR | Lista ordenada pelo UUID |
| GET | `/api/ordens-de-servico/{id}` | ATENDENTE, MECANICO, ADMINISTRADOR | Detalhe básico ou 404 |

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
`status` e `finalizadaEm` (nulo até a conclusão). Criação retorna 201 e cabeçalho Location.
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
