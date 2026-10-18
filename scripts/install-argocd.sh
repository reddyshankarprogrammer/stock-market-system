#!/usr/bin/env bash
# Installs Argo CD into the argocd namespace and bootstraps GitOps:
#   1. Helm install of argo/argo-cd with k8s/argocd/argocd-values.yaml
#   2. Repository credentials (only for a private repository)
#   3. The root application (k8s/argocd/root-application.yaml), which creates the
#      stock-market AppProject and every application in k8s/argocd/argocd-applications.yaml
#   4. The argocd CLI and the initial admin password
#
# Usage:
#   ./scripts/install-argocd.sh
#   GITOPS_REPO_TOKEN=<read-only token> ./scripts/install-argocd.sh   # private repository
#
# Environment:
#   GITOPS_REPO_URL     default https://github.com/reddyshankarprogrammer/stock-market-system.git
#   GITOPS_REPO_USER    default reddyshankarprogrammer
#   GITOPS_REPO_TOKEN   fine-grained GitHub token with "Contents: read" on the repository
#   SKIP_ROOT_APP=true  install Argo CD only

source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

GITOPS_REPO_URL="${GITOPS_REPO_URL:-https://github.com/reddyshankarprogrammer/stock-market-system.git}"
GITOPS_REPO_USER="${GITOPS_REPO_USER:-reddyshankarprogrammer}"

require_cluster
require_cmd jq curl
ensure_namespaces

SERVICE_MONITORS=false
kubectl get crd servicemonitors.monitoring.coreos.com >/dev/null 2>&1 && SERVICE_MONITORS=true

log "Adding the Argo Helm repository"
helm repo add argo https://argoproj.github.io/argo-helm --force-update >/dev/null
helm repo update argo >/dev/null

helm_install argocd argo/argo-cd "${NS_ARGOCD}" "${ARGOCD_CHART_VERSION}" \
  --values "${K8S_DIR}/argocd/argocd-values.yaml" \
  --set "server.metrics.serviceMonitor.enabled=${SERVICE_MONITORS}" \
  --set "controller.metrics.serviceMonitor.enabled=${SERVICE_MONITORS}" \
  --set "repoServer.metrics.serviceMonitor.enabled=${SERVICE_MONITORS}"

for deploy in argocd-server argocd-repo-server argocd-applicationset-controller; do
  wait_for_rollout "${NS_ARGOCD}" "deployment/${deploy}"
done
kubectl -n "${NS_ARGOCD}" rollout status statefulset/argocd-application-controller --timeout=300s

if [[ -n "${GITOPS_REPO_TOKEN:-}" ]]; then
  log "Registering the private repository ${GITOPS_REPO_URL}"
  kubectl -n "${NS_ARGOCD}" create secret generic repo-stock-market-system \
    --from-literal=type=git \
    --from-literal=url="${GITOPS_REPO_URL}" \
    --from-literal=username="${GITOPS_REPO_USER}" \
    --from-literal=password="${GITOPS_REPO_TOKEN}" \
    --dry-run=client -o yaml \
    | kubectl label --local -f - argocd.argoproj.io/secret-type=repository -o yaml \
    | kubectl apply -f -
fi

if ! command -v argocd >/dev/null; then
  version="$(helm list -n "${NS_ARGOCD}" -f '^argocd$' -o json | jq -r '.[0].app_version')"
  log "Installing the argocd CLI ${version}"
  if [[ -w /usr/local/bin ]]; then
    curl -fsSLo /usr/local/bin/argocd "https://github.com/argoproj/argo-cd/releases/download/${version}/argocd-linux-amd64"
    chmod 0755 /usr/local/bin/argocd
  else
    sudo curl -fsSLo /usr/local/bin/argocd "https://github.com/argoproj/argo-cd/releases/download/${version}/argocd-linux-amd64"
    sudo chmod 0755 /usr/local/bin/argocd
  fi
fi

if [[ "${SKIP_ROOT_APP:-false}" != true ]]; then
  log "Applying the root application (app of apps)"
  sed "s#https://github.com/reddyshankarprogrammer/stock-market-system.git#${GITOPS_REPO_URL}#" \
    "${K8S_DIR}/argocd/root-application.yaml" | kubectl apply -f -

  log "Waiting for the child applications to be created"
  for _ in $(seq 1 30); do
    count="$(kubectl -n "${NS_ARGOCD}" get applications -l app.kubernetes.io/part-of=stock-market --no-headers 2>/dev/null | wc -l)"
    (( count > 1 )) && break
    sleep 10
  done
  kubectl -n "${NS_ARGOCD}" get applications
fi

password="$(kubectl -n "${NS_ARGOCD}" get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' 2>/dev/null | base64 -d || true)"
ok "Argo CD installed"
cat <<EOF

UI      : https://argocd.stockmarket.local   or   kubectl -n ${NS_ARGOCD} port-forward svc/argocd-server 8443:80
Login   : admin / ${password:-<already rotated>}
CLI     : argocd login argocd.stockmarket.local --username admin --grpc-web
Rotate the admin password, then: kubectl -n ${NS_ARGOCD} delete secret argocd-initial-admin-secret
Token for the CD workflow (repository secret ARGOCD_AUTH_TOKEN):
          argocd proj role create-token stock-market ci-sync --expires-in 90d
EOF
