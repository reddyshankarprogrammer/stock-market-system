#!/usr/bin/env bash
# Installs Prometheus, Alertmanager and Grafana (kube-prometheus-stack) into the monitoring
# namespace, then applies the platform's ServiceMonitors, alert rules and dashboards
# (k8s/monitoring, afterwards reconciled by Argo CD application "monitoring").
#
# Usage: ./scripts/install-monitoring.sh
# Requires the grafana-admin and alertmanager-slack Secrets; this script creates them from
# .env if they do not exist yet (see scripts/create-secrets.sh).

source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_cluster
ensure_namespaces

if ! kubectl -n "${NS_MONITORING}" get secret grafana-admin >/dev/null 2>&1 \
   || ! kubectl -n "${NS_MONITORING}" get secret alertmanager-slack >/dev/null 2>&1; then
  log "Creating monitoring secrets"
  "${REPO_ROOT}/scripts/create-secrets.sh" --only monitoring
fi

log "Adding the prometheus-community Helm repository"
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts --force-update >/dev/null
helm repo update prometheus-community >/dev/null

helm_install kube-prometheus-stack prometheus-community/kube-prometheus-stack "${NS_MONITORING}" \
  "${KUBE_PROMETHEUS_STACK_CHART_VERSION}" \
  --values "${K8S_DIR}/monitoring/kube-prometheus-stack-values.yaml"

log "Applying ServiceMonitors, PrometheusRules and dashboards (k8s/monitoring)"
kubectl apply --server-side --force-conflicts -k "${K8S_DIR}/monitoring"

wait_for_rollout "${NS_MONITORING}" deployment/kube-prometheus-stack-grafana
kubectl -n "${NS_MONITORING}" wait prometheus/kube-prometheus-stack-prometheus --for=condition=Available --timeout=300s

ok "Monitoring installed"
cat <<EOF

Grafana      : https://grafana.stockmarket.local (after install-ingress.sh + Argo CD "ingress")
               or: kubectl -n ${NS_MONITORING} port-forward svc/kube-prometheus-stack-grafana 3000:80
               user: $(kubectl -n "${NS_MONITORING}" get secret grafana-admin -o jsonpath='{.data.admin-user}' | base64 -d)
               password: kubectl -n ${NS_MONITORING} get secret grafana-admin -o jsonpath='{.data.admin-password}' | base64 -d
Prometheus   : kubectl -n ${NS_MONITORING} port-forward svc/kube-prometheus-stack-prometheus 9090
Alertmanager : kubectl -n ${NS_MONITORING} port-forward svc/kube-prometheus-stack-alertmanager 9093
EOF
