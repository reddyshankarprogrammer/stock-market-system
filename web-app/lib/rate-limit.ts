// A small in-memory limiter for public endpoints (the waitlist). Each web pod counts for itself, so with several pods
// the real limit is the number of pods times `limit`: it slows a script down, it is not a security boundary. A shared
// limiter (the ingress, or Redis) is the proper next step and is listed in the root CLAUDE.md known gaps.
const hits = new Map<string, number[]>();
let lastSweep = 0;

/** Returns true when `key` has used up `limit` requests in the last `windowMs`; otherwise records this request. */
export function limited(key: string, limit = 10, windowMs = 60_000, now = Date.now()): boolean {
  if (now - lastSweep > windowMs) {
    lastSweep = now;
    for (const [k, times] of hits) if (times.every((t) => now - t > windowMs)) hits.delete(k);
  }
  const recent = (hits.get(key) ?? []).filter((t) => now - t <= windowMs);
  if (recent.length >= limit) {
    hits.set(key, recent);
    return true;
  }
  recent.push(now);
  hits.set(key, recent);
  return false;
}

/** The caller's address as the ingress reports it (first X-Forwarded-For entry), or a shared bucket when unknown. */
export function clientKey(headers: Headers): string {
  return headers.get("x-forwarded-for")?.split(",")[0]?.trim() || headers.get("x-real-ip") || "unknown";
}
