# ADR 0025 — E-mail ao cliente em cada mudança de status da OS

- Status: Proposto
- Data: 2026-11-18
- Complementa ADR 0007, ADR 0017 e ADR 0018

## Contexto

O cliente só era avisado quando a OS entrava em Aguardando aprovação. A outbox
`notificacao_orcamento` tinha `UNIQUE` em `ordem_de_servico_id` e o insert usava
`ON CONFLICT DO NOTHING`, então qualquer e-mail seguinte da mesma OS era descartado.
A decisão de 18/11/2026 pede um e-mail a cada transição de status, inclusive a rejeição
do orçamento. O link de aprovar ou rejeitar dentro do e-mail continua fora de escopo.

## Decisão

Os casos de uso que criam a OS ou mudam a situação dela chamam `NotificacaoPort.agendar`
depois de salvar, na mesma transação. A situação é o status atual. Na rejeição, o status
permanece Aguardando aprovação e a chave de idempotência é `ORCAMENTO_REJEITADO`.

A migration V13 preenche as linhas já gravadas com `AGUARDANDO_APROVACAO`, remove a
unicidade só da OS e cria `UNIQUE (ordem_de_servico_id, status)`. Repetir a mesma
transição não gera outra linha. Transições diferentes da mesma OS passam a ser gravadas.

O texto depende da situação. O de Aguardando aprovação continua o snapshot de itens e
total, sem link de decisão. Os demais identificam OS, veículo e situação, sem CPF.
O dispatcher segue cancelando somente a solicitação de aprovação ainda não enviada
quando a OS sai de Aguardando aprovação, tem rejeição registrada ou foi excluída.
As outras situações são entregues mesmo que a OS já tenha avançado. Exclusão lógica
continua cancelando pendências.

Não há endpoint novo. As roles e as rotas públicas de acompanhamento permanecem.

## Consequências

- Positivas: o cliente acompanha o ciclo completo pelo MailPit; a fila não silencia
  o e-mail seguinte; a idempotência por situação evita duplicata quando o comando é repetido.
- Negativas: uma OS gera várias linhas e vários envios. A solicitação de aprovação
  ainda pode ser cancelada se a decisão ocorrer antes do SMTP, como no ADR 0018.
  A entrega continua pelo menos uma vez.
- Alternativas descartadas: manter uma linha por OS e sobrescrever o texto (perde o
  histórico e continua impedindo o envio seguinte); broker novo (ADR 0002 e 0007
  mantêm a fila no PostgreSQL).
