# Linguagem Ubícua — Oficina (Grupo 117)

> Consolidado do Event Storming do grupo (Miro). Rascunho para revisão — atualizar aqui sempre
> que o board mudar. É a referência para nomes de classes, tabelas e endpoints.

## Bounded Contexts

| Contexto | Responsabilidade | Tipo |
|---|---|---|
| **Atendimento** | Ordem de Serviço: criação, orçamento, aprovação, execução, entrega | Core |
| **Catálogo e Estoque** | Serviços, peças e insumos; controle e reserva de estoque | Suporte |
| **Identidade e Acesso** | Identidade dos usuários administrativos (contas no Keycloak) e autenticação JWT das APIs administrativas | Genérico |

## Atores

- **Cliente**: dono do veículo; traz o carro, aprova/rejeita orçamentos, acompanha o status da OS.
- **Atendente**: recebe o cliente, identifica cliente/veículo, cria a OS, envia o orçamento.
- **Mecânico**: executa diagnóstico e serviços, informa peças necessárias e entrega do veículo.
- **Administrador**: administra o catálogo (serviços, peças, estoque). As contas de usuário (criação, senha, perfis) são administradas no console do Keycloak (issue #6), não pela API da oficina.
- **Sistema**: aplica transições de status automáticas, gera orçamento, calcula tempo médio de execução.

## Agregados e entidades

- **OrdemDeServico** (agregado raiz, contexto Atendimento): veículo, cliente, itens, orçamento, status.
- **Cliente**: identificado por CpfCnpj, com histórico de veículos e OS.
- **Veiculo**: placa, marca, modelo, ano — pertence a um Cliente.
- **Servico** (Catálogo): serviço do catálogo (ex.: troca de óleo, alinhamento) com preço.
- **Peca** (Catálogo e Estoque): peça/insumo com quantidade em estoque.
- **ItemDaOS** (entidade dentro de OrdemDeServico): serviço e/ou peça incluído na OS com quantidade.
- **ReservaDeEstoque**: reserva de peça vinculada a uma OS.

## Value Objects

- **CpfCnpj** — identificação fiscal do cliente (com validação de dígito).
- **Placa** — placa do veículo (validação de formato Mercosul/antigo).
- **Dinheiro** — valores monetários do orçamento e preços.
- **StatusOS** — Recebida → Em diagnóstico → Aguardando aprovação → Em execução → Finalizada → Entregue.
- **Orcamento** — total derivado de serviços + peças da OS.
- **QuantidadeReservada** — quantidade de peça reservada para uma OS.

## Identidade e Acesso

> A conta do usuário vive no Keycloak (ADR 0006); a oficina não guarda senha nem conta.
> A cada requisição o domínio monta a identidade autenticada a partir da credencial validada.
> Credencial, Perfil e Permissão estão definidos no Glossário.

- **Conta (Keycloak)**: usuário cadastrado no realm `oficina` do Keycloak, com login, senha e perfis. Criada e mantida só no Keycloak (o realm sobe no compose na issue #6).
- **Usuário administrativo** (`UsuarioAdministrativo`): identidade autenticada (`sub`, login e perfis da oficina), montada a cada requisição a partir da credencial validada. Não é persistida e não é agregado.
- **Dados da credencial** (`DadosDaCredencial`): o que o provedor afirma sobre o portador (sujeito, login e perfis informados), antes de aplicar a regra de perfis.
- **Validador de credencial** (`ValidadorDeCredencial`): contrato do domínio que valida a credencial junto ao provedor de identidade; a implementação fica na infraestrutura (issue #6).
- **Credencial inválida** (`CredencialInvalidaException`): credencial ausente, malformada, sem identificador ou login, ou recusada pelo provedor. Na API corresponde a 401 (issue #6).
- **Sem perfil da oficina** (`UsuarioSemPerfilDaOficinaException`): credencial válida, mas sem nenhum perfil da oficina. Na API corresponde a 403 (issue #6).

## Eventos de domínio (principais)

- `OSCriada`, `OrcamentoGerado`, `OrcamentoAprovado`, `OrcamentoRejeitado`,
  `ServicoAdicionado`, `PecaReservada`, `DiagnosticoRegistrado`, `OSIniciada`,
  `OSFinalizada`, `OSEntregue`, `PecaAdicionada`, `PecaRemovida`, `EstoqueBaixado`.
- Identidade e Acesso não publica eventos de domínio.

## Sistemas externos

- **Keycloak**: provedor de identidade (upstream). Identidade e Acesso é **conformista** (padrão de relação entre contextos do DDD): adota o modelo de usuários, realm roles e tokens do Keycloak, sem modelo próprio de conta. A escolha do Keycloak está na ADR 0006.
- **MailPit**: servidor SMTP local (ADR 0007); será também o SMTP do Keycloak para e-mails de conta (issue #6). O envio de e-mail pela própria oficina (`NotificacaoPort`) ainda não existe.

## Glossário

- **OS (Ordem de Serviço)**: registro que acompanha o atendimento do veículo do cliente.
- **Orçamento**: somatório de serviços e peças da OS, enviado ao cliente para aprovação.
- **Reserva de estoque**: quantidade de peça separada para uma OS antes da execução.
- **Tempo médio de execução**: métrica calculada pelo sistema entre OS iniciada e finalizada.
- **Credencial** (`Credencial`): prova de identidade do usuário administrativo: o access token Bearer emitido pelo Keycloak e enviado no header `Authorization`. Para o domínio é um valor opaco; nunca aparece em `toString` e não deve aparecer em log (issue #6). A senha não é credencial da API: só o Keycloak a recebe.
- **Perfil** (`Perfil`): conjunto de permissões de um funcionário da oficina: ATENDENTE, MECANICO ou ADMINISTRADOR (ADR 0006), reconhecido pelo nome exato. Equivalência: perfil = papel (termo da ADR 0006 e da issue #4) = *realm role* no Keycloak = authority `ROLE_*` no Spring Security. Roles técnicos do Keycloak (`default-roles-oficina`, `offline_access`, `uma_authorization`) não são perfis da oficina e são ignorados. Não confundir com **perfil de execução**: o profile do Spring/compose (`local`, `dev`, `full`).
- **Permissão**: ação que um perfil autoriza (por exemplo, excluir uma peça). Hoje é expressa pelas regras `@PreAuthorize` dos controllers e pela regra de URL do `SecurityConfig`; não é um tipo do domínio.

## Contato e notificação de orçamento

- **Email**: endereço de contato do cliente, validado sintaticamente. Opcional para cadastros legados.
- **Orçamento pronto**: geração por conclusão do diagnóstico ou submissão agenda o envio ao cliente.
- **Notificação pendente**: snapshot técnico persistido no PostgreSQL; enviado somente após commit,
  com retry SMTP e espera por contato ausente. Não altera o estado da OS nem a decisão do cliente.
