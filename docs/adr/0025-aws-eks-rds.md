# ADR-0025: Runtime AWS com EKS, RDS e deploy separado do release

- **Status**: Aceito
- **Data**: 2026-11-18
- **Participantes**: Grupo 117 (proposto por Bernardo Camargo)

## Contexto

A Fase 2 exige cluster Kubernetes (local ou cloud), banco, IaC em `/infra`, manifests em `/k8s` e pipeline CI/CD com deploy. A ADR 0019 já fixou a publicação da imagem no GHCR sem deploy. Precisamos escolher o runtime cloud e onde mora o deploy.

## Decisão

1. Provisionar na **AWS** via Terraform em [`infra/`](../../infra/): VPC mínima, **EKS** gerenciado e **RDS PostgreSQL 17**.
2. Manter a imagem da aplicação no **GHCR** (ADR 0019); o cluster puxa `ghcr.io/bernacamargo/tech-challenge-18soat:<versão>`.
3. O workflow de **release não ganha passos de hospedagem**. O deploy fica em [`.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml) (`workflow_dispatch`, Environment `aws`), autenticado por **OIDC**, aplicando Flyway e os manifests de `/k8s`. O mapa do enunciado fica: `ci.yml` (build/testes) + `release.yml` (imagem) + `deploy.yml` (banco + cluster).
4. Keycloak, MailPit e observabilidade **não** entram no Terraform mínimo da Fase 2; issuer/JWKS e SMTP ficam configuráveis por ConfigMap/Secret (issue #115).

## Consequências

- Positivas: alinha ao enunciado (IaC + k8s + banco + CI/CD); reutiliza o artefato já versionado no GHCR; destroy documentado reduz risco de custo esquecido.
- Negativas / trade-offs aceitos: EKS + NAT têm custo não trivial; sem apply real neste ciclo até existir conta/OIDC; IdP e e-mail ficam fora do provisionamento mínimo.
- Alternativas consideradas e por que foram descartadas:
  - **Cluster local (kind/k3d)**: atende o PDF, mas a entrega do grupo mira cloud AWS e diagramas/HPA no ambiente alvo.
  - **EKS Fargate**: menos nodes para operar, porém menos previsível para demos de HPA com métricas de pod.
  - **Deploy dentro do `release.yml`**: acoplaria corte de versão a credenciais AWS e quebraria o contrato da ADR 0019.
