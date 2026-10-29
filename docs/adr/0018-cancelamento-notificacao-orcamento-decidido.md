# ADR 0018 — Cancelar solicitações de aprovação após decisão do orçamento

- Status: Proposto
- Data: 2026-10-29
- Complementa ADR 0017

## Contexto

Uma notificação pode aguardar contato ou retry SMTP enquanto o cliente já aprova ou rejeita
o orçamento. Enviar posteriormente uma solicitação de aprovação transmite um estado obsoleto.
A seleção de pendências precisa considerar a OS e coordenar-se com suas decisões concorrentes.

## Decisão

A migration V10 adiciona `cancelada_em` à outbox e restringe o índice parcial aos registros
sem envio nem cancelamento. Preservar snapshots e tentativas; não remover o histórico nem
marcar um cancelamento como envio. Uma constraint impede ter envio e cancelamento simultâneos.
Registros existentes permanecem intactos, com cancelamento nulo.

Na próxima tentativa agendada, o dispatcher cancela a notificação quando a OS já não está
em Aguardando aprovação ou tem rejeição registrada. Não chamar SMTP nem incrementar tentativas
nesse caso. A ausência de e-mail não impede o cancelamento. Notificações canceladas não são
selecionadas novamente. Mensagens já entregues permanecem com o registro original.

A consulta adquire `FOR UPDATE OF os, n SKIP LOCKED`: o lock da OS é compartilhado com o
protocolo já usado pelos comandos de decisão. Um comando em andamento faz o worker pular
aquela OS; após o commit, a próxima tentativa vê o estado confirmado. Se o worker já começou,
a aprovação/rejeição aguarda o término do envio e da transação. Dois workers não processam
a mesma linha simultaneamente. Não se promete impedir um e-mail iniciado antes da decisão.

## Consequências

Mantém-se a entrega pelo menos uma vez para orçamentos ainda aguardando decisão, com retries
existentes. O cancelamento pode aguardar o prazo já agendado de retry, mas não gera outro envio.
Locks da OS permanecem durante o SMTP, limitado pelos timeouts existentes; isso pode atrasar
uma decisão enquanto há envio em andamento. Não há broker, novo status público ou mudança
nas permissões de aprovação/rejeição. Para novos volumes, reavaliar lotes e claim/lease.

## Validação

Regressões de aprovação/rejeição antes do cadastro de contato e após falha SMTP, exclusão
mútua com decisão concorrente, migração V9 → V10 preservando mensagens e transporte SMTP real
na suíte existente. As evidências da execução constam na PR.
