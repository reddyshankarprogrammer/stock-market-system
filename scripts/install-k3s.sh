#!/usr/bin/env bash
# Installs a production-configured K3s node on Ubuntu 24.04 LTS, plus kubectl and Helm,
# configures kubeconfig for the invoking user, creates the platform namespaces and
# validates that the cluster is ready.
#
# Usage:
#   sudo ./scripts/install-k3s.sh                        # first (server) node
#   sudo K3S_VERSION=v1.34.9+k3s1 ./scripts/install-k3s.sh   # pin an exact version
#   sudo ./scripts/install-k3s.sh --role agent --server https://10.0.0.10:6443 --token <node-token>
#
# Environment:
#   K3S_CHANNEL / K3S_VERSION   see scripts/lib/versions.env
#   K3S_TLS_SAN                 extra comma-separated SANs for the API server certificate
#   K3S_NODE_IP                 node IP to advertise (default: auto-detected)

source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

ROLE="server"
SERVER_URL=""
JOIN_TOKEN=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --role)   ROLE="$2"; shift 2 ;;
    --server) SERVER_URL="$2"; shift 2 ;;
    --token)  JOIN_TOKEN="$2"; shift 2 ;;
    -h|--help) sed -n '2,16p' "$0"; exit 0 ;;
    *) die "Unknown argument: $1" ;;
  esac
done

[[ "${ROLE}" == "server" || "${ROLE}" == "agent" ]] || die "--role must be server or agent"
if [[ "${ROLE}" == "agent" ]]; then
  [[ -n "${SERVER_URL}" && -n "${JOIN_TOKEN}" ]] || die "--role agent needs --server and --token"
fi

require_root
check_os

TARGET_USER="${SUDO_USER:-root}"
TARGET_HOME="$(getent passwd "${TARGET_USER}" | cut -d: -f6)"

install_prerequisites() {
  log "Installing OS prerequisites"
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -qq
  apt-get install -y -qq curl ca-certificates jq gnupg apt-transport-https \
    nfs-common open-iscsi gettext-base mysql-client >/dev/null

  # Kubernetes needs swap off and bridged traffic visible to iptables.
  swapoff -a
  sed -ri '/\sswap\s/s/^#?/#/' /etc/fstab

  cat >/etc/modules-load.d/k3s.conf <<'EOF'
overlay
br_netfilter
EOF
  modprobe overlay
  modprobe br_netfilter

  cat >/etc/sysctl.d/90-k3s.conf <<'EOF'
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
fs.inotify.max_user_instances       = 8192
fs.inotify.max_user_watches         = 524288
vm.max_map_count                    = 262144
EOF
  sysctl --system >/dev/null
  ok "Prerequisites installed"
}

configure_firewall() {
  if command -v ufw >/dev/null && ufw status | grep -q "Status: active"; then
    log "Opening K3s ports in ufw"
    ufw allow 6443/tcp comment 'k3s api'        >/dev/null
    ufw allow 80/tcp comment 'ingress http'     >/dev/null
    ufw allow 443/tcp comment 'ingress https'   >/dev/null
    ufw allow 10250/tcp comment 'kubelet'       >/dev/null
    ufw allow 8472/udp comment 'flannel vxlan'  >/dev/null
    ufw allow 2379:2380/tcp comment 'etcd'      >/dev/null
    ufw allow from 10.42.0.0/16 comment 'k3s pods'     >/dev/null
    ufw allow from 10.43.0.0/16 comment 'k3s services' >/dev/null
    ok "Firewall rules added"
  fi
}

write_server_config() {
  log "Writing /etc/rancher/k3s/config.yaml"
  mkdir -p /etc/rancher/k3s
  local tls_san_lines=""
  if [[ -n "${K3S_TLS_SAN:-}" ]]; then
    local san
    IFS=',' read -ra sans <<<"${K3S_TLS_SAN}"
    for san in "${sans[@]}"; do tls_san_lines+="  - ${san}"$'\n'; done
  fi

  # Audit log of every API request (metadata level), rotated by the API server.
  cat >/etc/rancher/k3s/audit-policy.yaml <<'EOF'
apiVersion: audit.k8s.io/v1
kind: Policy
omitStages: ["RequestReceived"]
rules:
  - level: None
    users: ["system:kube-proxy"]
    verbs: ["watch"]
  - level: None
    nonResourceURLs: ["/healthz*", "/readyz*", "/livez*", "/version", "/metrics"]
  - level: Metadata
    resources:
      - group: ""
        resources: ["secrets", "configmaps"]
  - level: Metadata
EOF
  mkdir -p /var/lib/rancher/k3s/server/logs

  cat >/etc/rancher/k3s/config.yaml <<EOF
# Managed by scripts/install-k3s.sh
write-kubeconfig-mode: "0600"
# Embedded etcd (instead of SQLite) so etcd snapshots can restore the whole cluster
# and further server nodes can join for HA.
cluster-init: true
etcd-snapshot-schedule-cron: "0 */6 * * *"
etcd-snapshot-retention: 28
etcd-snapshot-compress: true
# Secrets are encrypted at rest in etcd.
secrets-encryption: true
# NGINX Ingress Controller replaces the bundled Traefik.
disable:
  - traefik
${K3S_NODE_IP:+node-ip: ${K3S_NODE_IP}}
tls-san:
  - "$(hostname -f)"
${tls_san_lines}kube-apiserver-arg:
  - "audit-log-path=/var/lib/rancher/k3s/server/logs/audit.log"
  - "audit-policy-file=/etc/rancher/k3s/audit-policy.yaml"
  - "audit-log-maxage=30"
  - "audit-log-maxbackup=10"
  - "audit-log-maxsize=100"
kubelet-arg:
  - "max-pods=150"
  - "image-gc-high-threshold=85"
  - "image-gc-low-threshold=70"
  - "eviction-hard=memory.available<300Mi,nodefs.available<10%"
EOF
  chmod 600 /etc/rancher/k3s/config.yaml
}

