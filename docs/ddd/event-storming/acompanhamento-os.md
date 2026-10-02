# Event Storming — Acompanhamento da Ordem de Serviço

> Rascunho consolidado do Miro. Estados e transições da OS.

## Estados (StatusOS)

```
Recebida → Em diagnóstico → Aguardando aprovação → Em execução → Finalizada → Entregue
```

## Transições

| De | Evento/Comando | Para | Quem |
|---|---|---|---|
| Recebida | `DiagnosticoRegistrado` | Em diagnóstico | Mecânico |
| Em diagnóstico | `OrcamentoGerado` | Aguardando aprovação | Sistema |
| Aguardando aprovação | `OrcamentoAprovado` | Em execução | Cliente (via API) |
| Em execução | `ServicosConcluidos` | Finalizada | Mecânico/Sistema |
| Finalizada | `RegistrarEntrega` | Entregue | Atendente |

Regras:
- Transições inválidas são rejeitadas no domínio (máquina de estados no agregado `OrdemDeServico`).
- Alteração automática de status conforme ações no sistema (políticas acima).
- O **cliente consulta o andamento via API** (endpoint público de acompanhamento por OS).
- Sistema calcula o **tempo médio de execução** entre `OSIniciada` (Em execução) e `OSFinalizada`.
