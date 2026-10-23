# ADR-0010: Contratos de catálogo e reserva consumidos por Atendimento

- **Status**: Proposto
- **Data**: 2026-10-12
- **Participantes**: Proposta para revisão do Grupo 117 (issue #34)

## Contexto

Atendimento precisa incluir serviços e peças sem depender de entidades de Catálogo/Estoque.
Os PRs desses contextos ainda não estão disponíveis. Consultar saldo antes de reservar criaria
uma janela de concorrência, e alterações posteriores no preço não devem mudar itens existentes.

## Decisão

Definir ports na aplicação de Atendimento para consulta de catálogo e reserva atômica. Usar
uma projeção com UUID, descrição e BigDecimal como contrato proposto. Manter preço/descrição
no item imutável da OS e restringir inclusão aos estados anteriores ao orçamento.

O port de reserva recebe UUID da OS e do item para permitir identificação da operação. Reserva
e gravação deverão ser integradas na mesma transação Postgres; somente a execução efetua baixa.
Sem adaptadores reais, os casos de uso ficam sem wiring Spring/REST.

## Consequências

- Testes independentes de Spring e dos contextos externos; integração explícita por ports.
- Contratos e identificadores precisam de alinhamento com os responsáveis por Catálogo/Estoque.
- Persistência de itens e transação real são necessárias antes do uso destes casos com JPA.
- Snapshot de preço requer novos campos persistidos; não reutilizar preço atual do catálogo
  para recalcular itens antigos.
- Alternativa descartada: importar entidades externas ou simular estoque produtivo no Atendimento.

## Evolução em 2026-10-23

O [ADR 0014](0014-integracao-atendimento-catalogo-estoque.md) complementa esta decisão com
adaptadores reais, vínculo por item, transação conjunta e locks. A API inclui serviços/peças
e permite a ATENDENTE/ADMINISTRADOR registrar aprovação e iniciar execução. Os limites
descritos acima refletem a etapa original; rejeição por REST e revisão continuam pendentes.
