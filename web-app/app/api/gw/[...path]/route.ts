import { NextResponse, type NextRequest } from "next/server";
import { callAsUser, fail, sameOriginOnly, setAccessCookie, UPLOAD_TIMEOUT_MS } from "@/lib/auth/server";

// Generic BFF pass-through for the typed API clients (lib/api/*). The browser never talks to the gateway and never
// holds a token: this handler adds the session's access token (renewing it when needed, see callAsUser) and relays
// the answer. Only the account-service paths below are reachable. user-service paths have their own handlers, and
// internal or admin paths must not be reachable from a browser at all (the gateway also blocks them).
const ALLOWED = /^api\/v1\/(accounts|trades|holdings|statements|fees|taxes|charges|dividends|interest)(\/|$)/;
// The one path that takes a file: a broker statement (CSV, XLS, XLSX; account-service limits it to 20 MB).
const UPLOAD = /^api\/v1\/accounts\/[^/]+\/statements\/upload$/;

type Ctx = { params: Promise<{ path: string[] }> };

async function forward(req: NextRequest, { params }: Ctx) {
  const { path } = await params;
  // Decoded segments: refuse anything that could climb out of the allowed prefix.
  if (path.some((s) => s === "" || s === "." || s === ".." || s.includes("/") || s.includes("\\"))) return fail("VALIDATION", 400);
  const target = path.join("/");
  if (!ALLOWED.test(target)) return fail("UNKNOWN", 404, "Not found");

  const isUpload = req.method === "POST" && UPLOAD.test(target);
  if (req.method !== "GET") {
    const blocked = sameOriginOnly(req, { multipart: isUpload });
    if (blocked) return blocked;
  }
  const hasBody = req.method === "POST" || req.method === "PUT";
  // A file is relayed as raw bytes with its own multipart Content-Type (it carries the boundary), never read as text.
  const init: RequestInit & { timeoutMs?: number } = { method: req.method };
  if (hasBody && isUpload) {
    init.body = await req.arrayBuffer();
    init.headers = { "Content-Type": req.headers.get("content-type") ?? "" };
    init.timeoutMs = UPLOAD_TIMEOUT_MS;
  } else if (hasBody) {
    init.body = await req.text();
  }
  const r = await callAsUser(req, `/${target}${req.nextUrl.search}`, init);
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
