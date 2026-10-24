import { NextResponse, type NextRequest } from "next/server";
import { waitlistSchema } from "@/lib/waitlist";
import { clientKey, limited } from "@/lib/rate-limit";
import { fail, gateway, sameOriginOnly } from "@/lib/auth/server";

// POST /api/waitlist -> user-service POST /api/v1/users/waitlist (public). Visitors have no session.
// The answer is the same for a new and a listed email, and for a bot that filled the hidden field.
export async function POST(req: NextRequest) {
  const blocked = sameOriginOnly(req);
  if (blocked) return blocked;
  if (limited(`waitlist:${clientKey(req.headers)}`)) return NextResponse.json({ code: "RATE_LIMITED", message: "Too many requests. Please wait a minute and try again." }, { status: 429, headers: { "Retry-After": "60" } });

  const parsed = waitlistSchema.safeParse(await req.json().catch(() => null));
  if (!parsed.success) return fail("VALIDATION", 400, parsed.error.issues[0]?.message);
  if (parsed.data.company) return NextResponse.json({ ok: true }); // honeypot: pretend, store nothing

  const res = await gateway("/api/v1/users/waitlist", { method: "POST", body: JSON.stringify({ email: parsed.data.email, consent: true, source: "landing" }) });
  if (!res || res.status >= 500) return fail("UNAVAILABLE", 503);
  if (res.status === 400) return fail("VALIDATION", 400);
  if (!res.ok) return fail("UNKNOWN", 502);
  return NextResponse.json({ ok: true });
}
