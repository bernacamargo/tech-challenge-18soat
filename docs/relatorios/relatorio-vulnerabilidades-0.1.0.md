# Relatório de Vulnerabilidades — release v0.1.0

> Gerado automaticamente em 2026-10-31 03:11 UTC pelo workflow de release.

## SonarCloud (branch principal)

### Contexto da análise

| Campo | Valor |
|---|---|
| Branch analisada | `main` |
| Release (tag) | `v0.1.0` |
| Commit analisado (SonarCloud) | `8999079e32b7` |
| Data da análise SonarCloud | `2026-10-31T03:09:09+0000` |

Quality Gate: **Sonar way** — status da análise: **OK**

| Métrica | Valor | Classificação Sonar |
|---|---|---|
| Vulnerabilidades | **0** | A (`security_rating`) |
| Security Hotspots | **0** | A (`security_review_rating`) |
| Bugs | **0** | A (`reliability_rating`) |
| Code Smells | **0** | A (`sqale_rating`) |
| Cobertura de testes | **96.6%** | — |
| Linhas de código (ncloc) | 4682 | — |

Dashboard: <https://sonarcloud.io/dashboard?id=bernacamargo_tech-challenge-18soat&branch=main>

### Vulnerabilidades (SonarCloud)

**Nenhuma vulnerabilidade aberta** no branch principal.

### Security Hotspots (SonarCloud)

**Nenhum security hotspot** pendente de revisão.


## Dependências e CVEs (OWASP Dependency-Check)

**70 ocorrência(s) de CVE(s) identificada(s)** nas dependências (a mesma CVE pode aparecer para mais de um artefato; nem toda ocorrência é explorável na configuração desta aplicação — avaliar caso a caso):

### 🔴 CRITICAL (16)

| CVE | CVSS | Dependência | Correção a partir de | CWE |
|---|---|---|---|---|
| `CVE-2026-47884` | 9.8 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-22 |
| `CVE-2026-47890` | 9.8 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-93 |
| `CVE-2026-47891` | 9.8 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-770 |
| `CVE-2026-47892` | 9.8 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-863 |
| `CVE-2026-59313` | 9.8 | `maven/org.springframework/spring-core@6.2.19` | — | CWE-93 |
| `CVE-2026-59283` | 9.1 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-913 |
| `CVE-2026-59270` | 9.1 | `maven/org.springframework.security/spring-security-core@6.5.11` | 7.1.0.1 | CWE-863 |
| `CVE-2026-59270` | 9.1 | `maven/org.springframework.security/spring-security-oauth2-resource-server@6.5.11` | 7.1.0.1 | CWE-863 |
| `CVE-2026-65637` | 9.8 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.25 | CWE-20 |
| `CVE-2026-65905` | 9.8 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.25 | CWE-294 |
| `CVE-2026-53434` | 9.1 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.23 | CWE-390 |
| `CVE-2026-55276` | 9.1 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.23 | CWE-670 |
| `CVE-2026-59083` | 9.1 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | — | CWE-177 |
| `CVE-2026-59084` | 9.1 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | — | CWE-1059 |
| `CVE-2026-65182` | 9.1 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.25 | CWE-863, CWE-284 |
| `CVE-2026-68525` | 9.1 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.25 | CWE-863 |

### 🟠 HIGH (18)

| CVE | CVSS | Dependência | Correção a partir de | CWE |
|---|---|---|---|---|
| `CVE-2026-54291` | 5.9 | `maven/org.postgresql/postgresql@42.7.11` | 42.7.12 | CWE-636, CWE-757 |
| `CVE-2026-47885` | 7.5 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-770 |
| `CVE-2026-47886` | 7.5 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-400 |
| `CVE-2026-47888` | 7.5 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-401 |
| `CVE-2026-47889` | 7.5 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-1275 |
| `CVE-2026-47893` | 7.5 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-209 |
| `CVE-2026-59282` | 7.5 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-400 |
| `CVE-2026-41707` | 7.4 | `maven/org.springframework.security/spring-security-core@6.5.11` | 7.1.0.1 | CWE-294 |
| `CVE-2026-47841` | 7.4 | `maven/org.springframework.security/spring-security-core@6.5.11` | 7.1.0.1 | CWE-863 |
| `CVE-2026-41707` | 7.4 | `maven/org.springframework.security/spring-security-oauth2-resource-server@6.5.11` | 7.1.0.1 | CWE-294 |
| `CVE-2026-47841` | 7.4 | `maven/org.springframework.security/spring-security-oauth2-resource-server@6.5.11` | 7.1.0.1 | CWE-863 |
| `CVE-2026-116803` | — | `javascript/DOMPurify@3.3.2` | — | — |
| `CVE-2026-65183` | 8.1 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.25 | CWE-367 |
| `CVE-2026-66422` | 8.1 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.25 | CWE-285 |
| `CVE-2026-68569` | 8.1 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.25 | CWE-287 |
| `CVE-2026-65927` | 7.5 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.25 | CWE-193 |
| `CVE-2026-68763` | 7.5 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.25 | CWE-400 |
| `CVE-2026-53404` | 7.3 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.23 | CWE-670 |

