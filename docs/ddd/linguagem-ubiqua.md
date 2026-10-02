# Linguagem Ubícua — Oficina (Grupo 117)

> Consolidado do Event Storming do grupo (Miro). Rascunho para revisão — atualizar aqui sempre
> que o board mudar. É a referência para nomes de classes, tabelas e endpoints.

## Bounded Contexts

| Contexto | Responsabilidade | Tipo |
|---|---|---|
| **Atendimento** | Ordem de Serviço: criação, orçamento, aprovação, execução, entrega | Core |
| **Catálogo e Estoque** | Serviços, peças e insumos; controle e reserva de estoque | Suporte |
| **Identidade e Acesso** | Usuários administrativos, autenticação JWT das APIs administrativas | Genérico |

## Atores

- **Cliente**: dono do veículo; traz o carro, aprova/rejeita orçamentos, acompanha o status da OS.
- **Atendente**: recebe o cliente, identifica cliente/veículo, cria a OS, envia o orçamento.
- **Mecânico**: executa diagnóstico e serviços, informa peças necessárias e entrega do veículo.
- **Administrador**: administra catálogo (serviços, peças, estoque) e usuários (APIs administrativas, JWT).
- **Sistema**: aplica transições de status automáticas, gera orçamento, calcula tempo médio de execução.

## Agregados e entidades

- **OrdemDeServico** (agregado raiz, contexto Atendimento): veículo, cliente, itens, orçamento, status.
- **Cliente**: identificado por CpfCnpj, com histórico de veículos e OS.
- **Veiculo**: placa, marca, modelo, ano — pertence a um Cliente.
- **Servico** (Catálogo): serviço do catálogo (ex.: troca de óleo, alinhamento) com preço.
- **Peca** (Catálogo e Estoque): peça/insumo com quantidade em estoque.
- **ItemDaOS** (entidade dentro de OrdemDeServico): serviço e/ou peça incluído na OS com quantidade.
- **ReservaDeEstoque**: reserva de peça vinculada a uma OS.
- **UsuarioAdministrativo** (Identidade e Acesso): credenciais de acesso administrativo.

## Value Objects

- **CpfCnpj** — identificação fiscal do cliente (com validação de dígito).
- **Placa** — placa do veículo (validação de formato Mercosul/antigo).
- **Dinheiro** — valores monetários do orçamento e preços.
- **StatusOS** — Recebida → Em diagnóstico → Aguardando aprovação → Em execução → Finalizada → Entregue.
- **Orcamento** — total derivado de serviços + peças da OS.
- **QuantidadeReservada** — quantidade de peça reservada para uma OS.

## Eventos de domínio (principais)

- `OSCriada`, `OrcamentoGerado`, `OrcamentoAprovado`, `OrcamentoRejeitado`,
  `ServicoAdicionado`, `PecaReservada`, `DiagnosticoRegistrado`, `OSIniciada`,
  `OSFinalizada`, `OSEntregue`, `PecaAdicionada`, `PecaRemovida`, `EstoqueBaixado`.

## Glossário

- **OS (Ordem de Serviço)**: registro que acompanha o atendimento do veículo do cliente.
- **Orçamento**: somatório de serviços e peças da OS, enviado ao cliente para aprovação.
- **Reserva de estoque**: quantidade de peça separada para uma OS antes da execução.
- **Tempo médio de execução**: métrica calculada pelo sistema entre OS iniciada e finalizada.
