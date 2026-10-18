#!/usr/bin/env bash
# Upgrades K3s on this node in place: etcd snapshot, drain, upgrade, uncordon, verify.
# Run it on server nodes one at a time first, then on agents. Kubernetes supports
# upgrading one minor version at a time (for example v1.33 -> v1.34), never skipping one.
#
# Usage:
#   sudo ./scripts/upgrade-k3s.sh                         # latest of K3S_CHANNEL (default: stable)
#   sudo K3S_VERSION=v1.35.2+k3s1 ./scripts/upgrade-k3s.sh
#   sudo ./scripts/upgrade-k3s.sh --no-drain               # single-node clusters

source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

DRAIN=true
for arg in "$@"; do
  case "${arg}" in
    --no-drain) DRAIN=false ;;
    -h|--help) sed -n '2,10p' "$0"; exit 0 ;;
    *) die "Unknown argument: ${arg}" ;;
  esac
done

require_root
command -v k3s >/dev/null || die "K3s is not installed; use install-k3s.sh"
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

NODE="$(hostname)"
IS_SERVER=false
systemctl is-active --quiet k3s && IS_SERVER=true
CURRENT="$(k3s --version | awk 'NR==1{print $3}')"

if [[ -n "${K3S_VERSION}" ]]; then
  TARGET="${K3S_VERSION}"
else
  TARGET="$(curl -fsSL https://update.k3s.io/v1-release/channels | jq -r --arg c "${K3S_CHANNEL}" '.data[] | select(.id==$c) | .latest')"
fi
[[ -n "${TARGET}" && "${TARGET}" != "null" ]] || die "Could not resolve the target K3s version"

log "Node ${NODE}: ${CURRENT} -> ${TARGET}"
if [[ "${CURRENT}" == "${TARGET}" ]]; then
  ok "Already on ${TARGET}"
  exit 0
fi

current_minor="$(cut -d. -f2 <<<"${CURRENT}")"
target_minor="$(cut -d. -f2 <<<"${TARGET}")"
if (( target_minor - current_minor > 1 )); then
  die "Skipping minor versions is not supported (${CURRENT} -> ${TARGET}); upgrade one minor at a time."
fi

if [[ "${IS_SERVER}" == true ]]; then
  log "Taking etcd snapshot pre-upgrade-${TARGET//+/-}"
  k3s etcd-snapshot save --name "pre-upgrade-${TARGET//+/-}"
fi

if [[ "${DRAIN}" == true ]]; then
  log "Draining ${NODE}"
  kubectl drain "${NODE}" --ignore-daemonsets --delete-emptydir-data --timeout=600s
fi

log "Upgrading K3s"
if [[ "${IS_SERVER}" == true ]]; then
  # config.yaml written by install-k3s.sh is reused by the installer.
  curl -sfL https://get.k3s.io | INSTALL_K3S_VERSION="${TARGET}" sh -s - server
else
  curl -sfL https://get.k3s.io | INSTALL_K3S_VERSION="${TARGET}" sh -s - agent
fi

log "Waiting for ${NODE} to report ${TARGET}"
for _ in $(seq 1 60); do
  version="$(kubectl get node "${NODE}" -o jsonpath='{.status.nodeInfo.kubeletVersion}' 2>/dev/null || true)"
  [[ "${version}" == "${TARGET}" ]] && break
  sleep 5
done
kubectl wait --for=condition=Ready "node/${NODE}" --timeout=300s

[[ "${DRAIN}" == true ]] && kubectl uncordon "${NODE}"

ok "Upgraded ${NODE} to $(k3s --version | awk 'NR==1{print $3}')"
"${REPO_ROOT}/scripts/cluster-health-check.sh"
