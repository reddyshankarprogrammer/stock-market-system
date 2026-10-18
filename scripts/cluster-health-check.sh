#!/usr/bin/env bash
# Read-only health report of the K3s cluster and the stock-market platform.
# Exit code 0 = healthy, 1 = at least one check failed (usable from cron or CI).
#
# Usage:
#   ./scripts/cluster-health-check.sh            # full report
#   ./scripts/cluster-health-check.sh --quiet    # only failures and the summary

source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"
trap - ERR
set +e

QUIET=false
[[ "${1:-}" == "--quiet" ]] && QUIET=true

FAILURES=0
WARNINGS=0

pass()    { [[ "${QUIET}" == true ]] || ok "$*"; }
fail()    { printf '%s[FAIL] %s%s\n' "${C_RED}" "$*" "${C_RESET}"; FAILURES=$((FAILURES + 1)); }
caution() { warn "$*"; WARNINGS=$((WARNINGS + 1)); }
section() { [[ "${QUIET}" == true ]] || printf '\n%s== %s ==%s\n' "${C_BLUE}" "$*" "${C_RESET}"; }

require_cmd kubectl jq

section "API server"
if kubectl get --raw='/readyz' >/dev/null 2>&1; then
  pass "API server ready ($(kubectl version -o json 2>/dev/null | jq -r .serverVersion.gitVersion))"
else
  fail "API server not ready"
  echo "Summary: API server unreachable"; exit 1
fi

section "Nodes"
while read -r name ready pressure; do
  if [[ "${ready}" == "True" ]]; then pass "node ${name} Ready"; else fail "node ${name} NotReady"; fi
  [[ -n "${pressure}" ]] && fail "node ${name} under pressure: ${pressure}"
