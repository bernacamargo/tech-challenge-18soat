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
> (OWASP Dependency-Check), destravada no mesmo dia com a `NVD_API_KEY`, identificou
> **70 ocorrências de CVEs (16 CRITICAL, 18 HIGH, 33 MEDIUM, 3 LOW)**, concentradas nas
> versões atuais de Spring Framework, Tomcat e Spring Security — detalhes e avaliação de
> aplicabilidade na [seção 4](#4-dependências-e-cves-owasp-dependency-check).

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

### Histórico: bloqueio destravado no mesmo dia

A ODC 13 **exige uma NVD API key** (gratuita) para atualizar a base de CVEs — sem ela a
análise aborta com `NvdApiException: Invalid API Key, length of 0` e, com
`failOnError=false`, o job ficava verde **sem gerar relatório** (verificado nas execuções
até 2026-10-30, ex.: run `40982184835`). A chave foi solicitada, ativada e configurada como
secret `NVD_API_KEY` no repositório em 2026-10-30, destravando a análise:

| Run | Data (UTC) | Duração | Resultado |
|---|---|---|---|
| [`41120486176`](https://github.com/bernacamargo/tech-challenge-18soat/actions/runs/41120486176) | 2026-10-30 20:05 | 47min | ✅ Primeiro relatório gerado (base NVD completa, cache de 116 MB salvo) |
| [`41127018217`](https://github.com/bernacamargo/tech-challenge-18soat/actions/runs/41127018217) | 2026-10-30 20:55 | 36s | ✅ Re-execução com cache NVD (HTML + JSON publicados como artifact) |

### Resultado do scan (dependências do commit `cd80c68`)

**70 ocorrências de CVEs** (a mesma CVE pode aparecer em mais de um artefato) —
**16 CRITICAL, 18 HIGH, 33 MEDIUM, 3 LOW**. Relatório completo:
artifact `vulnerability-report` da run [`41127018217`](https://github.com/bernacamargo/tech-challenge-18soat/actions/runs/41127018217)
(`dependency-check-report.html` / `.json`).

**Distribuição por dependência afetada:**

| Dependência (versão) | Ocorrências | Observação |
|---|---|---|
| Spring Framework 6.2.19 | 17 | Inclui **CVE-2026-47884 (CVSS 9.8): SSRF/RCE via `XsltView` em Spring MVC** — ver avaliação abaixo |
| Apache Tomcat (embedded) 10.1.55 | 15 | Maioria em recursos não usados (DIGEST/FORM auth, rewrite valve, clustering) |
| DOMPurify 3.3.2 (webjar do Swagger UI) | 14 | XSS bypasses (RetireJS); risco contido à UI de documentação |
| Spring Security 6.5.11 | 6 | Maioria em recursos não usados (LDAP embutido, WebAuthn, DPoP) |
| log4j-api 2.24.3 | 4 | Saída XML/JSON malformada em cenários específicos |
| commons-lang3 3.17.0 | 1 | CVE-2025-48924 (recursão infinita) |
| jackson-databind 2.21.4 | 1 | CVE-2026-54515 |
| Spring Data JPA 3.5.13 | 1 | CVE-2026-47834 (bypass de validação no `Sort`) |
| PostgreSQL JDBC 42.7.11 | 1 | CVE-2026-54291 (downgrade de channel binding) |

**Avaliação de aplicabilidade (leitura do grupo, a confirmar em revisão):**

1. **CVE-2026-47884 (Spring Framework, CVSS 9.8)** é a de maior atenção: exige `XsltView`
   com mapeamento `/**` e view name não fixado — a aplicação é uma API REST sem views JSP/XSLT,
   então o vetor **provavelmente não se aplica**, mas a severidade recomenda **atualizar o
   Spring Boot para a versão patch** que traz Spring Framework ≥ 6.2.20, Tomcat ≥ 10.1.58 e
   Spring Security ≥ 6.5.12 (fecha em lote a maioria das CRITICAL/HIGH).
2. As demais CRITICAL de Tomcat/Spring Security atingem componentes **não configurados** na
   aplicação (autenticação é por token JWT via Keycloak, não DIGEST/FORM; não há LDAP embutido,
   WebFlux, RSocket nem clustering).
3. DOMPurify: presente apenas como dependência do Swagger UI; risco de XSS na própria UI de
   documentação, sem acesso a dados da aplicação.

### Correção recomendada

- Atualizar `spring-boot-starter-parent` para o patch mais recente da 3.5.x e re-executar o
  scan para validar a queda das ocorrências.

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
| 1 | Atualizar o Spring Boot (3.5.x patch mais recente) para absorver as correções de Spring Framework/Tomcat/Spring Security (seção 4) | Alta |
| 2 | Revisar caso a caso as ocorrências restantes após o upgrade e documentar as que não se aplicam (suppression file do Dependency-Check) | Média |
| 3 | Incluir o relatório de vulnerabilidades no pipeline de release (automação `release.yml`) | Média |
| 4 | Manter a cadência semanal do `security.yml` (cron de segunda-feira) e verificar que o artifact HTML+JSON é publicado | Média |
| 5 | Revisar security hotspots futuros cedo (o "Sonar way" não reprova por hotspot; revisão manual é responsabilidade do time) | Contínua |

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

Dependency-Check: bloqueio reproduzido localmente (seção 4 — histórico); scan efetivo
executado pelo workflow `security` (runs `41120486176` e `41127018217`) com a
`NVD_API_KEY`, publicando os relatórios HTML e JSON como artifact `vulnerability-report`.
