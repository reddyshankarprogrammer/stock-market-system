#!/usr/bin/env bash
# Restores the application database from a nightly backup (k8s/mysql/backup-cronjob.yaml).
#
# Usage:
#   ./scripts/restore-database.sh --list                                  # backups on the backup volume
#   ./scripts/restore-database.sh                                         # restore latest.sql.gz
#   ./scripts/restore-database.sh --file stock_market_system_db-20261017T220000Z.sql.gz
#   ./scripts/restore-database.sh --local ./downloaded-backup.sql.gz      # from this machine
#
# Options:
#   --yes             do not ask for confirmation
#   --no-safety-dump  skip the backup of the current state taken before restoring
#   --keep-services   do not stop the application services during the restore
#
# Steps: safety dump of the current database -> scale the services to 0 -> verify checksum ->
# DROP + recreate the database from the dump -> verify -> scale the services back.
# Point-in-time recovery beyond the dump (binlog replay) is described in docs/disaster-recovery.md.

source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

MODE="restore"
BACKUP_FILE="latest.sql.gz"
LOCAL_FILE=""
ASSUME_YES=false
SAFETY_DUMP=true
STOP_SERVICES=true
RESTORE_IMAGE="mysql:8.4"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --list) MODE="list"; shift ;;
    --file) BACKUP_FILE="$2"; shift 2 ;;
    --local) LOCAL_FILE="$2"; shift 2 ;;
    --yes) ASSUME_YES=true; shift ;;
    --no-safety-dump) SAFETY_DUMP=false; shift ;;
    --keep-services) STOP_SERVICES=false; shift ;;
    -h|--help) sed -n '2,19p' "$0"; exit 0 ;;
    *) die "Unknown argument: $1" ;;
  esac
done

require_cluster
require_cmd jq gzip

[[ "${BACKUP_FILE}" =~ ^[A-Za-z0-9._-]+$ ]] || die "Invalid backup file name: ${BACKUP_FILE}"
STAMP="$(date -u +%Y%m%d%H%M%S)"
POD_NAME="mysql-restore-${STAMP}"

# Runs a short-lived pod in the database namespace with the backup volume mounted at /backups.
run_backup_pod() {
  local script="$1"
  kubectl -n "${NS_DATABASE}" apply -f - >/dev/null <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: ${POD_NAME}
  labels:
    app.kubernetes.io/name: mysql-restore
    app.kubernetes.io/part-of: stock-market-system
spec:
  restartPolicy: Never
  automountServiceAccountToken: false
  enableServiceLinks: false
  securityContext:
    runAsNonRoot: true
    runAsUser: 999
    runAsGroup: 999
    fsGroup: 999
    seccompProfile:
      type: RuntimeDefault
  containers:
    - name: restore
      image: ${RESTORE_IMAGE}
      command: ["/bin/bash", "-c"]
      args:
        - |
$(sed 's/^/          /' <<<"${script}")
      env:
        - name: MYSQL_PWD
          valueFrom:
            secretKeyRef: { name: mysql-secret, key: MYSQL_ROOT_PASSWORD }
        - name: MYSQL_DATABASE
          valueFrom:
            secretKeyRef: { name: mysql-secret, key: MYSQL_DATABASE }
        - name: BACKUP_FILE
          value: "${BACKUP_FILE}"
      securityContext:
        allowPrivilegeEscalation: false
        readOnlyRootFilesystem: true
        capabilities: { drop: ["ALL"] }
      resources:
        requests: { cpu: 100m, memory: 256Mi }
        limits: { cpu: "1", memory: 1Gi }
      volumeMounts:
        - { name: backups, mountPath: /backups }
        - { name: tmp, mountPath: /tmp }
  volumes:
    - name: backups
      persistentVolumeClaim: { claimName: mysql-backup }
    - name: tmp
      emptyDir: {}
EOF
  kubectl -n "${NS_DATABASE}" wait "pod/${POD_NAME}" --for=condition=Ready --timeout=120s >/dev/null 2>&1 || true
  kubectl -n "${NS_DATABASE}" logs -f "pod/${POD_NAME}" || true
  local phase=""
  for _ in $(seq 1 720); do
    phase="$(kubectl -n "${NS_DATABASE}" get pod "${POD_NAME}" -o jsonpath='{.status.phase}')"
    [[ "${phase}" == "Succeeded" || "${phase}" == "Failed" ]] && break
    sleep 5
  done
  kubectl -n "${NS_DATABASE}" delete pod "${POD_NAME}" --wait=false >/dev/null
  [[ "${phase}" == "Succeeded" ]]
}

mysql_exec() {
  kubectl -n "${NS_DATABASE}" exec -i deploy/mysql -c mysql -- \
    sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --protocol=tcp -h127.0.0.1 "$@"' mysql "$@"
}

