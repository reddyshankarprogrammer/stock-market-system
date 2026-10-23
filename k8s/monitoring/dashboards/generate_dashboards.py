"""Generates the Grafana dashboards in this folder (one JSON file per dashboard).

Usage (from the repo root):
    python k8s/monitoring/dashboards/generate_dashboards.py

The JSON files are committed: Argo CD turns them into ConfigMaps (kustomization.yaml in the
parent folder) that the Grafana sidecar loads into the "Stock Market" folder. Edit this script,
not the JSON, then regenerate.
"""
import json
import os

OUT = os.path.dirname(os.path.abspath(__file__))
PROM = {"type": "prometheus", "uid": "prometheus"}
LOKI = {"type": "loki", "uid": "loki"}


class Board:
    def __init__(self, uid, title, tags, variables=None, description=""):
        self.uid, self.title, self.tags, self.description = uid, title, tags, description
        self.variables = variables or []
        self.panels, self.y, self.next_id = [], 0, 1

    def row(self, title):
        self.panels.append({"type": "row", "title": title, "id": self._id(), "collapsed": False,
                            "gridPos": {"h": 1, "w": 24, "x": 0, "y": self.y}, "panels": []})
        self.y += 1

    def _id(self):
        self.next_id += 1
        return self.next_id

    def add(self, panels, height):
        """Places panels side by side on one line; widths must add up to 24."""
        x = 0
        for p in panels:
            p["id"] = self._id()
            p["gridPos"] = {"h": height, "w": p.pop("w"), "x": x, "y": self.y}
            x += p["gridPos"]["w"]
        self.panels.extend(panels)
        self.y += height

    def json(self):
        return {
            "uid": self.uid, "title": self.title, "tags": self.tags, "description": self.description,
            "editable": True, "graphTooltip": 1, "schemaVersion": 39, "version": 1,
            "time": {"from": "now-6h", "to": "now"}, "refresh": "30s", "timezone": "browser",
            "templating": {"list": self.variables}, "annotations": {"list": []}, "links": [
                {"title": "Stock Market", "type": "dashboards", "tags": ["stock-market"], "asDropdown": True}],
            "panels": self.panels,
        }

    def save(self):
        with open(os.path.join(OUT, f"{self.uid}.json"), "w", newline="\n") as f:
            json.dump(self.json(), f, indent=2)
            f.write("\n")


def target(expr, legend="", ds=PROM, instant=False):
    t = {"datasource": ds, "expr": expr, "legendFormat": legend, "refId": "A"}
    if instant:
        t["instant"] = True
    return t


def stat(title, expr, w=4, unit="short", thresholds=None, legend="", decimals=None, mappings=None):
    steps = thresholds or [{"color": "green", "value": None}]
    p = {"type": "stat", "title": title, "w": w, "datasource": PROM, "targets": [target(expr, legend)],
         "options": {"reduceOptions": {"calcs": ["lastNotNull"]}, "colorMode": "background",
                     "graphMode": "area", "textMode": "auto"},
         "fieldConfig": {"defaults": {"unit": unit, "thresholds": {"mode": "absolute", "steps": steps},
                                      "mappings": mappings or []}, "overrides": []}}
    if decimals is not None:
        p["fieldConfig"]["defaults"]["decimals"] = decimals
    return p


def ts(title, exprs, w=12, unit="short", stack=False, desc=""):
    targets = []
    for i, (expr, legend) in enumerate(exprs):
        t = target(expr, legend)
        t["refId"] = chr(65 + i)
        targets.append(t)
    return {"type": "timeseries", "title": title, "w": w, "datasource": PROM, "targets": targets,
            "description": desc,
            "fieldConfig": {"defaults": {"unit": unit, "custom": {
                "lineWidth": 1, "fillOpacity": 15 if stack else 5, "showPoints": "never",
                "stacking": {"mode": "normal" if stack else "none"}}}, "overrides": []},
            "options": {"legend": {"displayMode": "table", "placement": "bottom", "calcs": ["mean", "max", "lastNotNull"]},
                        "tooltip": {"mode": "multi", "sort": "desc"}}}


