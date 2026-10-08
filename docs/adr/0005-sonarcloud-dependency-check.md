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