### 🟡 MEDIUM (33)

| CVE | CVSS | Dependência | Correção a partir de | CWE |
|---|---|---|---|---|
| `CVE-2025-48924` | 5.3 | `maven/org.apache.commons/commons-lang3@3.17.0` | 3.18.0 | CWE-674 |
| `CVE-2026-54515` | 5.3 | `maven/com.fasterxml.jackson.core/jackson-databind@2.21.4` | 3.1.4 | CWE-915 |
| `CVE-2026-34479` | 7.5 | `maven/org.apache.logging.log4j/log4j-api@2.24.3` | 2.25.4 | CWE-116 |
| `CVE-2026-34477` | 5.9 | `maven/org.apache.logging.log4j/log4j-api@2.24.3` | 2.25.4 | CWE-295, CWE-297 |
| `CVE-2026-49844` | 5.9 | `maven/org.apache.logging.log4j/log4j-api@2.24.3` | 2.25.5 | CWE-116 |
| `CVE-2026-47883` | 6.1 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-601 |
| `CVE-2026-47887` | 6.1 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-601 |
| `CVE-2026-59281` | 6.1 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-79 |
| `CVE-2026-59280` | 4.3 | `maven/org.springframework/spring-core@6.2.19` | 7.0.8.1 | CWE-22 |
| `CVE-2026-47834` | 6.5 | `maven/org.springframework.data/spring-data-jpa@3.5.13` | 4.1.0.1 | NVD-CWE-noinfo |
| `CVE-2026-47842` | 6.5 | `maven/org.springframework.security/spring-security-core@6.5.11` | 7.1.0.1 | CWE-326 |
| `CVE-2026-59276` | 5.9 | `maven/org.springframework.security/spring-security-core@6.5.11` | 7.1.0.1 | CWE-208 |
| `CVE-2026-47842` | 6.5 | `maven/org.springframework.security/spring-security-oauth2-resource-server@6.5.11` | 7.1.0.1 | CWE-326 |
| `CVE-2026-59276` | 5.9 | `maven/org.springframework.security/spring-security-oauth2-resource-server@6.5.11` | 7.1.0.1 | CWE-208 |
| `CVE-2026-49978` | 6.1 | `javascript/DOMPurify@3.3.2` | 3.4.7 | CWE-79 |
| `CVE-2026-49458` | 6.1 | `javascript/DOMPurify@3.3.2` | 3.4.6 | CWE-79, CWE-501, CWE-693 |
| `CVE-2026-49459` | 6.1 | `javascript/DOMPurify@3.3.2` | 3.4.6 | CWE-79, CWE-1321, CWE-693 |
| `CVE-2026-41240` | 6.1 | `javascript/DOMPurify@3.3.2` | 3.4.0 | CWE-79, CWE-183 |
| `CVE-2026-65902` | 6.1 | `javascript/DOMPurify@3.3.2` | 3.4.7 | CWE-501 |
| `CVE-2026-65898` | 7.2 | `javascript/DOMPurify@3.3.2` | 3.4.11 | CWE-79 |
| `CVE-2026-65899` | 6.1 | `javascript/DOMPurify@3.3.2` | 3.4.9 | CWE-693 |
| `CVE-2026-65900` | 6.1 | `javascript/DOMPurify@3.3.2` | 3.4.8 | CWE-79 |
| `CVE-2026-65901` | 6.1 | `javascript/DOMPurify@3.3.2` | 3.4.7 | CWE-79 |
| `CVE-2026-65903` | 6.1 | `javascript/DOMPurify@3.3.2` | 3.4.0 | CWE-697 |
| `CVE-2026-66010` | 6.1 | `javascript/DOMPurify@3.3.2` | 3.4.12 | CWE-79 |
| `CVE-2026-41238` | — | `javascript/DOMPurify@3.3.2` | — | — |
| `CVE-2026-41239` | — | `javascript/DOMPurify@3.3.2` | — | — |
| `CVE-2026-75838` | — | `javascript/DOMPurify@3.3.2` | — | — |
| `CVE-2026-73180` | 6.8 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.25 | CWE-613 |
| `CVE-2026-55955` | 6.5 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.23 | CWE-287 |
| `CVE-2026-55956` | 6.5 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.23 | CWE-285 |
| `CVE-2026-50229` | 6.1 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.23 | CWE-80 |
| `CVE-2026-66299` | 5.3 | `maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.55` | 11.0.25 | CWE-400 |

### ⚪ LOW (3)

| CVE | CVSS | Dependência | Correção a partir de | CWE |
|---|---|---|---|---|
| `CVE-2026-59314` | 3.7 | `maven/org.springframework/spring-core@6.2.19` | — | NVD-CWE-Other, CWE-113 |
| `CVE-2026-109345` | — | `javascript/DOMPurify@3.3.2` | — | — |
| `CVE-2026-116802` | — | `javascript/DOMPurify@3.3.2` | — | — |


## Recomendações

- Tratar vulnerabilidades/hotspots novos antes da próxima release; a análise de CVEs deve ser destravada com a secret `NVD_API_KEY` (ver seção 4).
