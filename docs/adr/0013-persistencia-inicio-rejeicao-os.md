# ADR 0013 — Persistência do início da execução e da rejeição do orçamento

- Status: Proposto
- Data: 2026-10-20
- Contexto: evolução da issue #40 e registro dos dados de duração para observabilidade
- Complementa: ADRs 0011 e 0012

## Contexto

O domínio já registra início e rejeição, mas o adaptador JPA perdia esses dados ao salvar e
reabrir uma OS. Isso removia o bloqueio de orçamento rejeitado e impediria calcular corretamente
a duração da execução. A versão V6 de migration está reservada na PR #52 de estoque.

## Decisão

Adicionar `iniciada_em` e `orcamento_rejeitado_em` como TIMESTAMPTZ e `motivo_rejeicao` como TEXT
em `ordem_de_servico`. Data e motivo são um único value object no domínio; no banco devem estar
ambos nulos ou ambos preenchidos, com motivo contendo conteúdo além de espaços.

Usar a migration incremental `V5_1` (versão 5.1 no histórico do Flyway), após V5 dos itens e antes da V6 de estoque,
sem alterar migrations existentes nem reutilizar versão de outra PR. Em bancos que seguem a sequência da main, não exige executar migrations fora de ordem
quando o estoque for integrado depois. Ambientes que já executaram a V6 isoladamente devem
ter a sequência de migração alinhada antes de integrar esta versão.

O início informado exige orçamento e status Em execução, Finalizada ou Entregue. Finalização
conhecida não pode anteceder o início. A rejeição exige orçamento, status Aguardando aprovação
e ausência de início, conforme ADR 0012. Constraints PostgreSQL protegem essas invariantes
mesmo para escrita direta no banco.

Manter colunas nulas para OS legadas, inclusive finalizadas/entregues, sem inventar instante de
início, rejeição ou orçamento. Os métodos de restauração anteriores continuam compatíveis.
`iniciada_em` e `finalizada_em` registram a duração de uma OS e não alimentam o tempo médio.
Esta entrega não implementa cálculo nem endpoint dessa duração. O tempo médio de execução
dos serviços é a latência das rotas no Grafana (ADR 0022).

Reconstituir o agregado com os dados completos sem executar ações de aprovação, rejeição,
transições ou baixa de estoque. O mapeamento normaliza os novos instantes para microssegundos
antes de salvar, evitando diferenças entre o retorno da gravação e a leitura do PostgreSQL.
Motivos são preservados como normalizados pelo value object, sem limite artificial de 255 caracteres.

## Consequências e limites

Gravações sucessivas, conclusão e entrega preservam o início; rejeição restaurada continua
bloqueando aprovação e nova rejeição. Persistência de status, decisão e itens ocorre na mesma
transação do adaptador, com rollback em falha de gravação.

Não conecta os casos de uso da decisão ao Spring/REST nem cria adaptador de baixa. A chamada ao
estoque e a gravação da OS ainda precisam ser integradas numa transação real, com autorização
definida para os comandos do cliente. Testes com um port fake de baixa não provam transação,
idempotência ou concorrência do estoque produtivo. A consulta pública não expõe os novos dados. A camada de API administrativa os apresenta
por DTOs próprios, conforme `docs/api/ordens-de-servico.md`, sem alterar comandos de decisão.

## Evidências

Testes unitários cobrem restauração válida/inválida e compatibilidade com estados legados.
Testes com PostgreSQL cobrem ciclo após releitura, precisão temporal, motivo longo, constraints,
rollback e atualização de um schema V5 com registros existentes para V5.1. A consulta pública
permanece limitada a status e finalização, sem exposição do motivo de rejeição.
