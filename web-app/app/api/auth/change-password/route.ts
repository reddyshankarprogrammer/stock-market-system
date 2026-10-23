import { NextResponse, type NextRequest } from "next/server";
import { changePasswordSchema } from "@/lib/settings";
import { callAsUser, fail, mapUpstream, sameOriginOnly, setSession } from "@/lib/auth/server";

// POST /api/auth/change-password -> user-service POST /api/v1/users/me/password.
// user-service ends every session that started before the change, including this one, and answers with fresh tokens
// for it, so the cookies are replaced here and the user stays signed in on this device. The refresh cookie becomes a
// session cookie (we cannot tell whether "Remember me" was ticked), so the next browser session logs in again.
export async function POST(req: NextRequest) {
  const blocked = sameOriginOnly(req);
  if (blocked) return blocked;

  const parsed = changePasswordSchema.safeParse(await req.json().catch(() => null));
  if (!parsed.success) return fail("VALIDATION", 400, parsed.error.issues[0]?.message);

  const r = await callAsUser(req, "/api/v1/users/me/password", {
    method: "POST",
    body: JSON.stringify({ currentPassword: parsed.data.currentPassword, newPassword: parsed.data.newPassword }),
  });
  if (r.kind === "response") return r.response;
  // 422 is user-service's own status for a wrong current password (never 401: the session itself is fine).
  if (r.res.status === 422) return fail("INVALID_CURRENT_PASSWORD", 422);
  if (!r.res.ok) return mapUpstream(r.res, "me");

  const tokens = await r.res.json().catch(() => null);
  if (!tokens?.accessToken || !tokens?.refreshToken) return fail("UNKNOWN", 502);
  const out = NextResponse.json({ ok: true });
  setSession(out, tokens, false);
  return out;
}
