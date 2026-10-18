#!/usr/bin/env bash
# Installs centralized logging into the logging namespace:
#   - Loki (single binary, 30-day retention, filesystem storage)
#   - Grafana Alloy DaemonSet (Promtail successor) collecting every container log
# Grafana (install-monitoring.sh) already has the Loki data source (uid "loki") and the
# "Stock Market / Log Search" dashboard (search by userId, accountId, tradeId, statementId).
#
# Usage: ./scripts/install-logging.sh

source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_cluster
ensure_namespaces

SERVICE_MONITORS=false
kubectl get crd servicemonitors.monitoring.coreos.com >/dev/null 2>&1 && SERVICE_MONITORS=true

log "Adding the Grafana Helm repositories"
# Loki's chart moved to the community repository in March 2026; Alloy stays in grafana/helm-charts.
helm repo add grafana-community https://grafana-community.github.io/helm-charts --force-update >/dev/null
helm repo add grafana https://grafana.github.io/helm-charts --force-update >/dev/null
helm repo update grafana-community grafana >/dev/null

helm_install loki grafana-community/loki "${NS_LOGGING}" "${LOKI_CHART_VERSION}" \
  --values "${K8S_DIR}/logging/loki-values.yaml" \
  --set "monitoring.serviceMonitor.enabled=${SERVICE_MONITORS}"

helm_install alloy grafana/alloy "${NS_LOGGING}" "${ALLOY_CHART_VERSION}" \
  --values "${K8S_DIR}/logging/alloy-values.yaml" \
  --set "serviceMonitor.enabled=${SERVICE_MONITORS}"

kubectl -n "${NS_LOGGING}" rollout status daemonset/alloy --timeout=300s

log "Checking that Loki receives logs"
for _ in $(seq 1 30); do
  labels="$(kubectl -n "${NS_LOGGING}" exec deploy/loki-gateway -- \
    wget -qO- 'http://localhost:8080/loki/api/v1/label/namespace/values' 2>/dev/null || true)"
  [[ "${labels}" == *'"data":['*'"'* ]] && break
  sleep 10
done
if [[ "${labels:-}" == *'"data":['*'"'* ]]; then
  ok "Loki is receiving logs from namespaces: $(jq -r '.data | join(", ")' <<<"${labels}")"
else
  warn "No log streams in Loki yet; check: kubectl -n ${NS_LOGGING} logs ds/alloy"
fi

cat <<EOF

Search logs: Grafana -> Dashboards -> Stock Market -> "Stock Market / Log Search", or Explore:
  {namespace="stock-market"} | userId="<uuid>"
  {namespace="stock-market", app="account-service"} | statementId="<uuid>"
  {namespace="stock-market", level="ERROR"}
EOF
