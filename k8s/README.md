# Manifestos Kubernetes — Fase 2

Recursos mínimos para subir a API da oficina no cluster provisionado por [`infra/`](../infra/) (ADR 0025).

## Conteúdo

| Arquivo | Recurso |
|---|---|
| `namespace.yaml` | Namespace `oficina` |
| `configmap.yaml` | `DB_URL`, `OAUTH2_*`, flags de notificação |
| `secret.yaml` | Placeholders de `DB_USERNAME` / `DB_PASSWORD` (trocar antes do apply real) |
| `deployment.yaml` | App (imagem GHCR), probes `/actuator/health/liveness|readiness` |
| `service.yaml` | LoadBalancer na porta 80 → 8080 |
| `hpa.yaml` | HPA por **CPU e memória** (enunciado Fase 2) |

## Antes do apply

1. Preencha `configmap.yaml` com o JDBC do RDS (`terraform output jdbc_url_template`) e o issuer/JWKS do Keycloak do ambiente (#115).
2. Substitua o Secret (não use `CHANGE_ME` em produção):

```bash
kubectl -n oficina create secret generic oficina-secrets \
  --from-literal=DB_USERNAME=oficina_app \
  --from-literal=DB_PASSWORD='...' \
  --dry-run=client -o yaml | kubectl apply -f -
```

3. Ajuste a tag da imagem em `deployment.yaml` (ou deixe o `deploy.yml` fazer `kubectl set image`).

## Apply

```bash
aws eks update-kubeconfig --name <eks_cluster_name> --region <aws_region>
kubectl apply -f k8s/
kubectl -n oficina get pods,svc,hpa
```

Ordem: Namespace → ConfigMap/Secret → Deployment → Service → HPA (o `kubectl apply -f k8s/` resolve dependências desde que o namespace exista; aplicar a pasta inteira é suficiente).

## Probes e segurança

O `SecurityConfig` libera `/actuator/health` e `/actuator/health/**` para liveness/readiness sem JWT.