if [[ "${MODE}" == "list" ]]; then
  run_backup_pod 'set -e; cd /backups; ls -lh --time-style=+%Y-%m-%dT%H:%M *.sql.gz 2>/dev/null || echo "no backups yet"; echo; df -h /backups | tail -n1'
  exit 0
fi

DB="$(kubectl -n "${NS_DATABASE}" get secret mysql-secret -o jsonpath='{.data.MYSQL_DATABASE}' | base64 -d)"
SOURCE="${LOCAL_FILE:-backup volume: ${BACKUP_FILE}}"
if [[ -n "${LOCAL_FILE}" ]]; then
  [[ -f "${LOCAL_FILE}" ]] || die "Local file not found: ${LOCAL_FILE}"
  gzip -t "${LOCAL_FILE}" || die "${LOCAL_FILE} is not a valid gzip file"
fi

warn "This REPLACES database '${DB}' with ${SOURCE}."
if [[ "${ASSUME_YES}" != true ]]; then
  read -r -p "Type the database name to continue: " answer
  [[ "${answer}" == "${DB}" ]] || die "Aborted."
fi

if [[ "${SAFETY_DUMP}" == true ]]; then
  log "Safety dump of the current state (job pre-restore-${STAMP})"
  kubectl -n "${NS_DATABASE}" create job "pre-restore-${STAMP}" --from=cronjob/mysql-backup >/dev/null
  kubectl -n "${NS_DATABASE}" wait "job/pre-restore-${STAMP}" --for=condition=Complete --timeout=1800s \
    || die "Safety dump failed: kubectl -n ${NS_DATABASE} logs job/pre-restore-${STAMP}"
  ok "Safety dump stored on the backup volume"
  # The safety dump moved latest.sql.gz: resolve the requested file before it was taken.
  if [[ "${BACKUP_FILE}" == "latest.sql.gz" && -z "${LOCAL_FILE}" ]]; then
    BACKUP_FILE="previous"
  fi
fi

declare -A REPLICAS=()
if [[ "${STOP_SERVICES}" == true ]]; then
  log "Scaling the application services to 0"
  while read -r name replicas; do
    REPLICAS["${name}"]="${replicas}"
  done < <(kubectl -n "${NS_APP}" get deploy -o json | jq -r '.items[] | "\(.metadata.name) \(.spec.replicas)"')
  kubectl -n "${NS_APP}" scale deploy --all --replicas=0 >/dev/null
  kubectl -n "${NS_APP}" wait pod -l app.kubernetes.io/part-of=stock-market-system --for=delete --timeout=180s >/dev/null 2>&1 || true
fi

restore_services() {
  [[ "${STOP_SERVICES}" == true ]] || return 0
  log "Scaling the application services back"
  local name
  for name in "${!REPLICAS[@]}"; do
    kubectl -n "${NS_APP}" scale "deploy/${name}" --replicas="${REPLICAS[${name}]}" >/dev/null
  done
}
trap 'restore_services' EXIT

log "Recreating database ${DB}"
mysql_exec -e "DROP DATABASE IF EXISTS \`${DB}\`; CREATE DATABASE \`${DB}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

if [[ -n "${LOCAL_FILE}" ]]; then
  log "Streaming ${LOCAL_FILE} into MySQL"
  gzip -dc "${LOCAL_FILE}" | mysql_exec
else
  log "Restoring ${BACKUP_FILE} from the backup volume"
  run_backup_pod '
set -euo pipefail
cd /backups
if [[ "${BACKUP_FILE}" == "previous" ]]; then
  # Newest dump except the safety dump that was just taken.
  BACKUP_FILE="$(ls -1t "${MYSQL_DATABASE}"-*.sql.gz | sed -n 2p)"
fi
[[ -f "${BACKUP_FILE}" ]] || { echo "Backup ${BACKUP_FILE} not found"; ls -1 /backups; exit 1; }
if [[ -f "${BACKUP_FILE}.sha256" ]]; then sha256sum -c "${BACKUP_FILE}.sha256"; fi
gzip -t "${BACKUP_FILE}"
echo "Importing ${BACKUP_FILE}"
gzip -dc "${BACKUP_FILE}" | mysql -h mysql.database.svc.cluster.local -uroot
echo "Import finished"
' || die "Restore failed; the safety dump pre-restore-${STAMP} holds the previous state."
fi

tables="$(mysql_exec -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB}';")"
(( tables > 0 )) || die "Restore produced an empty database"
ok "Database ${DB} restored: ${tables} tables"

restore_services
trap - EXIT
if [[ "${STOP_SERVICES}" == true ]]; then
  for name in "${!REPLICAS[@]}"; do
    [[ "${REPLICAS[${name}]}" -gt 0 ]] && wait_for_rollout "${NS_APP}" "deploy/${name}" 600s
  done
fi
ok "Restore complete. Run ./scripts/cluster-health-check.sh and the Postman smoke tests."
