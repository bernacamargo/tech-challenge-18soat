# ADR-0024 — Exclusão lógica de OS pelo administrador

- **Status** — Proposto
- **Data** — 2026-11-18

## Contexto

A oficina precisa excluir OS com autorização exclusiva do perfil ADMINISTRADOR e preservar
os registros históricos. Uma OS pode ter itens, reservas de estoque e notificações de orçamento.

## Decisão

Expor DELETE /api/ordens-de-servico/{id} com autorização ADMINISTRADOR na regra de URL e
no controller. Retornar 204 na exclusão, 401 sem autenticação, 403 para outros perfis e 404
para OS inexistente ou já excluída.

Adicionar a coluna técnica excluida_em com migration V12. OS existentes permanecem ativas,
com a coluna nula. A exclusão preserva o status de negócio, os itens, as reservas e os vínculos.
Esse metadado administrativo fica na persistência, e o domínio mantém os estados do atendimento.

O caso de uso verifica a existência, cancela reservas ativas e solicita a exclusão lógica.
Portas de aplicação separam o cancelamento de reservas da marcação na persistência.
Reutilizar o serviço do contexto de estoque para liberar somente reservas ativas.
As quantidades de reservas já baixadas permanecem consumidas.

Uma transação envolve a liberação de estoque, a marca de exclusão e o cancelamento dos e-mails
pendentes. O lock da OS serializa a exclusão com os comandos de atendimento e o envio de
notificações. Os locks das peças seguem a ordem crescente do UUID textual.

O adaptador de consultas omite OS excluídas da busca individual e da listagem. O lock dos
comandos também verifica a marca de exclusão. Salvar novamente um agregado anteriormente
carregado de uma OS excluída retorna erro de OS inexistente. A fila de notificações cancela
mensagens de OS excluídas também durante seu processamento.

## Consequências

- Todas as situações de negócio permitem exclusão administrativa.
- Consultas administrativas e públicas e comandos passam a responder 404 para OS excluídas.
- Repetir a exclusão retorna 404 e preserva seu efeito.
- Falha em qualquer etapa reverte a liberação de estoque e a exclusão juntas.
- Os registros continuam disponíveis no banco para histórico.
