#!/usr/bin/env bash
# Creates or updates every Kubernetes Secret of the platform from the repo-root .env.
# Secret values never go to Git: Argo CD is not allowed to manage Secrets (k8s/argocd/project.yaml).
#
# Usage:
#   ./scripts/create-secrets.sh                    # all secrets
#   ./scripts/create-secrets.sh --only monitoring  # grafana-admin + alertmanager-slack
#   ./scripts/create-secrets.sh --rotate KEY       # replace a generated value (see below)
#   ENV_FILE=/secure/prod.env ./scripts/create-secrets.sh
#
# Value resolution for every key:  .env  ->  value already stored in the cluster  ->  new random
# value (generated once, then kept). So re-running is safe: it never changes a password that
# MySQL or Redis have already been initialised with, unless the value in .env changes.
#
# .env keys (see .env.example):
#   required : MYSQL_ROOT_PASSWORD, JWT_SECRET (>= 32 characters, identical for user-service and gateway)
#   optional : MYSQL_DATABASE, MYSQL_APP_USERNAME, MYSQL_APP_PASSWORD, MYSQL_EXPORTER_PASSWORD,
#              REDIS_PASSWORD, ALPHA_VANTAGE_API_KEY, FINNHUB_API_KEY, GRAFANA_ADMIN_USER,
#              GRAFANA_ADMIN_PASSWORD, ALERT_SLACK_WEBHOOK_URL, GHCR_USERNAME, GHCR_TOKEN

source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

ONLY=""
ROTATE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --only) ONLY="$2"; shift 2 ;;
    --rotate) ROTATE="$2"; shift 2 ;;
    -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
    *) die "Unknown argument: $1" ;;
  esac
done

require_cluster
require_cmd openssl base64
ensure_namespaces
[[ -f "${ENV_FILE:-${REPO_ROOT}/.env}" ]] || warn "No .env at ${ENV_FILE:-${REPO_ROOT}/.env}: only existing or generated values can be used."

WORK_DIR="$(mktemp -d)"
chmod 700 "${WORK_DIR}"
trap 'rm -rf "${WORK_DIR}"' EXIT

existing() { # namespace secret key
  kubectl -n "$1" get secret "$2" -o "jsonpath={.data.$3}" 2>/dev/null | base64 -d 2>/dev/null || true
}

# resolve <ENV_KEY> <namespace> <secret> <secret_key> <generate:true|false> [default]
resolve() {
  local env_key="$1" ns="$2" secret="$3" key="$4" generate="$5" default="${6:-}" value=""
  value="$(env_value "${env_key}" 2>/dev/null || true)"
  if [[ -z "${value}" && "${ROTATE}" != "${env_key}" ]]; then
    value="$(existing "${ns}" "${secret}" "${key}")"
  fi
  if [[ -z "${value}" && -n "${default}" ]]; then
    value="${default}"
  fi
  if [[ -z "${value}" && "${generate}" == true ]]; then
    value="$(openssl rand -base64 33 | tr -d '/+=\n' | cut -c1-32)"
    warn "${env_key} not in .env: generated a random value, stored only in secret ${ns}/${secret}. Save it in your password manager."
  fi
  printf '%s' "${value}"
}

# apply_secret <namespace> <name> KEY=VALUE...   (values written to files: no process-list exposure)
apply_secret() {
  local ns="$1" name="$2"
  shift 2
  local dir="${WORK_DIR}/${ns}-${name}"
  mkdir -p "${dir}"
  local pair key
  for pair in "$@"; do
    key="${pair%%=*}"
    printf '%s' "${pair#*=}" > "${dir}/${key}"
  done
  kubectl -n "${ns}" create secret generic "${name}" --from-file="${dir}/" --dry-run=client -o yaml \
    | kubectl label --local -f - app.kubernetes.io/part-of=stock-market-system -o yaml \
    | kubectl apply -f - >/dev/null
  ok "secret ${ns}/${name} ($(ls "${dir}" | tr '\n' ' '))"
}

# kubectl create secret --from-file with an empty directory creates a Secret without data.
apply_empty_secret() {
  kubectl -n "$1" create secret generic "$2" --dry-run=client -o yaml \
    | kubectl label --local -f - app.kubernetes.io/part-of=stock-market-system -o yaml \
    | kubectl apply -f - >/dev/null
  ok "secret $1/$2 (no keys)"
}

monitoring_secrets() {
  local grafana_user grafana_password slack
  grafana_user="$(resolve GRAFANA_ADMIN_USER "${NS_MONITORING}" grafana-admin admin-user false admin)"
  grafana_password="$(resolve GRAFANA_ADMIN_PASSWORD "${NS_MONITORING}" grafana-admin admin-password true)"
  apply_secret "${NS_MONITORING}" grafana-admin "admin-user=${grafana_user}" "admin-password=${grafana_password}"

  slack="$(resolve ALERT_SLACK_WEBHOOK_URL "${NS_MONITORING}" alertmanager-slack webhook-url false)"
  if [[ -z "${slack}" ]]; then
    warn "ALERT_SLACK_WEBHOOK_URL not set: Alertmanager runs, but Slack notifications fail until it is."
    slack="https://hooks.slack.com/services/NOT/CONFIGURED/YET"
  fi
  apply_secret "${NS_MONITORING}" alertmanager-slack "webhook-url=${slack}"
}

