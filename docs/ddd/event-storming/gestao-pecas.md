# Event Storming — Gestão de Peças e Insumos

> Rascunho consolidado do Miro. Contexto Catálogo e Estoque.

## Comandos e eventos

| Comando | Ator | Evento | Política / regra |
|---|---|---|---|
| Cadastrar peça | Administrador | `PecaAdicionada` | CRUD administrativo (JWT) |
| Cadastrar serviço | Administrador | `ServicoAdicionado` | CRUD administrativo (JWT) |
| Dar entrada no estoque | Administrador | `EstoqueReabastecido` | Aumenta quantidade disponível |
| Dar baixa no estoque | Sistema | `EstoqueBaixado` | Ao iniciar execução da OS (consome reserva) |
| Reservar peça para OS | Sistema | `PecaReservada` | Regra: disponibilidade suficiente; senão, bloqueia orçamento |
| Remover peça da OS | Atendente/Mecânico | `ReservaCancelada` | Devolve quantidade ao disponível |

## Regras de negócio

- A peça sai do estoque quando o **serviço é iniciado** (reserva → baixa), não quando é orçada.
- Quantidade reservada não pode exceder o disponível (consistência transacional no Postgres — ADR 0002).
- Administração de peças/serviços exige **JWT** (contexto Identidade e Acesso).
