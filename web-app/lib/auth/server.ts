// Server-only helpers for the route handlers in app/api/auth/*. Never import this file from client components.
import { NextResponse, type NextRequest } from "next/server";
import { ACCESS_COOKIE, REFRESH_COOKIE } from "@/lib/auth/constants";
import { defaultMessages, type AuthErrorCode } from "@/lib/auth/errors";

// Gateway URL, from the environment (see web-app/.env.example). Same host rule as the rest of the system: never hard-code hosts in code paths that matter.
const GATEWAY = process.env.API_GATEWAY_URL ?? "http://localhost:8080";
const TIMEOUT_MS = 8000; // no request waits longer than 8 seconds
export const UPLOAD_TIMEOUT_MS = 60_000; // a statement is parsed and imported inside the upload request

export type Profile = { userId: string; email: string; firstName: string; lastName: string; role: "USER" | "ADMIN" };

export async function gateway(path: string, init: RequestInit & { timeoutMs?: number } = {}): Promise<Response | null> {
  try {
    return await fetch(`${GATEWAY}${path}`, {
      ...init,
      cache: "no-store",
      signal: AbortSignal.timeout(init.timeoutMs ?? TIMEOUT_MS),
      headers: { "Content-Type": "application/json", Accept: "application/json", ...init.headers },
    });
  } catch {
    return null; // network error or timeout
  }
}

export function fail(code: AuthErrorCode, status: number, message?: string) {
  return NextResponse.json({ code, message: message ?? defaultMessages[code] }, { status });
}

/** Translate a failed user-service response (or a missing one) into our error codes. Login hides whether an email exists. */
export async function mapUpstream(res: Response | null, context: "login" | "register" | "me" | "forgot" | "reset") {
  if (!res || res.status >= 500) return fail("UNAVAILABLE", 503);
  const body = (await res.json().catch(() => ({}))) as { message?: string };
  const text = String(body.message ?? "").toLowerCase();
  if (context === "login" && res.status === 403) return fail("ACCOUNT_NOT_ACTIVE", 403); // only returned after a correct password
  if (context === "login" && (res.status === 401 || res.status === 404)) return fail("INVALID_CREDENTIALS", 401);
  if (context === "register" && res.status === 400 && /already|exist/.test(text)) return fail("EMAIL_TAKEN", 409);
  // The password is validated here before it is sent, so a 400 from the reset endpoint means the token was refused.
  if (context === "reset" && res.status === 400) return fail("INVALID_RESET_TOKEN", 400);
  if (res.status === 400) return fail("VALIDATION", 400, body.message);
  if (res.status === 401 || res.status === 403) return fail("UNAUTHENTICATED", 401);
  return fail("UNKNOWN", 502);
}

/** Read the (unverified) claims of a JWT. The gateway verifies tokens; this is only for the UI (role, expiry). */
export function jwtClaims(token: string): { exp?: number; role?: string } {
  try {
    const part = token.split(".")[1];
    return JSON.parse(Buffer.from(part.replace(/-/g, "+").replace(/_/g, "/"), "base64").toString("utf8"));
  } catch {
    return {};
  }
}

type AuthResponse = { userId: string; email: string; firstName: string; lastName: string; accessToken: string; refreshToken: string };

export function profileFrom(r: AuthResponse): Profile {
  const role = jwtClaims(r.accessToken).role === "ADMIN" ? "ADMIN" : "USER";
  return { userId: r.userId, email: r.email, firstName: r.firstName, lastName: r.lastName, role };
}

const base = { httpOnly: true, sameSite: "lax" as const, secure: process.env.NODE_ENV === "production", path: "/" };

/** The access cookie expires together with the token (15 minutes). */
export function setAccessCookie(res: NextResponse, accessToken: string) {
  const now = Math.floor(Date.now() / 1000);
  const accessExp = jwtClaims(accessToken).exp ?? now + 900;
  res.cookies.set(ACCESS_COOKIE, accessToken, { ...base, maxAge: Math.max(accessExp - now, 1) });
}

export type RefreshResult = { status: "ok"; accessToken: string } | { status: "invalid" } | { status: "unavailable" };

/**
 * Exchange the refresh cookie for a new access token (user-service POST /api/v1/users/refresh).
 * "invalid" means the refresh token is gone, expired or rejected, so the session is over. "unavailable" means the
 * gateway could not be reached, so the session must NOT be dropped: the user may simply retry.
 */
