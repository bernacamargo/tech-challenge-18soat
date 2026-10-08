# Agregados, Entidades e Objetos de Valor

**Projeto:** Tech Challenge Fase 1 — SOAT (Software Architecture)
**Domínio:** Sistema Integrado de Atendimento e Execução de Serviços (oficina mecânica)

> Modelo tático validado pelo grupo com base nos três quadros de Event Storming
> (Criação da OS, Acompanhamento da OS, Gestão de Peças e Insumos). Cada elemento é
> justificado pela regra de negócio ou invariante que ele protege.

---

## 1. Agregados

Um agregado não nasce "um por fluxo de Event Storming" — ele nasce de uma pergunta
diferente: **o que precisa mudar atomicamente junto, e o que pode mudar em momentos
separados?** Por isso um único fluxo (ex.: Criação da OS) atravessa vários agregados, e um
agregado só (ex.: `Peca`) pode ser usado por vários fluxos.

| # | Agregado (raiz) | Contexto | Por que é uma raiz separada |
|---|---|---|---|
| 1 | **OrdemDeServico** | Atendimento | Concentra a maior parte dos comandos dos fluxos 1 e 2 (abrir, incluir item, submeter, diagnosticar, aprovar/rejeitar, executar, entregar). Precisa ser uma única fronteira transacional porque suas regras de transição de status (`StatusOS`) só podem ser garantidas se toda alteração passar por ela. |
| 2 | **Cliente** | Atendimento | Tem identidade e ciclo de vida **independentes** da OS — existe antes da primeira visita e continua existindo depois que a OS é entregue. Se vivesse dentro da OS, a oficina perderia o histórico do cliente a cada atendimento. |
| 3 | **Veiculo** | Atendimento | Mesmo raciocínio do Cliente: o mesmo veículo (mesma placa) gera **várias** Ordens de Serviço ao longo do tempo (ex.: troca de óleo em janeiro, revisão de freios em junho). Se o veículo fosse parte da OS, não haveria como consultar o histórico de um veículo — um dos problemas que o próprio desafio cita. |
| 4 | **Peca** | Catálogo/Estoque | Protege a invariante mais sensível do domínio: **o saldo de estoque nunca pode ficar negativo**, e toda reserva/baixa (quadro 3: `Reservar quantidade`, `Dar baixa efetiva`, `Estoque insuficiente`) precisa passar por ela. Fica separada de `OrdemDeServico` de propósito: se a reserva falhar, isso não deve travar a criação da OS na mesma transação — é consistência eventual entre agregados, não atomicidade dentro de um só. |
| 5 | **Servico** (catálogo) | Catálogo/Estoque | Cadastro de referência (preço, descrição), sem relação de dependência forte com a OS — um serviço pode ser cadastrado, alterado ou descontinuado sem nenhuma OS em andamento ser afetada. |
| 6 | **UsuarioAdministrativo** | Identidade e Acesso | Contexto totalmente à parte (autenticação JWT para as APIs administrativas); não compartilha nenhuma regra de negócio com a oficina em si. |

---

## 2. Entidades

Uma entidade tem **identidade própria** e muda de estado ao longo do tempo — duas
entidades com os mesmos atributos ainda são coisas diferentes se têm IDs diferentes.

### 2.1 Raízes de agregado

No código (Java/Spring Boot): `@Entity` com repositório próprio — são o único ponto de
entrada para ler/alterar o agregado.

- `OrdemDeServico`
- `Cliente`
- `Veiculo`
- `Peca`
- `Servico`
- `UsuarioAdministrativo`

### 2.2 Entidades internas

No código: `@Entity`, mas **sem** repositório próprio — só existem dentro do agregado que as
contém, acessadas via a raiz.

| Entidade interna | Vive dentro de | Por que precisa de identidade própria (e não é só um Objeto de Valor) |
|---|---|---|
| `ItemDaOS` | `OrdemDeServico` | Representa cada "Serviço/Peça apontado" incluído na OS. Precisa de identidade porque a mesma OS pode ter **dois itens com os mesmos atributos** (ex.: duas unidades do mesmo filtro de óleo incluídas em momentos diferentes) e o sistema precisa distinguir um do outro para, por exemplo, remover um item específico sem afetar o outro. |
| `ReservaDeEstoque` | `Peca` | Precisa registrar **quanto** foi reservado **para qual OS**, para permitir a reversão (quadro 3: `Peças e insumos voltam ao estoque` quando a OS é rejeitada). Sem uma entidade com identidade própria, não haveria como saber qual reserva específica desfazer. |

---

## 3. Objetos de Valor

Um objeto de valor **não tem identidade** — dois VOs com os mesmos atributos são
intercambiáveis, e ele é sempre imutável (qualquer "alteração" cria um novo VO, nunca edita
o existente).

No código (Java/Spring Boot): normalmente `record` e/ou `@Embeddable`; `StatusOS` é um `enum`.

| VO | Onde vive | Por que é VO e não Entidade |
|---|---|---|
| `CpfCnpj` | dentro de `Cliente` | Dois clientes com o mesmo CPF são, por definição, o mesmo cliente — a identidade do `Cliente` é justamente o CPF/CNPJ, então o VO não precisa de uma identidade própria além do valor que carrega. Também encapsula a validação de formato/dígitos verificadores. |
| `Placa` | dentro de `Veiculo` | Mesma lógica: a placa já É o identificador natural do veículo, não faz sentido ter um ID técnico separado "para a placa". Encapsula a validação de formato (padrão antigo e Mercosul). |
| `StatusOS` | dentro de `OrdemDeServico` | É um enum fechado (Recebida, Em diagnóstico, Aguardando aprovação, Em execução, Finalizada, Entregue) com as transições válidas conhecidas de antemão — não existem "duas instâncias diferentes" do status `EM_EXECUCAO`, é sempre o mesmo valor. |
| `Orcamento` | dentro de `OrdemDeServico` | Gerado automaticamente a partir dos itens; se os itens mudarem, **gera-se um novo cálculo**, nunca se edita o orçamento anterior (preserva histórico e evita inconsistência entre o que foi aprovado e o que está salvo). |
| `Dinheiro` | usado por `Servico`, `ItemDaOS`, `Orcamento` | Representa um valor monetário com sua própria validação (não aceita negativo); evita espalhar `BigDecimal`/`float` soltos pelo domínio sem nenhuma regra associada. |
| `QuantidadeReservada` | dentro de `ReservaDeEstoque` | Quantidade + unidade de uma reserva específica; não tem vida própria fora da reserva que a contém. |
