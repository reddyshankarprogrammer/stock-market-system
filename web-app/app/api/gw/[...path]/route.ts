import { NextResponse, type NextRequest } from "next/server";
import { callAsUser, fail, sameOriginOnly, setAccessCookie } from "@/lib/auth/server";

// Generic BFF pass-through for the typed API clients (lib/api/*). The browser never talks to the gateway and never
// holds a token: this handler adds the session's access token (renewing it when needed, see callAsUser) and relays
// the answer. Only the account-service paths below are reachable. user-service paths have their own handlers, and
// internal or admin paths must not be reachable from a browser at all (the gateway also blocks them).
const ALLOWED = /^api\/v1\/(accounts|trades|holdings|statements|fees|taxes|charges|dividends|interest)(\/|$)/;

type Ctx = { params: Promise<{ path: string[] }> };

async function forward(req: NextRequest, { params }: Ctx) {
  const { path } = await params;
  // Decoded segments: refuse anything that could climb out of the allowed prefix.
  if (path.some((s) => s === "" || s === "." || s === ".." || s.includes("/") || s.includes("\\"))) return fail("VALIDATION", 400);
  const target = path.join("/");
  if (!ALLOWED.test(target)) return fail("UNKNOWN", 404, "Not found");

  if (req.method !== "GET") {
    const blocked = sameOriginOnly(req);
    if (blocked) return blocked;
  }
  const hasBody = req.method === "POST" || req.method === "PUT";
  const r = await callAsUser(req, `/${target}${req.nextUrl.search}`, { method: req.method, ...(hasBody ? { body: await req.text() } : {}) });
  if (r.kind === "response") return r.response;

  // Relay status and JSON only: never upstream headers (no Set-Cookie, no internal hosts).
  const text = r.res.status === 204 ? "" : await r.res.text();
  const out = new NextResponse(text || null, { status: r.res.status, headers: text ? { "Content-Type": "application/json" } : {} });
  out.headers.set("Cache-Control", "no-store");
  if (r.renewed) setAccessCookie(out, r.renewed);
  return out;
}

export const GET = forward;
export const POST = forward;
export const PUT = forward;
export const DELETE = forward;
