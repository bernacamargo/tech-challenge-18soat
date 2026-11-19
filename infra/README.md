# Infraestrutura AWS (Terraform) — Fase 2

O enunciado da Fase 2 aceita cluster Kubernetes **local ou cloud**. O time escolheu **AWS**: EKS (Kubernetes gerenciado) + RDS PostgreSQL 17, alinhado ao ADR 0025 e à imagem já publicada no GHCR.

Este diretório **só entrega o código IaC**. Não há apply automático neste ciclo; o provisionamento é manual quando houver conta e credenciais.

## Recursos criados

| Recurso | Descrição |
|---|---|
| VPC + 2 AZs | Subnets públicas (IGW) e privadas (NAT único) |
| EKS | Control plane + node group gerenciado (`t3.medium`, desired 2) |
| RDS PostgreSQL 17 | Instância pequena em subnet privada; SG só aceita o cluster EKS |
| IAM | Roles do control plane e dos nodes (EKS + CNI + ECR read-only) |

## Pré-requisitos

- Terraform >= 1.5
- AWS CLI autenticado (`aws sts get-caller-identity`)
- Permissões para VPC, EKS, EC2, RDS, IAM e EIP/NAT
- Senha do RDS via `TF_VAR_rds_password` ou `terraform.tfvars` **local** (não versionado)

```bash
cp terraform.tfvars.example terraform.tfvars
# edite rds_password
```

## Apply

```bash
cd infra
terraform init
terraform plan
terraform apply
```

Outputs úteis:

- `eks_cluster_name` → `aws eks update-kubeconfig --name <nome> --region <região>`
- `jdbc_url_template` → ConfigMap/Secret do diretório `/k8s`
- `rds_address` / `rds_port` → Flyway no workflow de deploy

## Destroy (evitar custo esquecido)

NAT Gateway, EKS e RDS geram custo contínuo. Quando terminar a demo:

```bash
cd infra
terraform destroy
```

Confirme que não restam EIPs, load balancers ou volumes órfãos no console AWS.

## Relação com o restante do repo

1. `terraform apply` provisiona cluster + banco.
2. Manifestos em `/k8s` sobem a aplicação (imagem `ghcr.io/bernacamargo/tech-challenge-18soat:<versão>`).
3. Workflow `deploy.yml` aplica migrations (Flyway) e `kubectl apply` (quando OIDC/secrets existirem).

Issuer/JWKS do Keycloak e SMTP **não** são provisionados aqui — veja issue #115 e variáveis `OAUTH2_*` / `NOTIFICACOES_*` nos manifests.
