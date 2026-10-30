# Diagramas DDD — Critério de classificação de subdomínios

Este fluxograma é o critério usado para classificar cada subdomínio da oficina em Principal, Suporte
ou Genérico. O resultado está na tabela "Classificação e justificativa" de [ddd-subdominios.md](ddd-subdominios.md),
de onde este diagrama foi separado (um diagrama por arquivo).
Exemplos: Identidade e Acesso dá **Genérico**, porque a solução pode ser comprada (Keycloak, [ADR 0006](../adr/0006-keycloak-autenticacao-autorizacao.md))
e não arrisca o negócio; Gestão de Ordens de Serviço dá **Principal**, porque não se compra pronta e a lógica de negócio é complexa.
Fonte: Vlad Khononov, *Learning Domain-Driven Design* (O'Reilly, 2021), como apresentado na Aula 1 (Introdução ao DDD) do curso.
Não mostra os subdomínios da oficina nem a justificativa de cada um: veja o diagrama e a tabela em [ddd-subdominios.md](ddd-subdominios.md).

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
