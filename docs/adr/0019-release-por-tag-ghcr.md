# ADR-0019: Release por tag com imagem no GHCR (sem deploy)

- **Status**: Aceito
- **Data**: 2026-10-29
- **Participantes**: Grupo 117 (proposto por Bernardo Camargo)

## Contexto

O CI atual valida pull requests e pushes em `main` (build, testes, cobertura e SonarCloud), mas
não publica artefato de release. O `Dockerfile` multi-stage já empacota a API; o Compose local
usa `build: .`. A hospedagem futura será na AWS; neste momento não há conta, registry nem
runtime definidos. Precisamos de um contrato de release imutável (versão + imagem) sem acoplar
deploy.

## Decisão

Publicar uma **release a partir de tag semver anotada** `vMAJOR.MINOR.PATCH` apontando para um
commit de `main`:

1. Workflow dedicado `.github/workflows/release.yml`, disparado por `push` da tag.
2. Guardas: o commit da tag deve ser ancestral de `origin/main`; o check `ci` desse SHA deve
   estar `success`. A suíte de testes **não** é reexecutada no release.
3. Build e push da imagem para o **GitHub Container Registry (GHCR)**
   (`ghcr.io/bernacamargo/tech-challenge-18soat`), com tags imutáveis `X.Y.Z` (sem o prefixo `v`)
   e `sha-<curto>`. **Sem** tag `latest`.
4. Criação de **GitHub Release** com notas geradas (`gh release create --generate-notes`). Se a
   release já existir publicada, o job segue e apenas publica a imagem. Se existir como
   **draft** (caso típico: a tag foi apagada/recriada e o GitHub desvinculou a release), o
   workflow republica com `gh release edit --draft=false`.
5. Relatório de vulnerabilidades (SonarCloud + Dependency-Check): gerado no mesmo job, anexado
   como asset da release e versionado em `docs/relatorios/` via PR automático (exige a opção
   *Allow GitHub Actions to create and approve pull requests* nas Settings do repositório).
6. A versão da imagem vem da tag git. O `pom.xml` permanece `0.0.1-SNAPSHOT` no desenvolvimento;
   não há commit de bump.
7. **Nenhum** step de deploy, credencial AWS ou ECR neste workflow. Compose local continua com
   `build: .`.

Quando houver conta AWS, um ADR futuro escolhe o runtime; o deploy entra em workflow próprio
(OIDC), puxando a tag já publicada no GHCR — o workflow de release não ganha passos de
hospedagem.

Após o primeiro push, o pacote no GHCR nasce **privado**. Uma vez, nas Settings do pacote:
vincular o repositório e tornar o pacote público (`GITHUB_TOKEN` não altera visibilidade).

**Tag imutável após publicação:** não apagar nem mover `vX.Y.Z` já publicada. O GitHub
desvincula a release e a deixa draft (`untagged-…`). Correção posterior = nova versão
semver (ex.: `v0.1.1`), não retag da mesma.

## Consequências

- Positivas: artefato reproduzível e pinável para a AWS futura; evidência de release no GitHub
  (imagem + Release notes + relatório); CI de merge permanece sem permissão `packages: write`.
- Negativas / trade-offs aceitos: corte de versão é manual; pacote GHCR exige ajuste único de
  visibilidade após o primeiro push; sem `latest`, quem consome a imagem precisa da versão
  explícita; tag publicada é contrato — mover exige republicar draft ou cortar patch novo.
- Alternativas consideradas e por que foram descartadas:
  - **Bump automático a cada merge em `main`**: publicaria imagem de trabalho incompleto a cada
    PR e misturaria “artefato de release” com “build contínuo”.
  - **Anexar só o JAR na GitHub Release**: não deixa artefato de container para a AWS puxar;
    o contrato de deploy futuro seria outro.
