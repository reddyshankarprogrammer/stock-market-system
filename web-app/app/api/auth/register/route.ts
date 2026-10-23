import { NextResponse, type NextRequest } from "next/server";
import { registerSchema } from "@/lib/auth/schemas";
import { fail, gateway, mapUpstream, profileFrom, sameOriginOnly, setSession } from "@/lib/auth/server";

// Fields user-service does not store yet (see user-service/README.md "Data model"). They are validated here but
// NOT sent, and the response says so, so the UI can tell the user instead of silently dropping them.
// Remove entries from this list when user-service gains the columns (separate step, user-service/CLAUDE.md "Add a field").
const NOT_SAVED_YET = ["mobile", "gender", "country", "termsAcceptance"];

// POST /api/auth/register -> user-service POST /api/v1/users/register.
export async function POST(req: NextRequest) {
  const blocked = sameOriginOnly(req);
  if (blocked) return blocked;

  const parsed = registerSchema.safeParse(await req.json().catch(() => null));
  if (!parsed.success) return fail("VALIDATION", 400, parsed.error.issues[0]?.message);
  const { firstName, lastName, email, password } = parsed.data;

  const res = await gateway("/api/v1/users/register", { method: "POST", body: JSON.stringify({ firstName, lastName, email, password }) });
  if (!res || !res.ok) return mapUpstream(res, "register");

  const auth = await res.json();
  const out = NextResponse.json({ user: profileFrom(auth), notSaved: NOT_SAVED_YET }, { status: 201 });
  setSession(out, auth, false);
  return out;
}