done < <(kubectl get nodes -o json | jq -r '.items[] |
  [.metadata.name,
   (.status.conditions[] | select(.type=="Ready") | .status),
   ([.status.conditions[] | select(.type!="Ready" and .status=="True") | .type] | join(","))] | @tsv')

section "Node resources"
if kubectl top nodes >/dev/null 2>&1; then
  while read -r name cpu mem; do
    cpu="${cpu%\%}"; mem="${mem%\%}"
    if (( cpu > 85 || mem > 85 )); then caution "node ${name} cpu ${cpu}% mem ${mem}%"; else pass "node ${name} cpu ${cpu}% mem ${mem}%"; fi
  done < <(kubectl top nodes --no-headers | awk '{print $1, $3, $5}')
else
  caution "metrics-server not answering (kubectl top nodes)"
fi

section "Core add-ons (kube-system)"
for deploy in coredns local-path-provisioner metrics-server; do
  if kubectl -n kube-system rollout status "deployment/${deploy}" --timeout=5s >/dev/null 2>&1; then
    pass "${deploy}"
  else
    fail "${deploy} not available"
  fi
done

section "Namespaces"
for ns in "${NS_PLATFORM}" "${NS_MONITORING}" "${NS_LOGGING}" "${NS_DATABASE}" "${NS_MESSAGING}" "${NS_APP}" "${NS_ARGOCD}"; do
  if kubectl get ns "${ns}" >/dev/null 2>&1; then pass "namespace ${ns}"; else fail "namespace ${ns} missing"; fi
done

section "Workloads"
for ns in "${NS_PLATFORM}" "${NS_MONITORING}" "${NS_LOGGING}" "${NS_DATABASE}" "${NS_MESSAGING}" "${NS_APP}" "${NS_ARGOCD}"; do
  kubectl get ns "${ns}" >/dev/null 2>&1 || continue
  while read -r kind name desired available; do
    if [[ "${desired}" == "0" ]]; then
      pass "${ns}/${kind}/${name} scaled to 0"
    elif [[ "${available}" -ge "${desired}" ]]; then
      pass "${ns}/${kind}/${name} ${available}/${desired}"
    else
      fail "${ns}/${kind}/${name} ${available}/${desired} available"
    fi
  done < <(kubectl -n "${ns}" get deploy,statefulset -o json | jq -r '.items[] |
      [.kind, .metadata.name, (.spec.replicas // 0), (.status.availableReplicas // .status.readyReplicas // 0)] | @tsv')
done

section "Unhealthy pods"
bad_pods="$(kubectl get pods -A -o json | jq -r '.items[] |
  select(.status.phase != "Succeeded") |
  select(.status.phase != "Running" or ([.status.containerStatuses[]? | select(.ready == false)] | length > 0)) |
  "\(.metadata.namespace)/\(.metadata.name) \(.status.phase) \([.status.containerStatuses[]?.state | keys[0]] | join(","))"')"
if [[ -z "${bad_pods}" ]]; then pass "all pods running and ready"; else while read -r line; do fail "pod ${line}"; done <<<"${bad_pods}"; fi

restarts="$(kubectl get pods -A -o json | jq -r '.items[] | . as $p | .status.containerStatuses[]? |
  select(.restartCount > 5) | "\($p.metadata.namespace)/\($p.metadata.name) container \(.name) restarted \(.restartCount)x"')"
[[ -n "${restarts}" ]] && while read -r line; do caution "${line}"; done <<<"${restarts}"

section "Storage"
# local-path binds a volume only when the first pod uses it (WaitForFirstConsumer): a PVC no pod
# has claimed yet (no selected-node annotation, e.g. mysql-backup before the first backup) is fine.
pending_pvc="$(kubectl get pvc -A -o json | jq -r '.items[] | select(.status.phase != "Bound") |
  "\(.metadata.namespace)/\(.metadata.name) \(.status.phase) \(.metadata.annotations["volume.kubernetes.io/selected-node"] // "unclaimed")"')"
if [[ -z "${pending_pvc}" ]]; then
  pass "all PVCs bound"
else
  while read -r pvc phase node; do
    if [[ "${node}" == "unclaimed" ]]; then caution "pvc ${pvc} ${phase}: waiting for its first pod"; else fail "pvc ${pvc} ${phase} on ${node}"; fi
  done <<<"${pending_pvc}"
fi
root_use="$(df --output=pcent /var/lib/rancher 2>/dev/null | tail -n1 | tr -dc '0-9')"
[[ -n "${root_use}" ]] && { if (( root_use > 85 )); then caution "/var/lib/rancher ${root_use}% full"; else pass "/var/lib/rancher ${root_use}% used"; fi; }

section "Certificates"
if kubectl get crd certificates.cert-manager.io >/dev/null 2>&1; then
  not_ready="$(kubectl get certificates -A -o json | jq -r '.items[] |
    select([.status.conditions[]? | select(.type=="Ready" and .status=="True")] | length == 0) |
    "\(.metadata.namespace)/\(.metadata.name)"')"
  if [[ -z "${not_ready}" ]]; then pass "all certificates ready"; else while read -r c; do fail "certificate ${c} not ready"; done <<<"${not_ready}"; fi
else
  caution "cert-manager not installed"
fi

section "Kafka"
if kubectl get crd kafkas.kafka.strimzi.io >/dev/null 2>&1; then
  kafka_ready="$(kubectl -n "${NS_MESSAGING}" get kafka stock-market-kafka -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}' 2>/dev/null)"
  if [[ "${kafka_ready}" == "True" ]]; then pass "Kafka cluster ready"; else fail "Kafka cluster not ready (${kafka_ready:-absent})"; fi
  topics_not_ready="$(kubectl -n "${NS_MESSAGING}" get kafkatopics -o json | jq -r '.items[] |
    select([.status.conditions[]? | select(.type=="Ready" and .status=="True")] | length == 0) | .metadata.name')"
  [[ -n "${topics_not_ready}" ]] && while read -r t; do fail "topic ${t} not ready"; done <<<"${topics_not_ready}"
else
  caution "Strimzi not installed"
fi

section "Argo CD applications"
if kubectl get crd applications.argoproj.io >/dev/null 2>&1; then
  while read -r app sync health; do
    if [[ "${sync}" == "Synced" && "${health}" == "Healthy" ]]; then
      pass "app ${app} ${sync}/${health}"
    else
      fail "app ${app} ${sync}/${health}"
    fi
  done < <(kubectl -n "${NS_ARGOCD}" get applications -o json | jq -r '.items[] |
    [.metadata.name, (.status.sync.status // "Unknown"), (.status.health.status // "Unknown")] | @tsv')
else
  caution "Argo CD not installed"
fi

section "Backups"
if kubectl -n "${NS_DATABASE}" get cronjob mysql-backup >/dev/null 2>&1; then
  last="$(kubectl -n "${NS_DATABASE}" get cronjob mysql-backup -o jsonpath='{.status.lastSuccessfulTime}')"
  if [[ -z "${last}" ]]; then
    caution "mysql-backup has not succeeded yet"
  else
    age_h=$(( ( $(date +%s) - $(date -d "${last}" +%s) ) / 3600 ))
    if (( age_h > 26 )); then fail "last MySQL backup ${age_h}h ago (${last})"; else pass "last MySQL backup ${age_h}h ago"; fi
  fi
fi

section "Application health endpoints"
if kubectl get ns "${NS_APP}" >/dev/null 2>&1; then
  for svc in api-gateway user-service account-service portfolio-service order-service market-data-service notification-service; do
    port="$(kubectl -n "${NS_APP}" get svc "${svc}" -o jsonpath='{.spec.ports[?(@.name=="http")].port}' 2>/dev/null)"
    [[ -z "${port}" ]] && { caution "service ${svc} not deployed"; continue; }
    status="$(kubectl get --raw "/api/v1/namespaces/${NS_APP}/services/${svc}:${port}/proxy/actuator/health/readiness" 2>/dev/null | jq -r .status 2>/dev/null)"
    # Through the API-server proxy; the Deployment availability above is the authoritative check.
    if [[ "${status}" == "UP" ]]; then pass "${svc} readiness UP"; else caution "${svc} readiness ${status:-unreachable} via API proxy"; fi
  done
fi

printf '\nSummary: %d failure(s), %d warning(s)\n' "${FAILURES}" "${WARNINGS}"
(( FAILURES == 0 ))