install_k3s() {
  local install_env=()
  if [[ -n "${K3S_VERSION}" ]]; then
    install_env+=("INSTALL_K3S_VERSION=${K3S_VERSION}")
  else
    install_env+=("INSTALL_K3S_CHANNEL=${K3S_CHANNEL}")
  fi

  if [[ "${ROLE}" == "server" ]]; then
    write_server_config
    log "Installing K3s server (${K3S_VERSION:-channel ${K3S_CHANNEL}})"
    curl -sfL https://get.k3s.io | env "${install_env[@]}" sh -s - server
  else
    log "Joining K3s agent to ${SERVER_URL}"
    curl -sfL https://get.k3s.io | env "${install_env[@]}" K3S_URL="${SERVER_URL}" K3S_TOKEN="${JOIN_TOKEN}" sh -s - agent
  fi
  ok "K3s installed: $(k3s --version | head -n1)"
}

install_kubectl() {
  # K3s ships kubectl as a symlink to the k3s binary; install the upstream binary only if missing.
  if ! command -v kubectl >/dev/null; then
    local version
    version="$(curl -fsSL https://dl.k8s.io/release/stable.txt)"
    log "Installing kubectl ${version}"
    curl -fsSLo /usr/local/bin/kubectl "https://dl.k8s.io/release/${version}/bin/linux/amd64/kubectl"
    chmod 0755 /usr/local/bin/kubectl
  fi
  ok "kubectl: $(kubectl version --client -o json | jq -r .clientVersion.gitVersion)"
}

install_helm() {
  if ! command -v helm >/dev/null; then
    log "Installing Helm ${HELM_VERSION:-latest 3.x}"
    curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 -o /tmp/get-helm-3
    chmod 700 /tmp/get-helm-3
    DESIRED_VERSION="${HELM_VERSION}" /tmp/get-helm-3
    rm -f /tmp/get-helm-3
  fi
  ok "helm: $(helm version --short)"
}

configure_kubeconfig() {
  log "Configuring kubeconfig for ${TARGET_USER}"
  install -d -m 0700 -o "${TARGET_USER}" -g "${TARGET_USER}" "${TARGET_HOME}/.kube"
  install -m 0600 -o "${TARGET_USER}" -g "${TARGET_USER}" /etc/rancher/k3s/k3s.yaml "${TARGET_HOME}/.kube/config"
  export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

  local profile="${TARGET_HOME}/.bashrc"
  if ! grep -q 'kubectl completion bash' "${profile}" 2>/dev/null; then
    cat >>"${profile}" <<'EOF'

# Added by stock-market-system scripts/install-k3s.sh
export KUBECONFIG="$HOME/.kube/config"
source <(kubectl completion bash)
source <(helm completion bash)
alias k=kubectl
complete -o default -F __start_kubectl k
EOF
  fi
  ok "kubeconfig written to ${TARGET_HOME}/.kube/config"
}

wait_for_cluster() {
  log "Waiting for the node to become Ready"
  for _ in $(seq 1 60); do
    kubectl get nodes >/dev/null 2>&1 && break
    sleep 5
  done
  kubectl wait --for=condition=Ready node --all --timeout=300s

  log "Waiting for core add-ons"
  for deploy in coredns local-path-provisioner metrics-server; do
    kubectl -n kube-system rollout status "deployment/${deploy}" --timeout=300s
  done
  ok "Cluster is ready"
}

install_prerequisites
configure_firewall
install_k3s

if [[ "${ROLE}" == "server" ]]; then
  install_kubectl
  install_helm
  configure_kubeconfig
  wait_for_cluster
  ensure_namespaces
  ok "Namespaces: $(kubectl get ns -l app.kubernetes.io/part-of=stock-market-system -o name | tr '\n' ' ')"
  "${REPO_ROOT}/scripts/cluster-health-check.sh" || warn "Health check reported problems (expected before the platform is installed)."

  cat <<EOF

${C_GREEN}K3s is installed.${C_RESET}
  Node token for agents : sudo cat /var/lib/rancher/k3s/server/node-token
  Next steps            : ./scripts/bootstrap-platform.sh   (or the install-*.sh scripts one by one)
EOF
else
  ok "Agent joined; verify on the server with: kubectl get nodes"
fi
