# Event Storming — Gestão de Peças e Insumos

Quadro da imagem `gestao-pecas.png`.

Legenda: azul é comando, laranja é evento, roxo é política e verde é modelo de leitura. O ator fica no post-it amarelo do comando. As políticas não têm ator: reagem ao evento à esquerda.

```mermaid
flowchart LR
  classDef comando fill:#7dd3fc,stroke:#0369a1,color:#0c4a6e
  classDef evento fill:#fcd34d,stroke:#b45309,color:#78350f
  classDef politica fill:#d8b4fe,stroke:#6b21a8,color:#3b0764
  classDef leitura fill:#86efac,stroke:#166534,color:#14532d

  consulta["Consulta de Estoque disponível"]:::leitura
  cadastrar["Cadastrar peça/insumo<br/>Administrador"]:::comando
  pecaCadastrada["Peça cadastrada"]:::evento
  atualizar["Atualizar Estoque<br/>entrada de mercadoria<br/>Administrador"]:::comando
  estoqueEntrada["Estoque atualizado"]:::evento

  pecaIncluida["Peça incluída na Ordem de Serviço"]:::evento
  reservar["Reservar quantidade no estoque"]:::politica
  pecaReservada["Peça reservada"]:::evento
  insuficiente["Estoque insuficiente"]:::evento

  execIniciada["Execução de serviço iniciada"]:::evento
  baixa["Dar baixa efetiva no estoque reservado"]:::politica
  baixado["Estoque baixado"]:::evento

  osRejeitada["OS Rejeitada"]:::evento
  devolver["Peças e insumos voltam ao estoque"]:::politica
  estoqueDevolvido["Estoque atualizado"]:::evento

  consulta --> cadastrar --> pecaCadastrada
  atualizar --> estoqueEntrada
  pecaIncluida --> reservar --> pecaReservada
  reservar --> insuficiente
  execIniciada --> baixa --> baixado
  osRejeitada --> devolver --> estoqueDevolvido
```

## Cadastro e entrada

| Tipo | Rótulo | Ator | Encadeamento |
|---|---|---|---|
| Modelo de leitura | Consulta de Estoque disponível | | → Cadastrar peça/insumo |
| Comando | Cadastrar peça/insumo | Administrador | → Peça cadastrada |
| Evento | Peça cadastrada | | fim deste trecho |
| Comando | Atualizar Estoque (entrada de mercadoria) | Administrador | → Estoque atualizado |
| Evento | Estoque atualizado | | fim da entrada |

## Reserva, baixa e devolução

| Tipo | Rótulo | Ator | Encadeamento |
|---|---|---|---|
| Evento | Peça incluída na Ordem de Serviço | | dispara a reserva |
| Política | Reservar quantidade no estoque | | → Peça reservada ou Estoque insuficiente |
| Evento | Peça reservada | | reserva concluída |
| Evento | Estoque insuficiente | | ramo alternativo da mesma política |
| Evento | Execução de serviço iniciada | | dispara a baixa |
| Política | Dar baixa efetiva no estoque reservado | | → Estoque baixado |
| Evento | Estoque baixado | | fim da baixa |
| Evento | OS Rejeitada | | dispara a devolução |
| Política | Peças e insumos voltam ao estoque | | → Estoque atualizado |
| Evento | Estoque atualizado | | fim da devolução |
