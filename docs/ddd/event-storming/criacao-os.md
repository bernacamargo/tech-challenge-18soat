# Event Storming — Criação da Ordem de Serviço

> Rascunho consolidado do Miro. Um comando por linha; validar com o grupo.

| Comando | Ator | Evento | Política / regra |
|---|---|---|---|
| Identificar cliente | Atendente | `ClienteIdentificado` | Cliente novo → comando `CadastrarCliente` (CpfCnpj validado) |
| Cadastrar cliente | Atendente | `ClienteCadastrado` | CpfCnpj único |
| Cadastrar veículo | Atendente | `VeiculoCadastrado` | Placa validada (Mercosul/antigo); vinculado ao cliente |
| Criar OS | Atendente | `OSCriada` | Status inicial: **Recebida** |
| Incluir serviço | Atendente | `ServicoAdicionado` | Serviço vem do catálogo |
| Incluir peça | Atendente/Mecânico | `PecaReservada` | Regra: estoque deve ter quantidade disponível |
| Registrar diagnóstico | Mecânico | `DiagnosticoRegistrado` | Transição: Recebida → **Em diagnóstico** |
| Gerar orçamento | Sistema | `OrcamentoGerado` | Automático: Σ serviços + Σ peças (Dinheiro); transição → **Aguardando aprovação** |
| Enviar orçamento | Sistema/Atendente | `OrcamentoEnviado` | E-mail ao cliente via SMTP local — MailPit (ADR-0007); disponibilizado para aprovação |
| Aprovar orçamento | Cliente | `OrcamentoAprovado` | Transição → **Em execução** |
| Rejeitar orçamento | Cliente | `OrcamentoRejeitado` | OS volta a Aguardando aprovação após ajuste, ou é encerrada |

## Relação com a implementação — issue #33

`CriarOrdemDeServico` reutiliza `IdentificarCliente` e `CadastrarVeiculo`: identifica ou cadastra
o cliente, consulta o veículo pela placa e cadastra-o quando ausente. Veículo já cadastrado é
reutilizado apenas se pertencer ao cliente informado; outro proprietário impede a abertura da OS.
A OS nasce em `RECEBIDA`, com UUID próprio e documento/placa normalizados, e é salva pelo port
`OrdemDeServicoRepositorio`. O caso de uso fica em `application/usecase` e o port em
`application/port`; os testes espelham o pacote do caso de uso e usam ports fake, sem Spring ou JPA.

Esta etapa implementa somente a aplicação. A integração de persistência (#36) deverá envolver o
fluxo inteiro em uma transação, incluindo os cadastros de cliente/veículo, para garantir rollback
se qualquer gravação falhar. Os fakes desta etapa não simulam rollback.