def table(title, expr, w=12, unit="short"):
    return {"type": "table", "title": title, "w": w, "datasource": PROM,
            "targets": [dict(target(expr, instant=True), format="table")],
            "fieldConfig": {"defaults": {"unit": unit}, "overrides": []},
            "transformations": [{"id": "organize", "options": {"excludeByName": {"Time": True}}}],
            "options": {"showHeader": True, "sortBy": [{"displayName": "Value", "desc": True}]}}


def logs(title, expr, w=24):
    return {"type": "logs", "title": title, "w": w, "datasource": LOKI, "targets": [target(expr, ds=LOKI)],
            "options": {"showTime": True, "wrapLogMessage": True, "sortOrder": "Descending",
                        "enableLogDetails": True, "prettifyLogMessage": False}}


RED_AMBER = [{"color": "green", "value": None}, {"color": "orange", "value": 0.7}, {"color": "red", "value": 0.9}]
UP = [{"color": "red", "value": None}, {"color": "green", "value": 1}]


def spring_service_board(service, title, extra=None):
    sel = f'namespace="stock-market", service="{service}"'
    pod_var = {"name": "pod", "type": "query", "datasource": PROM, "label": "Pod", "multi": True,
               "includeAll": True, "refresh": 2, "query": {"query": f'label_values(up{{{sel}}}, pod)', "refId": "pod"},
               "definition": f'label_values(up{{{sel}}}, pod)'}
    s = f'{sel}, pod=~"$pod"'
    b = Board(f"stock-market-{service}", f"Stock Market / {title}", ["stock-market", "spring-boot", service],
              [pod_var], f"RED metrics, JVM, database pool and Kafka for {service}.")
    b.row("Overview")
    b.add([
        stat("Pods up", f'sum(up{{{sel}}})', thresholds=UP),
        stat("Requests / s", f'sum(rate(http_server_requests_seconds_count{{{s}, uri!~"/actuator.*"}}[5m]))', unit="reqps", decimals=2),
        stat("Error rate (5xx)", f'sum(rate(http_server_requests_seconds_count{{{s}, status=~"5..", uri!~"/actuator.*"}}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count{{{s}, uri!~"/actuator.*"}}[5m])), 1e-9)',
             unit="percentunit", thresholds=[{"color": "green", "value": None}, {"color": "orange", "value": 0.01}, {"color": "red", "value": 0.05}], decimals=2),
        stat("p95 latency", f'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{{{s}, uri!~"/actuator.*"}}[5m])))',
             unit="s", thresholds=[{"color": "green", "value": None}, {"color": "orange", "value": 0.5}, {"color": "red", "value": 1}], decimals=3),
        stat("Heap used", f'sum(jvm_memory_used_bytes{{{s}, area="heap"}}) / sum(jvm_memory_max_bytes{{{s}, area="heap"}})', unit="percentunit", thresholds=RED_AMBER, decimals=1),
        stat("Uptime", f'min(process_uptime_seconds{{{s}}})', unit="s"),
    ], 4)
    b.row("Traffic (RED)")
    b.add([
        ts("Requests by status", [(f'sum by (status) (rate(http_server_requests_seconds_count{{{s}, uri!~"/actuator.*"}}[5m]))', "{{status}}")], unit="reqps", stack=True),
        ts("Latency percentiles", [
            (f'histogram_quantile(0.50, sum by (le) (rate(http_server_requests_seconds_bucket{{{s}, uri!~"/actuator.*"}}[5m])))', "p50"),
            (f'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{{{s}, uri!~"/actuator.*"}}[5m])))', "p95"),
            (f'histogram_quantile(0.99, sum by (le) (rate(http_server_requests_seconds_bucket{{{s}, uri!~"/actuator.*"}}[5m])))', "p99")], unit="s"),
    ], 8)
    b.add([
        table("Top endpoints by request rate", f'topk(15, sum by (method, uri) (rate(http_server_requests_seconds_count{{{s}, uri!~"/actuator.*"}}[15m])))', unit="reqps"),
        table("Slowest endpoints (p95, 15m)", f'topk(15, histogram_quantile(0.95, sum by (le, method, uri) (rate(http_server_requests_seconds_bucket{{{s}, uri!~"/actuator.*"}}[15m]))))', unit="s"),
    ], 8)
    b.add([
        ts("Errors by endpoint (4xx/5xx)", [(f'sum by (status, uri) (rate(http_server_requests_seconds_count{{{s}, status=~"[45]..", uri!~"/actuator.*"}}[5m]))', "{{status}} {{uri}}")], unit="reqps"),
        ts("Log events by level", [(f'sum by (level) (rate(logback_events_total{{{s}}}[5m]))', "{{level}}")], unit="short"),
    ], 8)
    b.row("JVM")
    b.add([
        ts("Heap / non-heap", [(f'sum by (pod, area) (jvm_memory_used_bytes{{{s}}})', "{{pod}} {{area}}"),
                               (f'sum by (pod) (jvm_memory_max_bytes{{{s}, area="heap"}})', "{{pod}} heap max")], w=8, unit="bytes"),
        ts("GC pause time", [(f'sum by (pod) (rate(jvm_gc_pause_seconds_sum{{{s}}}[5m]))', "{{pod}}")], w=8, unit="s"),
        ts("CPU", [(f'sum by (pod) (process_cpu_usage{{{s}}})', "{{pod}}")], w=8, unit="percentunit"),
    ], 8)
    b.add([
        ts("Live threads", [(f'sum by (pod) (jvm_threads_live_threads{{{s}}})', "{{pod}}")], w=8),
        ts("Container memory (working set)", [(f'sum by (pod) (container_memory_working_set_bytes{{namespace="stock-market", pod=~"{service}-.*", container="{service}"}})', "{{pod}}")], w=8, unit="bytes"),
        ts("Container restarts (1h)", [(f'sum by (pod) (increase(kube_pod_container_status_restarts_total{{namespace="stock-market", container="{service}"}}[1h]))', "{{pod}}")], w=8),
    ], 8)
    if extra:
        extra(b, s)
    b.row("Logs")
    b.add([logs("Errors and warnings", f'{{namespace="stock-market", app="{service}"}} | json | level=~"ERROR|WARN"')], 10)
    return b


