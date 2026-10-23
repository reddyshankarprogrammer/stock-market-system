import { NextResponse, type NextRequest } from "next/server";
import { forgotSchema } from "@/lib/auth/schemas";
import { fail, gateway, sameOriginOnly } from "@/lib/auth/server";

// POST /api/auth/forgot-password -> user-service POST /api/v1/users/forgot-password.
// The answer is the same for every well-formed email (registered or not), so it cannot be used to find out who has an account.
// Only an unreachable service is reported, because then nothing was sent.
export async function POST(req: NextRequest) {
  const blocked = sameOriginOnly(req);
  if (blocked) return blocked;

  const parsed = forgotSchema.safeParse(await req.json().catch(() => null));
  if (!parsed.success) return fail("VALIDATION", 400, parsed.error.issues[0]?.message);

  const res = await gateway("/api/v1/users/forgot-password", { method: "POST", body: JSON.stringify({ email: parsed.data.email }) });
  if (!res || res.status >= 500) return fail("UNAVAILABLE", 503);
  return NextResponse.json({ ok: true }, { status: 202 });
}
