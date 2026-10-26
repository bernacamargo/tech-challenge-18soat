# ADR 0015 — Comandos do Event Storming que o código ainda não executava

- Status: Proposto
- Data: 2026-10-23
- Contexto: alinhamento aos quadros em `docs/ddd/event-storming`; complementa ADRs 0009, 0012 e 0014

## Contexto

Os quadros de criação, acompanhamento e gestão de peças descrevem atores e políticas
diferentes dos endpoints que estavam expostos. O orçamento era um comando do atendente.
A aprovação era registrada por atendente ou administrador. A rejeição existia no domínio,
sem rota e sem devolver o estoque. Reserva e baixa também podiam ser chamadas fora da OS.

## Decisão

O mecânico conclui o diagnóstico em `POST /api/ordens-de-servico/{id}/diagnostico/conclusao`.
Com itens, a política calcula o orçamento e muda o status para Aguardando aprovação.
O mecânico também submete a OS em `POST /api/ordens-de-servico/{id}/submissao`, a partir de
Recebida ou Em diagnóstico, com o mesmo efeito. Recebida passa a poder ir direto para
Aguardando aprovação nesse comando. O endpoint `POST /{id}/orcamento` deixa de existir.

Incluir serviço fica com MECANICO e ADMINISTRADOR. Na abertura, o atendente pode apontar
um serviço ou uma peça da lista de disponibilidade; a peça reserva estoque na mesma transação.

O cliente aprova e rejeita sem autenticação, pelo UUID, em
`POST /api/acompanhamento/ordens-de-servico/{id}/aprovacao` e `.../rejeicao`.
Consulta, aprovação e rejeição usam a cadeia stateless já existente, que não tem cookie
de sessão. O CSRF permanece desligado só nela.
A resposta pública continua só com status e finalização. Aprovar baixa as reservas e inicia
a execução. Rejeitar mantém Aguardando aprovação, grava motivo e hora, e cancela as reservas
ativas para a quantidade voltar ao saldo livre.

As rotas `POST /api/estoque/reservas` e `POST /api/estoque/ordens-de-servico/{id}/baixa`
são removidas. Reserva e baixa permanecem políticas da inclusão da peça e do início da execução.

## Consequências

- Quem conhece o UUID pode aprovar ou rejeitar a OS, no mesmo limite já aceito para a consulta pública.
- A rejeição não cria status novo e não reabre o orçamento para ajuste.
- A submissão direta não passa por Em diagnóstico.
- Não há e-mail de envio do orçamento: o envio para aprovação é a mudança de status.