def db_and_kafka(b, s):
    b.row("Database and Kafka")
    b.add([
        ts("HikariCP connections", [(f'sum by (pod) (hikaricp_connections_active{{{s}}})', "{{pod}} active"),
                                    (f'sum by (pod) (hikaricp_connections_pending{{{s}}})', "{{pod}} pending"),
                                    (f'max(hikaricp_connections_max{{{s}}})', "max")], w=8),
        ts("Connection acquire time (avg)", [(f'sum by (pod) (rate(hikaricp_connections_acquire_seconds_sum{{{s}}}[5m])) / clamp_min(sum by (pod) (rate(hikaricp_connections_acquire_seconds_count{{{s}}}[5m])), 1e-9)', "{{pod}}")], w=8, unit="s"),
        ts("Kafka messages produced / consumed", [
            (f'sum by (name) (rate(spring_kafka_template_seconds_count{{{s}}}[5m]))', "sent {{name}}"),
            (f'sum by (name) (rate(spring_kafka_listener_seconds_count{{{s}}}[5m]))', "consumed {{name}}")], w=8, unit="ops"),
    ], 8)


def gateway_extra(b, s):
    b.row("Routing")
    b.add([
        ts("Requests by downstream route (uri)", [(f'sum by (uri) (rate(http_server_requests_seconds_count{{{s}, uri=~"/api/.*"}}[5m]))', "{{uri}}")], unit="reqps", stack=True),
        ts("Rejected requests (401/403)", [(f'sum by (status) (rate(http_server_requests_seconds_count{{{s}, status=~"401|403"}}[5m]))', "{{status}}")], unit="reqps"),
    ], 8)


