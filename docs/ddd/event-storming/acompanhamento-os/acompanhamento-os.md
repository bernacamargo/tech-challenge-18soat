# Event Storming — Acompanhamento da Ordem de Serviço

Quadro da imagem `acompanhamento-os.png`.

Legenda: azul é comando, laranja é evento, roxo é política e verde é modelo de leitura. O ator fica no post-it amarelo do comando.

```mermaid
flowchart TB
  classDef comando fill:#7dd3fc,stroke:#0369a1,color:#0c4a6e
  classDef evento fill:#fcd34d,stroke:#b45309,color:#78350f
  classDef politica fill:#d8b4fe,stroke:#6b21a8,color:#3b0764
  classDef leitura fill:#86efac,stroke:#166534,color:#14532d

  subgraph etapaAbertura["1. Abertura"]
    direction LR
    osAberta["OS Aberta<br/>status: Recebida"]:::evento
  end

  subgraph etapaDiagnostico["2. Diagnóstico pelo mecânico"]
    direction LR
    iniciar["Iniciar diagnóstico<br/>Mecânico"]:::comando
    diagIniciado["Diagnóstico iniciado<br/>status: Em Diagnóstico"]:::evento
    concluirDiag["Concluir diagnóstico<br/>Mecânico"]:::comando
    diagConcluido["Diagnóstico concluído"]:::evento
    gerar["Ao concluir diagnóstico com itens,<br/>gerar orçamento e mudar status"]:::politica
    iniciar --> diagIniciado --> concluirDiag --> diagConcluido --> gerar
  end

  subgraph etapaAprovacao["3. Aprovação do orçamento pelo cliente"]
    direction LR
    aguardando["OS aguardando aprovação<br/>status: Aguardando Aprovação"]:::evento
    aprova["Aprova OS<br/>Cliente"]:::comando
    rejeita["Rejeita OS<br/>Cliente"]:::comando
    aprovado["Orçamento aprovado"]:::evento
    libera["Aprovação libera execução<br/>automaticamente"]:::politica
    aguardando --> aprova --> aprovado --> libera
    aguardando --> rejeita
  end

  subgraph etapaExecucao["4. Execução e entrega"]
    direction LR
    emExecucao["Execução iniciada<br/>status: Em execução"]:::evento
    conclui["Conclui execução<br/>Mecânico"]:::comando
    finalizada["Execução concluída<br/>status: Finalizada"]:::evento
    entrega["Registrar entrega<br/>Atendente"]:::comando
    entregue["OS Entregue<br/>status: Entregue"]:::evento
    emExecucao --> conclui --> finalizada --> entrega --> entregue
  end

  subgraph etapaConsulta["Consulta pelo cliente"]
    direction LR
    consultar["Consultar status da OS (API)<br/>Cliente"]:::comando
    statusAtual["Status atual da OS"]:::leitura
    consultar --> statusAtual
  end

  etapaAbertura --> etapaDiagnostico
  etapaDiagnostico --> etapaAprovacao
  etapaAprovacao --> etapaExecucao
  etapaAbertura --> etapaConsulta
  etapaExecucao --> etapaConsulta
```

## Ciclo de status

```
Recebida → Em Diagnóstico → Aguardando Aprovação → Em execução → Finalizada → Entregue
```

`Rejeita OS` parte de Aguardando Aprovação. O quadro não desenha o evento seguinte a essa rejeição.

| Tipo | Rótulo | Ator | Encadeamento |
|---|---|---|---|
| Evento | OS Aberta (status: Recebida) | | → Iniciar diagnóstico |
| Comando | Iniciar diagnóstico | Mecânico | → Diagnóstico iniciado (status: Em Diagnóstico) |
| Evento | Diagnóstico iniciado (status: Em Diagnóstico) | | → Concluir diagnóstico |
| Comando | Concluir diagnóstico | Mecânico | → Diagnóstico concluído |
| Evento | Diagnóstico concluído | | dispara a política de orçamento |
| Política | Ao concluir diagnóstico com itens, gerar orçamento e mudar status | | → OS aguardando aprovação (status: Aguardando Aprovação) |
| Evento | OS aguardando aprovação (status: Aguardando Aprovação) | | → Aprova OS ou Rejeita OS |
| Comando | Aprova OS | Cliente | → Orçamento aprovado |
| Evento | Orçamento aprovado | | dispara a liberação da execução |
| Política | Aprovação libera execução automaticamente | | → Execução iniciada (status: Em execução) |
| Comando | Rejeita OS | Cliente | sem evento seguinte no quadro |
| Evento | Execução iniciada (status: Em execução) | | → Conclui execução |
| Comando | Conclui execução | Mecânico | → Execução concluída (status: Finalizada) |
| Evento | Execução concluída (status: Finalizada) | | → Registrar entrega |
| Comando | Registrar entrega | Atendente | → OS Entregue (status: Entregue) |
| Evento | OS Entregue (status: Entregue) | | fim do ciclo |

## Consulta

O cliente consulta o andamento em qualquer ponto do ciclo desenhado no quadro. A leitura devolve o status atual.

| Tipo | Rótulo | Ator | Encadeamento |
|---|---|---|---|
| Comando | Consultar status da OS (API) | Cliente | lê o status atual; o quadro liga Recebida, Em execução, Finalizada e Entregue a esta consulta |
| Modelo de leitura | Status atual da OS | | resultado da consulta |
