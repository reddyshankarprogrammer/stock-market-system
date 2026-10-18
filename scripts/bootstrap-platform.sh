#!/usr/bin/env bash
# Installs the complete platform on a fresh K3s cluster, in dependency order:
#   namespaces -> secrets -> monitoring (Prometheus CRDs) -> ingress + cert-manager -> logging
#   -> Kafka operator + cluster -> Argo CD (root app syncs data stores, ingress, services)
# Every step is idempotent: re-run after a failure.
#
# Usage:
#   sudo ./scripts/install-k3s.sh          # once per node
#   ./scripts/bootstrap-platform.sh        # as the user owning ~/.kube/config
#   GITOPS_REPO_TOKEN=... ./scripts/bootstrap-platform.sh   # private repository

source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_cluster
SCRIPTS="${REPO_ROOT}/scripts"

step() { printf '\n%s======== %s ========%s\n' "${C_BLUE}" "$*" "${C_RESET}"; }

step "1/7 Namespaces and priority classes"
ensure_namespaces
ok "namespaces applied"

step "2/7 Secrets from .env"
"${SCRIPTS}/create-secrets.sh"

step "3/7 Monitoring (Prometheus, Alertmanager, Grafana)"
"${SCRIPTS}/install-monitoring.sh"

step "4/7 Ingress (NGINX) and certificates (cert-manager)"
"${SCRIPTS}/install-ingress.sh"

step "5/7 Logging (Loki, Alloy)"
"${SCRIPTS}/install-logging.sh"

step "6/7 Kafka (Strimzi)"
"${SCRIPTS}/install-kafka.sh"

step "7/7 Argo CD and GitOps applications"
"${SCRIPTS}/install-argocd.sh"

log "Waiting for Argo CD to sync the applications (up to 15 minutes)"
for _ in $(seq 1 90); do
  pending="$(kubectl -n "${NS_ARGOCD}" get applications -o json | jq '[.items[] |
    select(.status.sync.status != "Synced" or .status.health.status != "Healthy")] | length')"
  [[ "${pending}" == "0" ]] && break
  sleep 10
done
kubectl -n "${NS_ARGOCD}" get applications

"${SCRIPTS}/cluster-health-check.sh" || warn "Some checks failed: see docs/operations-runbook.md"