def services_overview():
    svc_var = {"name": "service", "type": "query", "datasource": PROM, "label": "Service", "multi": True, "includeAll": True,
               "refresh": 2, "query": {"query": 'label_values(up{namespace="stock-market"}, service)', "refId": "svc"},
               "definition": 'label_values(up{namespace="stock-market"}, service)'}
    s = 'namespace="stock-market", service=~"$service", uri!~"/actuator.*"'
    b = Board("stock-market-services-overview", "Stock Market / Services Overview", ["stock-market", "spring-boot"],
              [svc_var], "All services side by side.")
    b.add([
        table("Service health (pods up)", 'sum by (service) (up{namespace="stock-market", service=~"$service"})', w=8),
        ts("Requests / s by service", [(f'sum by (service) (rate(http_server_requests_seconds_count{{{s}}}[5m]))', "{{service}}")], w=16, unit="reqps"),
    ], 8)
    b.add([
        ts("5xx ratio by service", [(f'sum by (service) (rate(http_server_requests_seconds_count{{{s}, status=~"5.."}}[5m])) / clamp_min(sum by (service) (rate(http_server_requests_seconds_count{{{s}}}[5m])), 1e-9)', "{{service}}")], unit="percentunit"),
        ts("p95 latency by service", [(f'histogram_quantile(0.95, sum by (le, service) (rate(http_server_requests_seconds_bucket{{{s}}}[5m])))', "{{service}}")], unit="s"),
    ], 8)
    b.add([
        ts("Heap used by service", [('sum by (service) (jvm_memory_used_bytes{namespace="stock-market", service=~"$service", area="heap"})', "{{service}}")], unit="bytes"),
        ts("Replicas (HPA current / max)", [
            ('kube_horizontalpodautoscaler_status_current_replicas{namespace="stock-market"}', "{{horizontalpodautoscaler}} current"),
            ('kube_horizontalpodautoscaler_spec_max_replicas{namespace="stock-market"}', "{{horizontalpodautoscaler}} max")]),
    ], 8)
    return b


def web_app_board():
    # The Next.js server exposes no Prometheus metrics, so this board uses the container and Kubernetes metrics
    # of the pods plus their logs.
    c = 'namespace="stock-market", pod=~"web-app-.*", container="web-app"'
    b = Board("stock-market-web-app", "Stock Market / Web App", ["stock-market", "web-app"], [],
              "Quantiva web app (Next.js): pod health, resources and logs. Sign-in, registration and onboarding errors show in the gateway and user-service boards.")
    b.add([
        stat("Pods ready", 'kube_deployment_status_replicas_available{namespace="stock-market", deployment="web-app"}', thresholds=UP),
        stat("Restarts (1h)", f'sum(increase(kube_pod_container_status_restarts_total{{{c}}}[1h]))',
             thresholds=[{"color": "green", "value": None}, {"color": "orange", "value": 1}, {"color": "red", "value": 3}]),
        stat("Memory vs limit", f'max(container_memory_working_set_bytes{{{c}}} / on(pod) group_left() max by (pod) (kube_pod_container_resource_limits{{namespace="stock-market", pod=~"web-app-.*", resource="memory"}}))',
             w=8, unit="percentunit", thresholds=RED_AMBER),
        stat("CPU throttled", f'sum(rate(container_cpu_cfs_throttled_periods_total{{{c}}}[5m])) / clamp_min(sum(rate(container_cpu_cfs_periods_total{{{c}}}[5m])), 1e-9)',
             w=8, unit="percentunit", thresholds=RED_AMBER),
    ], 4)
    b.add([
        ts("CPU by pod", [(f'sum by (pod) (rate(container_cpu_usage_seconds_total{{{c}}}[5m]))', "{{pod}}")], unit="cores"),
        ts("Memory by pod", [(f'sum by (pod) (container_memory_working_set_bytes{{{c}}})', "{{pod}}")], unit="bytes"),
    ], 8)
    b.add([
        ts("Replicas (HPA current / max)", [
            ('kube_horizontalpodautoscaler_status_current_replicas{namespace="stock-market", horizontalpodautoscaler="web-app"}', "current"),
            ('kube_horizontalpodautoscaler_spec_max_replicas{namespace="stock-market", horizontalpodautoscaler="web-app"}', "max")]),
        ts("Network by pod", [(f'sum by (pod) (rate(container_network_receive_bytes_total{{namespace="stock-market", pod=~"web-app-.*"}}[5m]))', "{{pod}} in"),
                              (f'sum by (pod) (rate(container_network_transmit_bytes_total{{namespace="stock-market", pod=~"web-app-.*"}}[5m]))', "{{pod}} out")], unit="Bps"),
    ], 8)
    b.add([logs("Web app logs", '{namespace="stock-market", app="web-app"}')], 10)
    return b


