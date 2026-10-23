import { NextResponse, type NextRequest } from "next/server";
import { callAsUser, jwtClaims, mapUpstream, setAccessCookie } from "@/lib/auth/server";
import { ACCESS_COOKIE } from "@/lib/auth/constants";

// GET /api/auth/me -> user-service GET /api/v1/users/me. The session renews itself (see callAsUser).
export async function GET(req: NextRequest) {
  const r = await callAsUser(req, "/api/v1/users/me");
  if (r.kind === "response") return r.response;
  if (!r.res.ok) return mapUpstream(r.res, "me");

  const u = (await r.res.json()) as { id: string; email: string; firstName: string; lastName: string; role?: string; onboardingCompletedAt?: string | null };
  const token = r.renewed ?? req.cookies.get(ACCESS_COOKIE)?.value ?? "";
  const role = (u.role ?? jwtClaims(token).role) === "ADMIN" ? "ADMIN" : "USER";
  const out = NextResponse.json({
    user: { userId: u.id, email: u.email, firstName: u.firstName, lastName: u.lastName, role, onboardingCompleted: Boolean(u.onboardingCompletedAt) },
  });
  if (r.renewed) setAccessCookie(out, r.renewed);
  return out;
}
