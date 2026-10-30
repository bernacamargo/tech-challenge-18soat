# Relatório de Vulnerabilidades — 2026-10-30

| Campo | Valor |
|---|---|
| Projeto | `oficina` (`bernacamargo_tech-challenge-18soat`) |
| Commit analisado | `cd80c687a79570f30787edcf30d4acd8575d1c23` (`chore: dispara o CI da main (#97)`) |
| Branch analisada | `main` (branch principal no SonarCloud) |
| Data da análise SonarCloud | 2026-10-30 00:51:51 UTC |
| Execução do CI | [Run `40982184876`](https://github.com/bernacamargo/tech-challenge-18soat/actions/runs/40982184876) (`ci`), [Run `40982184835`](https://github.com/bernacamargo/tech-challenge-18soat/actions/runs/40982184835) (`security`) |
| Ferramentas | SonarCloud (sonar-maven-plugin 5.8.0.7211), OWASP Dependency-Check 13.0.0 |

> **Resumo executivo:** a análise estática do SonarCloud no branch `main` não encontrou
> **nenhuma vulnerabilidade, security hotspot, bug ou code smell** — ratings de segurança e
> confiabilidade "A". A cobertura é de 96,6%. A análise de CVEs de dependências
> (OWASP Dependency-Check) está **bloqueada por ausência de NVD API key** — a falha e o
> plano para destravá-la estão detalhados na [seção 4](#4-dependências-e-cves-owasp-dependency-check).

## 1. Qualidade Gate e métricas gerais

Quality Gate associado ao projeto: **Sonar way** (padrão da organização).

> O status do gate da última análise consta como `NONE` no SonarCloud — o gate foi
> associado ao projeto após a primeira análise da `main` repopulada (pós-remame
> `master` → `main`). Com as métricas abaixo, o projeto **atende** às condições do
> "Sonar way"; o status será calculado normalmente na próxima análise.

| Métrica | Valor | Classificação Sonar |
|---|---|---|
| Vulnerabilidades | **0** | A (`security_rating` = 1.0) |
| Security Hotspots | **0** | A (`security_review_rating` = 1.0) |
| Bugs | **0** | A (`reliability_rating` = 1.0) |
| Code Smells | **0** | A (`sqale_rating` = 1.0) |
| Cobertura de testes | **96,6%** | — |
| Linhas de código (ncloc) | 5.021 | — |

Dashboard: <https://sonarcloud.io/dashboard?id=bernacamargo_tech-challenge-18soat&branch=main>

## 2. Vulnerabilidades (SonarCloud)

**Nenhuma vulnerabilidade aberta** no branch `main`
(`api/issues/search?types=VULNERABILITY&resolved=false` → `total: 0`).

## 3. Security Hotspots (SonarCloud)

**Nenhum hotspot pendente de revisão**
(`api/hotspots/search?branch=main` → `total: 0`).

## 4. Dependências e CVEs (OWASP Dependency-Check)

### Status: bloqueado — ausência de NVD API key

O OWASP Dependency-Check 13 **exige uma NVD API key** (gratuita) para atualizar a base de
CVEs. Sem a chave, a análise é abortada com erro fatal antes de avaliar as dependências:

```
[ERROR] Error updating the NVD Data
Caused by: io.github.jeremylong.openvulnerability.client.nvd.NvdApiException:
    Invalid API Key, length of 0 too short to provided a masked partial key
[ERROR] Unable to continue dependency-check analysis.
```

Reproduzido localmente e no CI (run `40982184835` do workflow `security`, 2026-10-30
00:50 UTC). Consequências atuais:

1. **Nenhum relatório de CVE é gerado** — o passo de upload do artifact
   (`dependency-check-report.html`) registra "No files were found" e não publica nada.
2. **O job `security` fica verde de forma enganosa**: o `security.yml` usa
   `-DfailOnError=false`, então a falha de atualização da NVD não reprovou o build.

### Ações para destravar (prioridade alta)

1. Solicitar a chave gratuita em <https://nvd.nist.gov/developers/request-an-api-key>.
2. Configurá-la como secret `NVD_API_KEY` no repositório GitHub.
3. Passar `-DnvdApiKey=$NVD_API_KEY` nos workflows `security.yml` e `release.yml`
   (a automação de relatório por release já inclui esse suporte).

## 5. Configuração do Sonar no CI (contexto)

- O workflow `ci.yml` dispara em `pull_request` **e** `push` para `main`; o passo
  "SonarCloud analysis" roda `./mvnw -B sonar:sonar` em ambos (sem filtro de branch).
- O branch `main` está configurado como **branch principal** no SonarCloud
  (`api/project_branches/list` → `isMain: true`) e a análise de 2026-10-30 00:51 UTC
  corresponde ao commit `cd80c68` — confirmando que a correção introduzida pelo PR #97
  (trigger de push na main + remame do branch principal no SonarCloud) está funcionando.

## 6. Recomendações priorizadas

| # | Ação | Prioridade |
|---|---|---|
| 1 | Configurar `NVD_API_KEY` e destravar a análise de CVEs (seção 4) | Alta |
| 2 | Incluir o relatório de vulnerabilidades no pipeline de release (automação `release.yml`) | Média |
| 3 | Manter o cadência semanal do `security.yml` (cron de segunda-feira) e verificar que o artifact passa a ser publicado | Média |
| 4 | Revisar security hotspots futuros cedo (o "Sonar way" não reprova por hotspot; revisão manual é responsabilidade do time) | Contínua |

## 7. Metodologia e reprodutibilidade

Dados coletados em 2026-10-30 (13:30 BRT) via API pública do SonarCloud
(projeto público, sem token):

```bash
P=bernacamargo_tech-challenge-18soat
curl -s "https://sonarcloud.io/api/project_branches/list?project=$P"        # branch principal + data da análise
curl -s "https://sonarcloud.io/api/measures/component?component=$P&branch=main&metricKeys=vulnerabilities,security_hotspots,security_rating,security_review_rating,bugs,code_smells,reliability_rating,sqale_rating,coverage,ncloc"
curl -s "https://sonarcloud.io/api/issues/search?componentKeys=$P&branch=main&types=VULNERABILITY&resolved=false&ps=500"
curl -s "https://sonarcloud.io/api/hotspots/search?projectKey=$P&branch=main&ps=500"
curl -s "https://sonarcloud.io/api/qualitygates/get_by_project?project=$P&organization=bernacamargo"
```

Dependency-Check reproduzido localmente com `./mvnw -B -DskipTests
dependency-check:check -DfailOnError=false` (saída na seção 4).