def kafka_board():
    k = 'namespace="messaging"'
    b = Board("stock-market-kafka", "Stock Market / Kafka", ["stock-market", "kafka"], [],
              "Strimzi Kafka cluster stock-market-kafka: brokers, throughput, replication, consumer lag.")
    b.add([
        stat("Brokers online", f'count(kafka_server_replicamanager_leadercount{{{k}}})', thresholds=UP),
        stat("Active controllers", f'sum(kafka_controller_kafkacontroller_activecontrollercount{{{k}}})',
             thresholds=[{"color": "red", "value": None}, {"color": "green", "value": 1}, {"color": "red", "value": 2}]),
        stat("Offline partitions", f'sum(kafka_controller_kafkacontroller_offlinepartitionscount{{{k}}})',
             thresholds=[{"color": "green", "value": None}, {"color": "red", "value": 1}]),
        stat("Under-replicated partitions", f'sum(kafka_server_replicamanager_underreplicatedpartitions{{{k}}})',
             thresholds=[{"color": "green", "value": None}, {"color": "red", "value": 1}]),
        stat("Topics", f'count(count by (topic) (kafka_topic_partitions{{{k}, topic!~"__.*"}}))'),
        stat("Total consumer lag", f'sum(kafka_consumergroup_lag{{{k}}})',
             thresholds=[{"color": "green", "value": None}, {"color": "orange", "value": 100}, {"color": "red", "value": 1000}]),
    ], 4)
    b.add([
        ts("Messages in / s by topic", [(f'sum by (topic) (rate(kafka_server_brokertopicmetrics_messagesin_total{{{k}, topic!=""}}[5m]))', "{{topic}}")], unit="ops", stack=True),
        ts("Consumer lag by group and topic", [(f'sum by (consumergroup, topic) (kafka_consumergroup_lag{{{k}}})', "{{consumergroup}} / {{topic}}")]),
    ], 9)
    b.add([
        ts("Bytes in / out", [(f'sum(rate(kafka_server_brokertopicmetrics_bytesin_total{{{k}, topic=""}}[5m]))', "in"),
                              (f'sum(rate(kafka_server_brokertopicmetrics_bytesout_total{{{k}, topic=""}}[5m]))', "out")], w=8, unit="Bps"),
        ts("Request handler idle", [(f'avg(kafka_server_kafkarequesthandlerpool_requesthandleravgidle_percent{{{k}}})', "idle")], w=8, unit="percentunit"),
        ts("Partition offsets (production) by topic", [(f'sum by (topic) (rate(kafka_topic_partition_current_offset{{{k}, topic!~"__.*"}}[5m]))', "{{topic}}")], w=8, unit="ops"),
    ], 8)
    b.add([
        ts("Broker JVM heap", [(f'sum by (pod) (jvm_memory_used_bytes{{{k}, area="heap"}} or jvm_memory_bytes_used{{{k}, area="heap"}})', "{{pod}}")], w=8, unit="bytes"),
        ts("Log size by topic", [(f'sum by (topic) (kafka_log_log_size{{{k}, topic!~"__.*"}})', "{{topic}}")], w=8, unit="bytes"),
        ts("Broker volume used", [(f'kubelet_volume_stats_used_bytes{{{k}}} / kubelet_volume_stats_capacity_bytes{{{k}}}', "{{persistentvolumeclaim}}")], w=8, unit="percentunit"),
    ], 8)
    return b


