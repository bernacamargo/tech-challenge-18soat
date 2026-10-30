# ADR-0005: SonarCloud + OWASP Dependency-Check para análise de vulnerabilidades

- **Status**: Aceito
- **Data**: 2026-10-01
- **Participantes**: Grupo 117 (Sonar discutido com Frederico Brion; setup definido por Bernardo Camargo)

## Contexto

O enunciado exige "relatório ou resultado da análise de vulnerabilidades" e o grupo discutiu
incluir análise estática (Sonar) no fluxo de CI. O repositório é público.

## Decisão

- **SonarCloud** (gratuito para repositórios públicos): análise estática (bugs, code smells,
  security hotspots), quality gate por PR e importação da cobertura JaCoCo. Configuração no `pom.xml`
  (`sonar.organization=bernacamargo`), token no secret `SONAR_TOKEN`.
- **OWASP Dependency-Check** em workflow dedicado (`security`): varredura de CVEs nas dependências,
  com relatório HTML publicado como artifact e execução semanal agendada.

## Consequências

- Positivas: evidências contínuas para o PDF/vídeo (URL pública do SonarCloud + artifact do
  relatório CVE); quality gate bloqueia PRs com problemas.
- Negativas: primeira execução do Dependency-Check é lenta (download da base NVD) — mitigado com
  cache; opcionalmente um `NVD_API_KEY` acelera.
- Alternativas descartadas: SonarQube self-hosted em docker-compose (scan só local, sem gate de PR),
  Trivy (relatório menos tangível como deliverable).

## Atualização (2026-10-30): `NVD_API_KEY` passou a ser obrigatória

Com a versão **13.0.0** do `dependency-check-maven`, a análise de CVEs **não roda sem uma
`NVD_API_KEY`** (gratuita, em <https://nvd.nist.gov/developers/request-an-api-key>): o cliente da
API do NVD aborta com `NvdApiException: Invalid API Key, length of 0` e, com
`failOnError=false`, o job fica verde **sem gerar relatório** — o que de fato aconteceu nas
execuções até 2026-10-30 (ver seção 4 do
[relatório de vulnerabilidades de 2026-10-30](https://github.com/bernacamargo/tech-challenge-18soat/pull/98)). O motivo é estrutural: o NVD aposentou os data feeds estáticos e a API REST exige
autenticação para um rate limit que torne a análise viável (50 req/30s com chave vs. 5 req/30s anônimo).

Ajustes feitos: `<nvdApiKey>${env.NVD_API_KEY}</nvdApiKey>` no `pom.xml` (os parâmetros
`formats`/`nvdApiKey` não têm user property — não funcionam via CLI), env `NVD_API_KEY` nos
workflows `security` e `release`, e geração dos formatos HTML **e JSON** para o relatório por
release. A mesma atualização criou a **automação do relatório de vulnerabilidades por release**
(asset da GitHub Release + PR automático em `docs/relatorios/`, via
`.github/scripts/generate_vulnerability_report.py`).
