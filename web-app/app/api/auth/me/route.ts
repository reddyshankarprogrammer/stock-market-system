import { NextResponse, type NextRequest } from "next/server";
import { ACCESS_COOKIE } from "@/lib/auth/constants";
import { clearSession, fail, gateway, jwtClaims, mapUpstream } from "@/lib/auth/server";

// GET /api/auth/me -> user-service GET /api/v1/users/me with the access token from the cookie.
export async function GET(req: NextRequest) {
  const token = req.cookies.get(ACCESS_COOKIE)?.value;
  if (!token) return fail("UNAUTHENTICATED", 401);

  const res = await gateway("/api/v1/users/me", { headers: { Authorization: `Bearer ${token}` } });
  if (!res || !res.ok) {
    const out = await mapUpstream(res, "me");
    if (out.status === 401) clearSession(out); // expired or invalid token: drop the cookies so the browser stops sending them
    return out;
  }

  const u = (await res.json()) as { id: string; email: string; firstName: string; lastName: string; role?: string };
  const role = (u.role ?? jwtClaims(token).role) === "ADMIN" ? "ADMIN" : "USER";
  return NextResponse.json({ user: { userId: u.id, email: u.email, firstName: u.firstName, lastName: u.lastName, role } });
}
