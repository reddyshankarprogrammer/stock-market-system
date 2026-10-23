// GET /api/health: liveness and readiness probe for Kubernetes. It does not call the gateway: a gateway outage must
// not restart the web pods.
export function GET() {
  return Response.json({ status: "ok" });
}