export async function refreshAccessToken(req: NextRequest): Promise<RefreshResult> {
  const refreshToken = req.cookies.get(REFRESH_COOKIE)?.value;
  if (!refreshToken) return { status: "invalid" };
  const res = await gateway("/api/v1/users/refresh", { method: "POST", body: JSON.stringify({ refreshToken }) });
  if (!res || res.status >= 500) return { status: "unavailable" };
  if (!res.ok) return { status: "invalid" };
  const body = (await res.json().catch(() => null)) as { accessToken?: string } | null;
  return body?.accessToken ? { status: "ok", accessToken: body.accessToken } : { status: "invalid" };
}

export type AsUser = { kind: "ok"; res: Response; renewed: string | null } | { kind: "response"; response: NextResponse };

/**
 * Call user-service as the signed-in user. Uses the access cookie; when it is missing or user-service answers 401 it
 * exchanges the refresh cookie once and retries. Returns either a successful upstream response (plus the new access
 * token, if one was issued, so the caller can set the cookie) or a ready-made error response: 401 with both cookies
 * cleared when the session is really over, 503 when the service is unreachable (cookies kept: that is not a logout).
 * A non-2xx answer from user-service (for example a 400) is returned as "ok" with that response, for the caller to map.
 */
export async function callAsUser(req: NextRequest, path: string, init: RequestInit & { timeoutMs?: number } = {}): Promise<AsUser> {
  let token = req.cookies.get(ACCESS_COOKIE)?.value ?? null;
  let renewed: string | null = null;

  const call = (t: string) => gateway(path, { ...init, headers: { ...init.headers, Authorization: `Bearer ${t}` } });
  const renew = async (): Promise<AsUser | null> => {
    const r = await refreshAccessToken(req);
    if (r.status === "unavailable") return { kind: "response", response: fail("UNAVAILABLE", 503) };
    if (r.status === "invalid") {
      const out = fail("UNAUTHENTICATED", 401);
      clearSession(out);
      return { kind: "response", response: out };
    }
    renewed = token = r.accessToken;
    return null;
  };

  if (!token) {
    const stop = await renew();
    if (stop) return stop;
  }
  let res = await call(token!);
  if (res?.status === 401 && !renewed) {
    const stop = await renew();
    if (stop) return stop;
    res = await call(token!);
  }
  if (!res || res.status >= 500) return { kind: "response", response: fail("UNAVAILABLE", 503) };
  if (res.status === 401) {
    const out = fail("UNAUTHENTICATED", 401);
    clearSession(out); // the session is really over: drop both cookies
    return { kind: "response", response: out };
  }
  return { kind: "ok", res, renewed };
}

/** Set the session cookies. The refresh cookie lasts 7 days only if "remember me" is ticked; otherwise it ends with the browser session. */
export function setSession(res: NextResponse, r: AuthResponse, remember: boolean) {
  const now = Math.floor(Date.now() / 1000);
  setAccessCookie(res, r.accessToken);
  const refreshExp = jwtClaims(r.refreshToken).exp;
  res.cookies.set(REFRESH_COOKIE, r.refreshToken, {
    ...base,
    ...(remember ? { maxAge: Math.max((refreshExp ?? now + 7 * 86400) - now, 1) } : {}), // no maxAge = session cookie
  });
}

export function clearSession(res: NextResponse) {
  res.cookies.set(ACCESS_COOKIE, "", { ...base, maxAge: 0 });
  res.cookies.set(REFRESH_COOKIE, "", { ...base, maxAge: 0 });
}

/** CSRF defence for state-changing routes: cookies are SameSite=Lax, and the Origin header must match this site when present. */
export function sameOriginOnly(req: NextRequest, opts: { multipart?: boolean } = {}): NextResponse | null {
  const origin = req.headers.get("origin");
  // Compare hosts, not full origins: behind a TLS-terminating ingress the server sees http while the browser sends https.
  if (origin) {
    const host = req.headers.get("x-forwarded-host") ?? req.headers.get("host") ?? req.nextUrl.host;
    let originHost = "";
    try { originHost = new URL(origin).host; } catch { /* malformed Origin is rejected below */ }
    if (originHost !== host) return fail("FORBIDDEN_ORIGIN", 403);
  }
  const type = req.headers.get("content-type") ?? "";
  const allowed = type.includes("application/json") || (opts.multipart === true && type.startsWith("multipart/form-data"));
  if (!allowed && req.method !== "GET") return fail("VALIDATION", 415, opts.multipart ? "Expected JSON or a file upload" : "Expected JSON");
  return null;
}
