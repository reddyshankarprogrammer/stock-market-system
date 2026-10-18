#!/usr/bin/env bash
# Removes K3s from this node, including all workloads, persistent volumes on local-path
# storage and the kubeconfig. Takes a final etcd snapshot first unless --no-snapshot.
#
# Usage:
#   sudo ./scripts/uninstall-k3s.sh [--yes] [--no-snapshot] [--keep-data]
#
#   --yes          do not ask for confirmation
#   --no-snapshot  skip the final etcd snapshot
#   --keep-data    keep /var/lib/rancher/k3s/storage (local-path volumes: MySQL, backups)

source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

ASSUME_YES=false
TAKE_SNAPSHOT=true
KEEP_DATA=false

for arg in "$@"; do
  case "${arg}" in
    --yes) ASSUME_YES=true ;;
    --no-snapshot) TAKE_SNAPSHOT=false ;;
    --keep-data) KEEP_DATA=true ;;
    -h|--help) sed -n '2,10p' "$0"; exit 0 ;;
    *) die "Unknown argument: ${arg}" ;;
  esac
done

require_root
TARGET_HOME="$(getent passwd "${SUDO_USER:-root}" | cut -d: -f6)"
SAFE_DIR="/var/backups/k3s-uninstall-$(date +%Y%m%d-%H%M%S)"

if [[ "${ASSUME_YES}" != true ]]; then
  warn "This deletes the K3s cluster on $(hostname) and every workload in it."
  read -r -p "Type the hostname to continue: " answer
  [[ "${answer}" == "$(hostname)" ]] || die "Aborted."
fi

if [[ -x /usr/local/bin/k3s-uninstall.sh ]]; then
  if [[ "${TAKE_SNAPSHOT}" == true ]]; then
    log "Taking a final etcd snapshot"
    mkdir -p "${SAFE_DIR}"
    k3s etcd-snapshot save --name pre-uninstall >/dev/null
    cp -a /var/lib/rancher/k3s/server/db/snapshots "${SAFE_DIR}/" 2>/dev/null || true
    cp -a /var/lib/rancher/k3s/server/token "${SAFE_DIR}/server-token" 2>/dev/null || true
    ok "Snapshot and server token saved to ${SAFE_DIR} (both are needed to restore)"
  fi

  if [[ "${KEEP_DATA}" == true && -d /var/lib/rancher/k3s/storage ]]; then
    mkdir -p "${SAFE_DIR}"
    log "Preserving local-path volumes in ${SAFE_DIR}/storage"
    cp -a /var/lib/rancher/k3s/storage "${SAFE_DIR}/"
  fi

  log "Running k3s-uninstall.sh"
  /usr/local/bin/k3s-uninstall.sh
elif [[ -x /usr/local/bin/k3s-agent-uninstall.sh ]]; then
  log "Running k3s-agent-uninstall.sh"
  /usr/local/bin/k3s-agent-uninstall.sh
else
  warn "K3s is not installed on this node."
fi

rm -f "${TARGET_HOME}/.kube/config"
rm -f /etc/modules-load.d/k3s.conf /etc/sysctl.d/90-k3s.conf
ok "K3s removed. Helm remains installed (/usr/local/bin/helm)."
