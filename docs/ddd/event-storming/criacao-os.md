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
| Enviar orçamento | Sistema/Atendente | `OrcamentoEnviado` | Disponibilizado ao cliente para aprovação |
| Aprovar orçamento | Cliente | `OrcamentoAprovado` | Transição → **Em execução** |
| Rejeitar orçamento | Cliente | `OrcamentoRejeitado` | OS volta a Aguardando aprovação após ajuste, ou é encerrada |
