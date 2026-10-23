import { NextResponse, type NextRequest } from "next/server";
import { ACCESS_COOKIE } from "@/lib/auth/constants";
import { clearSession, fail, gateway, jwtClaims, mapUpstream, refreshAccessToken, setAccessCookie } from "@/lib/auth/server";

const me = (token: string) => gateway("/api/v1/users/me", { headers: { Authorization: `Bearer ${token}` } });

// GET /api/auth/me -> user-service GET /api/v1/users/me. The session renews itself: when the access cookie has
// expired (or user-service answers 401), the refresh cookie is exchanged once for a new access token.
export async function GET(req: NextRequest) {
  let token = req.cookies.get(ACCESS_COOKIE)?.value ?? null;
  let renewed: string | null = null;

  const renew = async () => {
    const r = await refreshAccessToken(req);
    if (r.status === "ok") renewed = token = r.accessToken;
    return r;
  };

  if (!token) {
    const r = await renew();
    if (r.status === "unavailable") return fail("UNAVAILABLE", 503); // keep the cookies: this is not a logout
    if (r.status === "invalid") {
      const out = fail("UNAUTHENTICATED", 401);
      clearSession(out);
      return out;
    }
  }

  let res = await me(token!);
  if (res?.status === 401 && !renewed) {
    const r = await renew();
    if (r.status === "unavailable") return fail("UNAVAILABLE", 503);
    if (r.status === "ok") res = await me(token!);
  }

  if (!res || !res.ok) {
    const out = await mapUpstream(res, "me");
    if (out.status === 401) clearSession(out); // the session is really over: drop both cookies
    return out;
  }

  const u = (await res.json()) as { id: string; email: string; firstName: string; lastName: string; role?: string };
  const role = (u.role ?? jwtClaims(token!).role) === "ADMIN" ? "ADMIN" : "USER";
  const out = NextResponse.json({ user: { userId: u.id, email: u.email, firstName: u.firstName, lastName: u.lastName, role } });
  if (renewed) setAccessCookie(out, renewed);
  return out;
}
