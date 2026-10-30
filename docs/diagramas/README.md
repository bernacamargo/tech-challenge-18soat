# Diagramas

Cada diagrama em arquivo próprio (decisão do grupo — nada de PDF único gigante).

- `c4-contexto.md` — C4 System Context (nível 1): pessoas, o sistema da oficina e os sistemas externos
- `c4-container.md` — C4 Container (nível 2): runtime da requisição autenticada (monólito, Keycloak, MailPit e banco `oficina`)
- `c4-container-observabilidade.md` — C4 Container (nível 2), vista de observabilidade separada de `c4-container.md`: Prometheus coleta `app:9091` e Grafana consulta o Prometheus (perfil `observability`, ADR 0016)
- `ddd-subdominios.md` — Diagrama de subdomínios (Principal / Suporte / Genérico), com a tabela de classificação
- `ddd-criterio-classificacao.md` — Critério de classificação (fluxograma de Vlad Khononov) separado de `ddd-subdominios.md`, cuja tabela é o resultado dele
- Event storming: fica em `docs/ddd/event-storming/<fluxo>/`, com o export do Miro (PNG) ao lado do `.md`:
  - `docs/ddd/event-storming/acompanhamento-os/acompanhamento-os.md` e `acompanhamento-os.png`
  - `docs/ddd/event-storming/criacao-os/criacao-os.md` e `criacao-os.png`
  - `docs/ddd/event-storming/gestao-pecas/gestao-pecas.md` e `gestao-pecas.png`

Fontes em Mermaid (editáveis e versionáveis); exports de imagem do Miro ficam ao lado dos `.md`.
Os C4 usam `flowchart` com estilo C4 (cores por tipo de elemento, `subgraph` para as fronteiras e
tabela de legenda), e não `C4Context`/`C4Container`: a sintaxe C4 do Mermaid é experimental e faz o layout
sozinha, o que sobrepunha rótulos e cruzava linhas na renderização do GitHub.
Diagramas C4 evoluem **em paralelo com o código** (decisão da reunião) — atualizar a cada mudança
arquitetural e referenciar no ADR correspondente.
