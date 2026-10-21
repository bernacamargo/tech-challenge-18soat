# ADR 0012 — Decisão sobre orçamento e início da execução

- Status: Proposto
- Data: 2026-10-20
- Contexto: issue #40; prepara o registro temporal necessário à #39

## Contexto

A aprovação deve consumir as reservas de estoque e iniciar a execução. O enunciado não fixa
o estado posterior à rejeição; o Event Storming admite ajuste ou encerramento. Não há status
de cancelamento nem versionamento de orçamentos no modelo atual.

## Decisão

Somente orçamento gerado em Aguardando aprovação pode receber decisão. Aprovar registra
`iniciadaEm` e altera a OS para Em execução, preservando itens/preços e orçamento. O domínio
rejeita conclusão anterior ao início conhecido da execução.

Rejeitar registra data e motivo em um value object e mantém Aguardando aprovação. A rejeição
bloqueia aprovação, nova rejeição e início pela transição genérica daquele orçamento. Não cria
status adicional, não inicia execução e não dá baixa no estoque. A revisão/reabertura de
orçamentos rejeitados é um fluxo posterior: esta entrega não libera alterações nos itens
orçados nem apaga a decisão registrada.

O caso de uso de aprovação valida o agregado e obtém o momento antes de chamar um port de
baixa definido em Atendimento. Somente após sucesso da baixa aplica a aprovação e salva a OS.
O adaptador futuro deve assegurar consumo completo das reservas correspondentes aos itens,
idempotência e a mesma transação PostgreSQL da gravação da OS. Não haverá compensação fictícia
com fakes nem acoplamento do domínio às entidades de estoque.

## Consequências e limites

Os casos de uso são preparados com ports fake, sem beans Spring ou endpoints. A persistência
do início e da rejeição foi complementada pelo ADR 0013. O wiring produtivo ainda depende do
adaptador de baixa da #30 (PR #51/#52), transação conjunta e autorização do cliente.
A revisão de orçamento e o destino/liberação das reservas após rejeição precisam ser definidos
no fluxo de ajuste; não se presume que o port atual de estoque já ofereça essas operações.

As transições genéricas anteriores continuam sendo infraestrutura de ciclo de vida e não
substituem o caso de uso de aprovação; nenhum endpoint novo expõe status arbitrário.
O cliente ainda não pode aprovar/rejeitar por REST. Autorização desse fluxo será definida antes
da exposição; a liberação pública da consulta não é autorização para comandos.

## Evidências

Testes de domínio verificam decisões, estados, horário, rejeição repetida, aprovação após
rejeição e imutabilidade do orçamento. Testes de aplicação verificam ordem de chamadas, baixa
rejeitada e ausência de efeitos externos quando a validação falha.
