import { NextResponse, type NextRequest } from "next/server";
import { TERMS_VERSION } from "@/lib/auth/constants";
import { registerSchema } from "@/lib/auth/schemas";
import { fail, gateway, mapUpstream, profileFrom, sameOriginOnly, setSession } from "@/lib/auth/server";

// POST /api/auth/register -> user-service POST /api/v1/users/register. user-service stores the profile fields
// (mobile, gender, country, terms version) since the "profile fields" step; the acceptance time is set by the server.
// `notSaved` stays in the response (empty) so the browser contract does not change; add names here if a field is ever held back again.
export async function POST(req: NextRequest) {
  const blocked = sameOriginOnly(req);
  if (blocked) return blocked;

  const parsed = registerSchema.safeParse(await req.json().catch(() => null));
  if (!parsed.success) return fail("VALIDATION", 400, parsed.error.issues[0]?.message);
  const { firstName, lastName, email, password, dialCode, mobile, gender, country, termsAccepted } = parsed.data;
  if (!termsAccepted) return fail("VALIDATION", 400); // schema already requires it; belt and braces

  const res = await gateway("/api/v1/users/register", {
    method: "POST",
    body: JSON.stringify({
      firstName,
      lastName,
      email,
      password,
      mobileCountryCode: dialCode,
      mobileNumber: mobile,
      gender: gender.toUpperCase(),
      country,
      termsVersion: TERMS_VERSION,
    }),
  });
  if (!res || !res.ok) return mapUpstream(res, "register");

  const auth = await res.json();
  const out = NextResponse.json({ user: profileFrom(auth), notSaved: [] as string[] }, { status: 201 });
  setSession(out, auth, false);
  return out;
}