def mysql_board():
    m = 'namespace="database"'
    b = Board("stock-market-mysql", "Stock Market / MySQL", ["stock-market", "mysql"], [],
              "MySQL 8.4 (mysqld-exporter sidecar): availability, connections, throughput, InnoDB, backups.")
    b.add([
        stat("MySQL up", f'max(mysql_up{{{m}}})', thresholds=UP),
        stat("Uptime", f'max(mysql_global_status_uptime{{{m}}})', unit="s"),
        stat("Connections used", f'max(mysql_global_status_threads_connected{{{m}}}) / max(mysql_global_variables_max_connections{{{m}}})', unit="percentunit", thresholds=RED_AMBER, decimals=1),
        stat("Queries / s", f'sum(rate(mysql_global_status_queries{{{m}}}[5m]))', unit="qps", decimals=1),
        stat("Slow queries (1h)", f'sum(increase(mysql_global_status_slow_queries{{{m}}}[1h]))',
             thresholds=[{"color": "green", "value": None}, {"color": "orange", "value": 10}, {"color": "red", "value": 100}]),
        stat("Last backup age", 'time() - max(kube_cronjob_status_last_successful_time{namespace="database", cronjob="mysql-backup"})', unit="s",
             thresholds=[{"color": "green", "value": None}, {"color": "orange", "value": 90000}, {"color": "red", "value": 93600}]),
    ], 4)
    b.add([
        ts("Connections", [(f'max(mysql_global_status_threads_connected{{{m}}})', "connected"),
                           (f'max(mysql_global_status_threads_running{{{m}}})', "running"),
                           (f'max(mysql_global_variables_max_connections{{{m}}})', "max")]),
        ts("Commands / s (top 8)", [(f'topk(8, rate(mysql_global_status_commands_total{{{m}}}[5m]))', "{{command}}")], unit="ops"),
    ], 8)
    b.add([
        ts("InnoDB buffer pool", [(f'max(mysql_global_status_innodb_buffer_pool_bytes_data{{{m}}})', "data"),
                                  (f'max(mysql_global_variables_innodb_buffer_pool_size{{{m}}})', "size")], w=8, unit="bytes"),
        ts("InnoDB row operations / s", [(f'sum by (operation) (rate(mysql_global_status_innodb_row_ops_total{{{m}}}[5m]))', "{{operation}}")], w=8, unit="ops"),
        ts("Row lock waits / s", [(f'rate(mysql_global_status_innodb_row_lock_waits{{{m}}}[5m])', "lock waits")], w=8, unit="ops"),
    ], 8)
    b.add([
        ts("Network traffic", [(f'rate(mysql_global_status_bytes_received{{{m}}}[5m])', "received"),
                               (f'rate(mysql_global_status_bytes_sent{{{m}}}[5m])', "sent")], w=8, unit="Bps"),
        ts("Aborted connections", [(f'rate(mysql_global_status_aborted_connects{{{m}}}[5m])', "aborted connects"),
                                   (f'rate(mysql_global_status_aborted_clients{{{m}}}[5m])', "aborted clients")], w=8, unit="ops"),
        ts("Data and backup volumes used", [(f'kubelet_volume_stats_used_bytes{{{m}}} / kubelet_volume_stats_capacity_bytes{{{m}}}', "{{persistentvolumeclaim}}")], w=8, unit="percentunit"),
    ], 8)
    return b


