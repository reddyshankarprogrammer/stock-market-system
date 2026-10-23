import { NextResponse, type NextRequest } from "next/server";
import { loginSchema } from "@/lib/auth/schemas";
import { fail, gateway, mapUpstream, profileFrom, sameOriginOnly, setSession } from "@/lib/auth/server";

// POST /api/auth/login -> user-service POST /api/v1/users/login. Tokens go into httpOnly cookies, never into the response body.
export async function POST(req: NextRequest) {
  const blocked = sameOriginOnly(req);
  if (blocked) return blocked;

  const parsed = loginSchema.safeParse(await req.json().catch(() => null));
  if (!parsed.success) return fail("VALIDATION", 400, parsed.error.issues[0]?.message);
  const { email, password, remember } = parsed.data;

  const res = await gateway("/api/v1/users/login", { method: "POST", body: JSON.stringify({ email, password }) });
  if (!res || !res.ok) return mapUpstream(res, "login");

  const auth = await res.json();
  const out = NextResponse.json({ user: profileFrom(auth) });
  setSession(out, auth, remember);
  return out;
}
