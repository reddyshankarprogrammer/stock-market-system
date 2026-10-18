#!/usr/bin/env bash
# Shared helpers for the Kubernetes platform scripts. Source it, do not execute it:
#   source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

# shellcheck disable=SC2034  # variables are used by the scripts that source this file
set -Eeuo pipefail

SCRIPT_LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_LIB_DIR}/../.." && pwd)"
K8S_DIR="${REPO_ROOT}/k8s"

# shellcheck source=versions.env
source "${SCRIPT_LIB_DIR}/versions.env"

export KUBECONFIG="${KUBECONFIG:-${HOME}/.kube/config}"

# Namespaces of the platform (k8s/namespaces/namespaces.yaml is the source of truth).
NS_PLATFORM="platform"
NS_MONITORING="monitoring"
NS_LOGGING="logging"
NS_DATABASE="database"
NS_MESSAGING="messaging"
NS_APP="stock-market"
NS_ARGOCD="argocd"

if [[ -t 1 ]]; then
  C_RED=$'\e[31m'; C_GREEN=$'\e[32m'; C_YELLOW=$'\e[33m'; C_BLUE=$'\e[34m'; C_RESET=$'\e[0m'
else
  C_RED=""; C_GREEN=""; C_YELLOW=""; C_BLUE=""; C_RESET=""
fi

log()  { printf '%s[%s] %s%s\n' "${C_BLUE}" "$(date '+%H:%M:%S')" "$*" "${C_RESET}"; }
ok()   { printf '%s[ OK ] %s%s\n' "${C_GREEN}" "$*" "${C_RESET}"; }
warn() { printf '%s[WARN] %s%s\n' "${C_YELLOW}" "$*" "${C_RESET}" >&2; }
die()  { printf '%s[FAIL] %s%s\n' "${C_RED}" "$*" "${C_RESET}" >&2; exit 1; }

trap 'die "Command failed at ${BASH_SOURCE[0]}:${LINENO}: ${BASH_COMMAND}"' ERR

require_root() {
  [[ "${EUID}" -eq 0 ]] || die "Run as root: sudo $0 $*"
}

require_cmd() {
  local cmd
  for cmd in "$@"; do
    command -v "${cmd}" >/dev/null 2>&1 || die "Required command not found: ${cmd}"
  done
}

require_cluster() {
  require_cmd kubectl helm
  kubectl cluster-info >/dev/null 2>&1 || die "Cannot reach the cluster (KUBECONFIG=${KUBECONFIG}). Run scripts/install-k3s.sh first."
}

# Ubuntu 24.04 is the supported target; other distributions only get a warning.
check_os() {
  if [[ -r /etc/os-release ]]; then
    # shellcheck disable=SC1091
    source /etc/os-release
    if [[ "${ID:-}" != "ubuntu" || "${VERSION_ID:-}" != "24.04" ]]; then
      warn "Tested on Ubuntu 24.04 LTS; detected ${PRETTY_NAME:-unknown}."
    fi
  fi
}

ensure_namespaces() {
  kubectl apply -k "${K8S_DIR}/namespaces" >/dev/null
}

# helm_install <release> <chart> <namespace> <version|""> [extra helm args...]
helm_install() {
  local release="$1" chart="$2" namespace="$3" version="$4"
  shift 4
  local version_args=()
  [[ -n "${version}" ]] && version_args=(--version "${version}")
  log "helm upgrade --install ${release} (${chart} ${version:-latest}) in ${namespace}"
  helm upgrade --install "${release}" "${chart}" \
    --namespace "${namespace}" --create-namespace \
    "${version_args[@]}" \
    --wait --timeout "${HELM_TIMEOUT:-15m}" \
    "$@"
  ok "${release} installed: $(helm list -n "${namespace}" -f "^${release}\$" -o json | jq -r '.[0].chart + " (app " + .[0].app_version + ")"')"
}

# wait_for_rollout <namespace> <kind/name> [timeout]
wait_for_rollout() {
  kubectl -n "$1" rollout status "$2" --timeout "${3:-300s}"
}

# Reads KEY from the repo-root .env (the same file docker compose and the services use).
env_value() {
  local key="$1" file="${ENV_FILE:-${REPO_ROOT}/.env}"
  [[ -f "${file}" ]] || return 1
  # Last assignment wins, like docker compose; strips optional surrounding quotes and CR (CRLF files).
  local line
  line="$(grep -E "^${key}=" "${file}" | tail -n1 || true)"
  [[ -n "${line}" ]] || return 1
  printf '%s' "${line#*=}" | tr -d '\r' | sed -E 's/^"(.*)"$/\1/; s/^'\''(.*)'\''$/\1/'
}
