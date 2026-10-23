// Server-only helpers for the route handlers in app/api/auth/*. Never import this file from client components.
import { NextResponse, type NextRequest } from "next/server";
import { ACCESS_COOKIE, REFRESH_COOKIE } from "@/lib/auth/constants";
import { defaultMessages, type AuthErrorCode } from "@/lib/auth/errors";

// Gateway URL, from the environment (see web-app/.env.example). Same host rule as the rest of the system: never hard-code hosts in code paths that matter.
const GATEWAY = process.env.API_GATEWAY_URL ?? "http://localhost:8080";
const TIMEOUT_MS = 8000; // no request waits longer than 8 seconds

export type Profile = { userId: string; email: string; firstName: string; lastName: string; role: "USER" | "ADMIN" };

export async function gateway(path: string, init: RequestInit = {}): Promise<Response | null> {
  try {
    return await fetch(`${GATEWAY}${path}`, {
      ...init,
      cache: "no-store",
      signal: AbortSignal.timeout(TIMEOUT_MS),
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
export async function mapUpstream(res: Response | null, context: "login" | "register" | "me") {
  if (!res || res.status >= 500) return fail("UNAVAILABLE", 503);
  const body = (await res.json().catch(() => ({}))) as { message?: string };
  const text = String(body.message ?? "").toLowerCase();
  if (context === "login" && (res.status === 401 || res.status === 404)) return fail("INVALID_CREDENTIALS", 401);
  if (context === "register" && res.status === 400 && /already|exist/.test(text)) return fail("EMAIL_TAKEN", 409);
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

/** Set the session cookies. The access cookie expires with the token (15 minutes); the refresh cookie lasts 7 days only if "remember me" is ticked. */
export function setSession(res: NextResponse, r: AuthResponse, remember: boolean) {
  const now = Math.floor(Date.now() / 1000);
  const accessExp = jwtClaims(r.accessToken).exp ?? now + 900;
  res.cookies.set(ACCESS_COOKIE, r.accessToken, { ...base, maxAge: Math.max(accessExp - now, 1) });
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
export function sameOriginOnly(req: NextRequest): NextResponse | null {
  const origin = req.headers.get("origin");
  if (origin && origin !== req.nextUrl.origin) return fail("FORBIDDEN_ORIGIN", 403);
  if (!(req.headers.get("content-type") ?? "").includes("application/json") && req.method !== "GET") return fail("VALIDATION", 415, "Expected JSON");
  return null;
}
