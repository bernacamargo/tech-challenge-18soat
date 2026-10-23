# ADR 0014 — Integração transacional de Atendimento com Catálogo/Estoque

- Status: Proposto
- Data: 2026-10-23
- Contexto: conclusão da integração das issues #34/#40; complementa ADRs 0010–0013

## Contexto

Os ports de Atendimento recebem o ID do item para identificar a reserva. O port mesclado de
Estoque recebe OS/peça/quantidade e retorna o ID da reserva, e sua baixa consome as reservas da
OS. Conectar diretamente os contratos perderia a identificação por item e permitiria aprovar
uma OS sem comprovar que as reservas correspondem às peças do orçamento.

## Decisão

Implementar adaptadores de integração na infraestrutura de Atendimento, sem alterar os
arquivos de Catálogo/Estoque. Consultar seus repositórios por interfaces e projetar nome e
preço vigente em `ItemCatalogo`. O nome é a descrição do item; preços/nomes registrados na OS
não acompanham alterações posteriores do catálogo.

A migration V8 cria `reserva_item_os` com item, OS, peça, quantidade e reserva. O ID do item é
chave primária e a reserva é única. FK para a OS pertence a Atendimento; a FK para o item é
DEFERRABLE INITIALLY DEFERRED, pois a reserva precede a inclusão/gravação do item na mesma
transação. Isso também permite ao Hibernate regravar a coleção sem apagar vínculos. Não há
FK para tabelas do outro contexto; sua correspondência é conferida pelo adaptador.

Uma nova chamada do port com o mesmo item e parâmetros retorna a reserva confirmada sem
reservar de novo; parâmetros divergentes são recusados. Esse contrato NÃO torna requisições
HTTP idempotentes: cada inclusão bem-sucedida cria um novo item. Falta de saldo retorna false
antes de chamar o port externo, sob lock da peça, sem efeitos parciais.

Antes da baixa, exigir correspondência exata entre itens PECA, vínculos e reservas: IDs,
peças e quantidades. Recusar reservas extras, ausentes, canceladas, parciais ou divergentes.
Reservas todas BAIXADAS e correspondentes representam operação já confirmada e não são
consumidas novamente; ausência de reservas para peças não equivale a baixa concluída.
OS somente com serviços não exige reservas. Conferir a contagem e o estado após a baixa.

A fronteira Spring em Atendimento envolve busca/validação, reserva/baixa e gravação da OS em
uma transação PostgreSQL; os serviços de Estoque participam pela propagação REQUIRED já
existente. Os adaptadores exigem transação MANDATORY. Falha em item, vínculo, saldo, reserva
ou OS reverte as gravações conjuntas. Eventos existentes do estoque continuam publicados pelo
contexto; consumidores que executem efeitos externos deverão respeitar confirmação da transação.

Serializar os comandos de Atendimento por lock da linha da OS. Bloquear peças e atualizar o
cache JPA sob o lock para consultar saldos/versões atuais; múltiplas peças são bloqueadas por
UUID em ordem estável. As transições administrativas existentes usam o mesmo lock da OS.
Isso protege inclusões/geração/aprovação concorrentes sem remover o lock otimista de Estoque.
Operações manuais da API de estoque continuam com seu contrato próprio: este PR não redefine
nem autoriza seus comandos; divergências detectadas impedem aprovação por Atendimento.

## Exposição e autorização

A camada REST terá inclusão de serviço para ATENDENTE/ADMINISTRADOR, peça para
ATENDENTE/MECANICO/ADMINISTRADOR e registro de aprovação para ATENDENTE/ADMINISTRADOR.
O usuário confirmou que esses papéis registrarão a aprovação do cliente nesta etapa.
Não há comando público de aprovação nem mudança na consulta pública. A aprovação e o início
da execução são uma única operação, com instante UTC na precisão de microssegundos do banco.

## Limites

Reservas antigas sem vínculo não são associadas silenciosamente a itens. Exigem reconciliação
antes da aprovação. Cancelamento, revisão de orçamento, liberação de reservas e envio de e-mail
não fazem parte desta entrega. A métrica de duração continua no trabalho de observabilidade.
Não altera o domínio nem expõe entidades do contexto externo na aplicação de Atendimento.

## Evidências

Testes PostgreSQL cobrem preços preservados, reserva sem baixa, consumo das reservas e início
persistido, OS apenas com serviços, saldo insuficiente, vínculos divergentes, repetição do port,
FK diferida, rollback de inclusão/aprovação e operações concorrentes na mesma OS ou peça.
