# Diagramas DDD

## Diagrama de Subdomínios

Domínio: **Manutenção e Reparo de Veículos**

```mermaid
flowchart TB
    subgraph DOM["Domínio: Manutenção e Reparo de Veículos"]
        direction TB

        subgraph CORE["PRINCIPAL (Core): o diferencial do negócio"]
            OS["<b>Gestão de Ordens de Serviço</b><br/>diagnóstico · orçamento · aprovação<br/>execução · acompanhamento de status"]
        end

        subgraph SUP["SUPORTE: necessário, mas não diferencia"]
            direction LR
            EST["<b>Catálogo e Estoque</b><br/>serviços · peças · insumos<br/>saldo · reserva · baixa"]
            CLI["<b>Clientes e Veículos</b><br/>cadastro · histórico<br/>validação CPF/CNPJ e placa"]
            IND["<b>Indicadores Operacionais</b><br/>tempo médio de execução"]
        end

        subgraph GEN["GENÉRICO: comum a qualquer empresa"]
            direction LR
            IAM["<b>Identidade e Acesso</b><br/>autenticação JWT<br/>usuários administrativos"]
            NOT["<b>Notificações</b><br/>envio de orçamento<br/>avisos de status"]
        end

        CORE ~~~ SUP ~~~ GEN
    end

    classDef core fill:#f8d3af,stroke:#fe9f4d,color:#1a1a1a
    classDef sup fill:#c6dcff,stroke:#659df2,color:#1a1a1a
    classDef gen fill:#dedaff,stroke:#8f7fee,color:#1a1a1a
    class OS core
    class EST,CLI,IND sup
    class IAM,NOT gen
    style CORE fill:#fff5ed,stroke:#fe9f4d
    style SUP fill:#f0f5fd,stroke:#659df2
    style GEN fill:#f4f2fd,stroke:#8f7fee
```

### Classificação e justificativa

| Subdomínio | Tipo | Justificativa | Bounded Context |
|---|---|---|---|
| Gestão de Ordens de Serviço | Principal | É o diferencial da oficina (acompanhamento em tempo real e aprovação pelo cliente) e resolve as dores centrais do negócio. Lógica complexa: regras de transição de status e de orçamento. | Atendimento (Core) |
| Catálogo e Estoque | Suporte | Necessário para montar o orçamento, mas não diferencia a oficina. Lógica relativamente simples (CRUD com reserva e baixa). Poderia ser um ERP pronto, mas a reserva está acoplada ao fluxo da OS. | Catálogo e Estoque |
| Clientes e Veículos | Suporte | Cadastro e histórico, basicamente CRUD com validação de CPF/CNPJ e placa. Sem vantagem competitiva. | A definir |
| Indicadores Operacionais | Suporte | Cálculo derivado dos dados da OS (tempo médio de execução), sem regra própria complexa. | Atendimento |
| Identidade e Acesso | Genérico | Toda empresa tem; existe solução pronta e não diferencia o negócio. | Identidade e Acesso |
| Notificações | Genérico | Commodity; pode usar serviço pronto de e-mail ou push. | Sem contexto próprio no MVP |

### Critério de classificação

Fluxograma de Vlad Khononov (Aula 1 — Introdução ao DDD):

```mermaid
flowchart TD
    Q1{"A solução pode<br/>ser comprada?"}
    Q2{"Pode arriscar<br/>o negócio?"}
    Q3{"A lógica de negócio<br/>é complexa?"}
    G["Subdomínio Genérico"]
    P["Subdomínio Principal"]
    S["Subdomínio de Suporte"]

    Q1 -- Sim --> Q2
    Q1 -- Não --> Q3
    Q2 -- Não --> G
    Q2 -- Sim --> P
    Q3 -- Sim --> P
    Q3 -- Não --> S

    classDef core fill:#f8d3af,stroke:#fe9f4d,color:#1a1a1a
    classDef sup fill:#c6dcff,stroke:#659df2,color:#1a1a1a
    classDef gen fill:#dedaff,stroke:#8f7fee,color:#1a1a1a
    class P core
    class S sup
    class G gen
```

> Versão visual no Miro: [Diagrama de Subdomínios](https://miro.com/app/board/uXjVHnu_abs=/?moveToWidget=3458764685741236999)
