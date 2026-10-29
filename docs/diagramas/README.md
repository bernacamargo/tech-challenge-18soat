# Diagramas

Cada diagrama em arquivo próprio (decisão do grupo — nada de PDF único gigante).

- `c4-contexto.md` — C4 System Context
- `c4-container.md` — C4 Container
- `ddd-subdominios.md` — Diagrama de subdomínios (Principal / Suporte / Genérico)
- Event storming: fica em `docs/ddd/event-storming/<fluxo>/`, com o export do Miro (PNG) ao lado do `.md`:
  - `docs/ddd/event-storming/acompanhamento-os/acompanhamento-os.md` e `acompanhamento-os.png`
  - `docs/ddd/event-storming/criacao-os/criacao-os.md` e `criacao-os.png`
  - `docs/ddd/event-storming/gestao-pecas/gestao-pecas.md` e `gestao-pecas.png`

Fontes em Mermaid (editáveis e versionáveis); exports de imagem do Miro ficam ao lado dos `.md`.
Diagramas C4 evoluem **em paralelo com o código** (decisão da reunião) — atualizar a cada mudança
arquitetural e referenciar no ADR correspondente.
