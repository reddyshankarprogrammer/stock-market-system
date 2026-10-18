#!/usr/bin/env bash
# Installs Kafka on K3s: the Strimzi Cluster Operator via Helm, then the KRaft Kafka cluster,
# metrics configuration and all topics from k8s/kafka (later reconciled by Argo CD).
#
# Usage:
#   ./scripts/install-kafka.sh                # operator + cluster + topics
#   ./scripts/install-kafka.sh --operator-only

source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

OPERATOR_ONLY=false
[[ "${1:-}" == "--operator-only" ]] && OPERATOR_ONLY=true

require_cluster
require_cmd jq
ensure_namespaces

log "Adding the Strimzi Helm repository"
helm repo add strimzi https://strimzi.io/charts/ --force-update >/dev/null
helm repo update strimzi >/dev/null

helm_install strimzi-kafka-operator strimzi/strimzi-kafka-operator "${NS_MESSAGING}" "${STRIMZI_CHART_VERSION}" \
  --values "${K8S_DIR}/kafka/strimzi-values.yaml"

for crd in kafkas.kafka.strimzi.io kafkanodepools.kafka.strimzi.io kafkatopics.kafka.strimzi.io; do
  kubectl wait --for=condition=Established "crd/${crd}" --timeout=120s >/dev/null
done
ok "Strimzi CRDs established"

[[ "${OPERATOR_ONLY}" == true ]] && exit 0

log "Applying the Kafka cluster and topics (k8s/kafka)"
kubectl apply -k "${K8S_DIR}/kafka"

log "Waiting for the Kafka cluster to become Ready (first start takes a few minutes)"
kubectl -n "${NS_MESSAGING}" wait kafka/stock-market-kafka --for=condition=Ready --timeout=900s

log "Waiting for topics"
kubectl -n "${NS_MESSAGING}" wait kafkatopic --all --for=condition=Ready --timeout=300s

bootstrap="$(kubectl -n "${NS_MESSAGING}" get kafka stock-market-kafka -o json |
  jq -r '.status.listeners[] | select(.name=="plain") | .bootstrapServers')"
ok "Kafka ready. Bootstrap servers: ${bootstrap}"
kubectl -n "${NS_MESSAGING}" get kafkatopics -o custom-columns='TOPIC:.spec.topicName,PARTITIONS:.spec.partitions,STATUS:.metadata.labels.stock-market/topic-status,READY:.status.conditions[0].status'
