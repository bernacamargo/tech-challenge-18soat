# ADR-0009: Consulta pública do andamento da OS por UUID

- **Status**: Proposto
- **Data**: 2026-10-12
- **Participantes**: Proposta para revisão do Grupo 117; consulta sem autenticação solicitada por Frederico

## Contexto

A issue #37 exige consulta do cliente ao andamento via API, sem definir o mecanismo de
identificação. O UUID da OS já é retornado pela abertura administrativa. A orientação para
esta implementação é não exigir validação da identidade do cliente.

## Decisão

Expor `GET /api/acompanhamento/ordens-de-servico/{id}` sem autenticação, usando o UUID informado
para localizar a OS. Retornar apenas `status` e `finalizadaEm`, sem documento, placa, dados do
cliente, itens ou valores. UUID inexistente retorna 404; formato inválido retorna 400.

Reutilizar o caso de uso de detalhe pela fachada read-only existente. Configurar uma cadeia
Spring Security específica para a rota pública, permitindo somente GET; manter inalterados
os arquivos de segurança existentes e a exigência de JWT/roles nas rotas administrativas.

## Consequências

- Cliente acompanha a OS sem login ou token administrativo.
- A oficina precisa informar o UUID da OS ao cliente; não há envio automático nesta issue.
- Qualquer pessoa que conheça o UUID pode consultar esse andamento; não há comprovação de
  propriedade. Essa é uma escolha explícita para o MVP, limitada à projeção pública.
- A emissão de token de acompanhamento foi descartada conforme a orientação recebida.
