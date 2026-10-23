import { NextResponse, type NextRequest } from "next/server";
import { resetSchema } from "@/lib/auth/schemas";
import { fail, gateway, mapUpstream, sameOriginOnly } from "@/lib/auth/server";

// POST /api/auth/reset-password -> user-service POST /api/v1/users/reset-password.
// The token comes from the emailed link. No session is created: after a reset the user logs in with the new password.
export async function POST(req: NextRequest) {
  const blocked = sameOriginOnly(req);
  if (blocked) return blocked;

  const parsed = resetSchema.safeParse(await req.json().catch(() => null));
  if (!parsed.success) return fail("VALIDATION", 400, parsed.error.issues[0]?.message);

  const res = await gateway("/api/v1/users/reset-password", {
    method: "POST",
    body: JSON.stringify({ token: parsed.data.token, newPassword: parsed.data.password }),
  });
  if (!res || !res.ok) return mapUpstream(res, "reset");
  return NextResponse.json({ ok: true });
}
