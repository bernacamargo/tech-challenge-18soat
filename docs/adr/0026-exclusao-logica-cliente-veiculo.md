# ADR 0026 — Exclusão lógica de cliente e veículo

- Status: Proposto
- Data: 2026-11-21
- Complementa ADR 0008 e ADR 0024

## Contexto

O DELETE de cliente e de veículo apaga a linha. A chave estrangeira da OS, e a do veículo
no caso do cliente, responde 409 quando ainda há vínculo. O histórico precisa permanecer
no PostgreSQL, no mesmo espírito da exclusão lógica da OS.

## Decisão

A migration V13 adiciona `excluida_em` em `cliente` e `veiculo`. O DELETE permitido grava
o instante e não remove a linha. Repetir a exclusão responde 404, porque listagens e buscas
ignoram a marca preenchida.

O caso de uso consulta `VinculoDeCadastro` antes de marcar. Cliente com veículo ainda
visível, ou com qualquer OS, responde 409 com a mesma mensagem de vínculo. Veículo com
qualquer OS também. Veículo já excluído não impede excluir o cliente. OS excluída
logicamente continua sendo vínculo: a linha histórica permanece.

Não há role nova. A regra de URL do `SecurityConfig` segue exigindo ATENDENTE ou
ADMINISTRADOR. MECANICO continua recebendo 403.

Reutilizar o documento ou a placa de um cadastro excluído responde 409 de cadastro já
existente. A linha histórica não volta a aparecer.

## Consequências

- O histórico de cliente e veículo permanece para as FKs da OS.
- Consultas administrativas deixam de devolver cadastros excluídos.
- A ordem de exclusão sem OS continua sendo veículo e depois cliente.
- A autorização não muda.