def cluster_board():
    b = Board("stock-market-cluster", "Stock Market / Kubernetes Cluster", ["stock-market", "kubernetes"], [],
              "K3s nodes, namespaces, workloads and storage.")
    b.add([
        stat("Nodes ready", 'sum(kube_node_status_condition{condition="Ready", status="true"})', thresholds=UP),
        stat("Cluster CPU", '1 - avg(rate(node_cpu_seconds_total{mode="idle"}[5m]))', unit="percentunit", thresholds=RED_AMBER, decimals=1),
        stat("Cluster memory", '1 - sum(node_memory_MemAvailable_bytes) / sum(node_memory_MemTotal_bytes)', unit="percentunit", thresholds=RED_AMBER, decimals=1),
        stat("Pods running", 'sum(kube_pod_status_phase{phase="Running"})'),
        stat("Pods not ready", 'sum(kube_pod_status_ready{condition="false"})', thresholds=[{"color": "green", "value": None}, {"color": "red", "value": 1}]),
        stat("Firing alerts", 'count(ALERTS{alertstate="firing", severity=~"warning|critical"}) or vector(0)',
             thresholds=[{"color": "green", "value": None}, {"color": "orange", "value": 1}, {"color": "red", "value": 3}]),
    ], 4)
    b.add([
        ts("CPU by node", [('1 - avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m]))', "{{instance}}")], w=8, unit="percentunit"),
        ts("Memory by node", [('1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes', "{{instance}}")], w=8, unit="percentunit"),
        ts("Root disk by node", [('1 - node_filesystem_avail_bytes{mountpoint="/", fstype!="tmpfs"} / node_filesystem_size_bytes{mountpoint="/", fstype!="tmpfs"}', "{{instance}}")], w=8, unit="percentunit"),
    ], 8)
    b.add([
        ts("CPU by namespace", [('sum by (namespace) (rate(container_cpu_usage_seconds_total{container!="", container!="POD"}[5m]))', "{{namespace}}")], unit="cores", stack=True),
        ts("Memory by namespace", [('sum by (namespace) (container_memory_working_set_bytes{container!="", container!="POD"})', "{{namespace}}")], unit="bytes", stack=True),
    ], 8)
    b.add([
        table("Restarts in the last hour", 'sum by (namespace, pod) (increase(kube_pod_container_status_restarts_total[1h])) > 0', w=8),
        table("Persistent volume usage", 'kubelet_volume_stats_used_bytes / kubelet_volume_stats_capacity_bytes', w=8, unit="percentunit"),
        table("Deployments not at desired replicas", 'kube_deployment_spec_replicas - kube_deployment_status_replicas_available != 0', w=8),
    ], 8)
    b.add([
        ts("Network receive by namespace", [('sum by (namespace) (rate(container_network_receive_bytes_total[5m]))', "{{namespace}}")], unit="Bps"),
        ts("Pending pods", [('sum by (namespace) (kube_pod_status_phase{phase="Pending"})', "{{namespace}}")]),
    ], 8)
    return b


def logs_board():
    # Text box variables: an empty value matches every line.
    def textbox(name, label):
        return {"name": name, "type": "textbox", "label": label, "query": "", "current": {"text": "", "value": ""}}

    svc = {"name": "app", "type": "query", "datasource": LOKI, "label": "Service", "multi": True, "includeAll": True,
           "refresh": 2, "query": {"label": "app", "stream": '{namespace="stock-market"}', "type": 1, "refId": "app"},
           "definition": "label_values(app)", "allValue": ".+"}
    b = Board("stock-market-logs", "Stock Market / Log Search", ["stock-market", "logs"],
              [svc, textbox("userId", "userId"), textbox("accountId", "accountId"), textbox("tradeId", "tradeId"),
               textbox("statementId", "statementId"), textbox("search", "Free text")],
              "Search the service logs by business id (structured metadata extracted by Alloy).")
    q = ('{namespace="stock-market", app=~"$app"} '
         '| userId=~"${userId:regex}.*" | accountId=~"${accountId:regex}.*" '
         '| tradeId=~"${tradeId:regex}.*" | statementId=~"${statementId:regex}.*" |= "$search"')
    b.add([{"type": "timeseries", "title": "Matching lines by level", "w": 24, "datasource": LOKI,
            "targets": [target(f'sum by (level) (count_over_time({q} [$__interval]))', "{{level}}", ds=LOKI)],
            "fieldConfig": {"defaults": {"custom": {"drawStyle": "bars", "fillOpacity": 60,
                                                    "stacking": {"mode": "normal"}}}, "overrides": []},
            "options": {"legend": {"displayMode": "list", "placement": "bottom"}}}], 6)
    b.add([logs("Matching log lines", q)], 18)
    return b


if __name__ == "__main__":
    boards = [
        spring_service_board("api-gateway", "API Gateway", gateway_extra),
        spring_service_board("user-service", "User Service", db_and_kafka),
        spring_service_board("account-service", "Account Service", db_and_kafka),
        spring_service_board("portfolio-service", "Portfolio Service", db_and_kafka),
        spring_service_board("order-service", "Order Service", db_and_kafka),
        spring_service_board("market-data-service", "Market Data Service", db_and_kafka),
        spring_service_board("notification-service", "Notification Service", db_and_kafka),
        web_app_board(),
        services_overview(),
        kafka_board(),
        mysql_board(),
        cluster_board(),
        logs_board(),
    ]
    for board in boards:
        board.save()
    print(f"Wrote {len(boards)} dashboards to {OUT}")
