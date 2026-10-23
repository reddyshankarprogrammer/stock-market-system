import { NextResponse, type NextRequest } from "next/server";
import { preferencesSchema } from "@/lib/settings";
import { callAsUser, fail, mapUpstream, sameOriginOnly, setAccessCookie } from "@/lib/auth/server";

// GET /api/preferences -> user-service GET /api/v1/users/me/preferences (the caller's own settings; the identity is the session).
export async function GET(req: NextRequest) {
  const r = await callAsUser(req, "/api/v1/users/me/preferences");
  if (r.kind === "response") return r.response;
  if (!r.res.ok) return mapUpstream(r.res, "me");
  const out = NextResponse.json(await r.res.json());
  if (r.renewed) setAccessCookie(out, r.renewed);
  return out;
}

// PUT /api/preferences saves one setting or several. Only the known fields are forwarded, never anything else from the body.
export async function PUT(req: NextRequest) {
  const blocked = sameOriginOnly(req);
  if (blocked) return blocked;

  const parsed = preferencesSchema.safeParse(await req.json().catch(() => null));
  if (!parsed.success) return fail("VALIDATION", 400, parsed.error.issues[0]?.message);

  const r = await callAsUser(req, "/api/v1/users/me/preferences", { method: "PUT", body: JSON.stringify(parsed.data) });
  if (r.kind === "response") return r.response;
  if (!r.res.ok) return mapUpstream(r.res, "me");
  const out = NextResponse.json(await r.res.json());
  if (r.renewed) setAccessCookie(out, r.renewed);
  return out;
}