data_secrets() {
  local root db app_user app_password exporter redis
  root="$(resolve MYSQL_ROOT_PASSWORD "${NS_DATABASE}" mysql-secret MYSQL_ROOT_PASSWORD false)"
  [[ -n "${root}" ]] || die "MYSQL_ROOT_PASSWORD is required in .env"
  db="$(resolve MYSQL_DATABASE "${NS_DATABASE}" mysql-secret MYSQL_DATABASE false stock_market_system_db)"
  app_user="$(resolve MYSQL_APP_USERNAME "${NS_DATABASE}" mysql-secret MYSQL_USER false stockmarket)"
  app_password="$(resolve MYSQL_APP_PASSWORD "${NS_DATABASE}" mysql-secret MYSQL_PASSWORD true)"
  exporter="$(resolve MYSQL_EXPORTER_PASSWORD "${NS_DATABASE}" mysql-secret MYSQL_EXPORTER_PASSWORD true)"
  [[ "${app_user}" != "root" ]] || die "MYSQL_APP_USERNAME must not be root: the services use a dedicated account"

  apply_secret "${NS_DATABASE}" mysql-secret \
    "MYSQL_ROOT_PASSWORD=${root}" "MYSQL_DATABASE=${db}" \
    "MYSQL_USER=${app_user}" "MYSQL_PASSWORD=${app_password}" \
    "MYSQL_EXPORTER_PASSWORD=${exporter}"

  redis="$(resolve REDIS_PASSWORD "${NS_DATABASE}" redis-secret REDIS_PASSWORD true)"
  apply_secret "${NS_DATABASE}" redis-secret "REDIS_PASSWORD=${redis}"

  # Shared with the application secrets below.
  APP_DB_USER="${app_user}"
  APP_DB_PASSWORD="${app_password}"
  REDIS_SECRET="${redis}"
}

application_secrets() {
  local jwt alpha finnhub
  jwt="$(resolve JWT_SECRET "${NS_APP}" api-gateway-secret JWT_SECRET false)"
  [[ -n "${jwt}" ]] || die "JWT_SECRET is required in .env"
  (( ${#jwt} >= 32 )) || die "JWT_SECRET must be at least 32 characters (HMAC-SHA256 key)"
  alpha="$(env_value ALPHA_VANTAGE_API_KEY 2>/dev/null || true)"
  finnhub="$(env_value FINNHUB_API_KEY 2>/dev/null || true)"

  local db=("SPRING_DATASOURCE_USERNAME=${APP_DB_USER}" "SPRING_DATASOURCE_PASSWORD=${APP_DB_PASSWORD}")

  apply_secret "${NS_APP}" api-gateway-secret "JWT_SECRET=${jwt}"
  apply_secret "${NS_APP}" user-service-secret "${db[@]}" "JWT_SECRET=${jwt}"
  apply_secret "${NS_APP}" market-data-service-secret "${db[@]}" \
    "SPRING_DATA_REDIS_PASSWORD=${REDIS_SECRET}" \
    "ALPHA_VANTAGE_API_KEY=${alpha}" "FINNHUB_API_KEY=${finnhub}"
  apply_secret "${NS_APP}" order-service-secret "${db[@]}"
  apply_secret "${NS_APP}" portfolio-service-secret "${db[@]}"
  apply_secret "${NS_APP}" account-service-secret "${db[@]}"
  apply_empty_secret "${NS_APP}" notification-service-secret
  apply_empty_secret "${NS_APP}" ai-service-secret
}

registry_secret() {
  local user token
  user="$(env_value GHCR_USERNAME 2>/dev/null || true)"
  token="$(env_value GHCR_TOKEN 2>/dev/null || true)"
  if [[ -z "${user}" || -z "${token}" ]]; then
    if kubectl -n "${NS_APP}" get secret ghcr-pull-secret >/dev/null 2>&1; then
      ok "secret ${NS_APP}/ghcr-pull-secret kept (GHCR_USERNAME/GHCR_TOKEN not in .env)"
      return
    fi
    warn "GHCR_USERNAME / GHCR_TOKEN not set: creating an empty pull secret (works only for public images)."
    kubectl -n "${NS_APP}" create secret docker-registry ghcr-pull-secret \
      --docker-server=ghcr.io --docker-username=anonymous --docker-password=none \
      --dry-run=client -o yaml | kubectl apply -f - >/dev/null
    return
  fi
  kubectl -n "${NS_APP}" create secret docker-registry ghcr-pull-secret \
    --docker-server=ghcr.io --docker-username="${user}" --docker-password="${token}" \
    --dry-run=client -o yaml \
    | kubectl label --local -f - app.kubernetes.io/part-of=stock-market-system -o yaml \
    | kubectl apply -f - >/dev/null
  ok "secret ${NS_APP}/ghcr-pull-secret (ghcr.io as ${user})"
}

case "${ONLY}" in
  "")
    monitoring_secrets
    data_secrets
    application_secrets
    registry_secret
    ;;
  monitoring) monitoring_secrets ;;
  data) data_secrets ;;
  apps) data_secrets; application_secrets; registry_secret ;;
  *) die "--only must be monitoring, data or apps" ;;
esac

cat <<EOF

Secrets applied. Pods read Secrets at start-up: after changing a value run
  kubectl -n ${NS_APP} rollout restart deployment
Changing MYSQL_*_PASSWORD after MySQL was initialised also needs ALTER USER inside MySQL
(docs/operations-runbook.md, "Rotate credentials").
EOF
