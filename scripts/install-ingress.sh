#!/usr/bin/env bash
# Installs the ingress layer into the platform namespace:
#   - F5 NGINX Ingress Controller (OSS), exposed on ports 80/443 of the node by K3s ServiceLB
#   - cert-manager with the Let's Encrypt and local-CA ClusterIssuers (k8s/ingress/cluster-issuer.yaml)
# The Ingress objects themselves are applied by Argo CD (application "ingress").
#
# Usage: ./scripts/install-ingress.sh

source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_cluster
ensure_namespaces

# ServiceMonitors only if the Prometheus Operator CRDs exist (install-monitoring.sh ran first).
SERVICE_MONITORS=false
kubectl get crd servicemonitors.monitoring.coreos.com >/dev/null 2>&1 && SERVICE_MONITORS=true
log "ServiceMonitors enabled: ${SERVICE_MONITORS}"

log "Adding the jetstack Helm repository"
helm repo add jetstack https://charts.jetstack.io --force-update >/dev/null
helm repo update jetstack >/dev/null

helm_install cert-manager jetstack/cert-manager "${NS_PLATFORM}" "${CERT_MANAGER_CHART_VERSION}" \
  --values "${K8S_DIR}/ingress/cert-manager-values.yaml" \
  --set "prometheus.servicemonitor.enabled=${SERVICE_MONITORS}"

helm_install nginx-ingress "${NGINX_INGRESS_CHART}" "${NS_PLATFORM}" "${NGINX_INGRESS_CHART_VERSION}" \
  --values "${K8S_DIR}/ingress/nginx-ingress-values.yaml" \
  --set "prometheus.serviceMonitor.create=${SERVICE_MONITORS}"

log "Applying ClusterIssuers"
# The cert-manager webhook can need a few seconds after its rollout before it admits resources.
for attempt in $(seq 1 12); do
  if kubectl apply -f "${K8S_DIR}/ingress/cluster-issuer.yaml"; then break; fi
  [[ "${attempt}" -eq 12 ]] && die "cert-manager webhook did not become ready"
  sleep 10
done
kubectl -n "${NS_PLATFORM}" wait certificate/stockmarket-local-ca --for=condition=Ready --timeout=120s
kubectl wait clusterissuer --all --for=condition=Ready --timeout=180s

lb_ip="$(kubectl -n "${NS_PLATFORM}" get svc -l app.kubernetes.io/instance=nginx-ingress \
  -o jsonpath='{.items[0].status.loadBalancer.ingress[0].ip}' 2>/dev/null || true)"
ok "Ingress ready on ${lb_ip:-<pending>}:80/443"
cat <<EOF

Add the host names to DNS, or on client machines to /etc/hosts (C:\\Windows\\System32\\drivers\\etc\\hosts):
  ${lb_ip:-<node-ip>}  api.stockmarket.local grafana.stockmarket.local argocd.stockmarket.local

Trust the local CA on clients:
  kubectl -n ${NS_PLATFORM} get secret stockmarket-local-ca -o jsonpath='{.data.ca\\.crt}' | base64 -d > stockmarket-ca.crt
EOF
