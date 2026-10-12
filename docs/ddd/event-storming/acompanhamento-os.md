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

## Relação com a implementação — issue #32

- `atendimento/domain/OrdemDeServico` nasce em `RECEBIDA` e concentra a validação das cinco
  transições da tabela, através de `transicionarPara`. Repetições, retrocessos e saltos são rejeitados
  sem alterar o estado atual; `ENTREGUE` é terminal.
- `StatusOS` representa os seis estados. A OS possui identificador UUID e referencia cliente e
  veículo por seus identificadores (documento e placa), sem depender das classes desses cadastros.
  O caso de uso de criação verificará a existência e o vínculo entre cliente e veículo (#33).
- `OrdemDeServicoTest` cobre todas as 36 combinações entre estados, além do estado inicial e
  dos identificadores obrigatórios, sem Spring ou banco de dados.
- Orçamento, registro de rejeição, horários de execução e casos de uso serão tratados nas issues
  seguintes. As transições implementadas aqui validam o ciclo de vida; ainda não verificam itens,
  aprovação de orçamento ou baixa de estoque.
