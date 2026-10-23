import { NextResponse, type NextRequest } from "next/server";
import { callAsUser, mapUpstream, sameOriginOnly, setAccessCookie } from "@/lib/auth/server";

// POST /api/onboarding/complete -> user-service POST /api/v1/users/me/onboarding/complete.
export async function POST(req: NextRequest) {
  const blocked = sameOriginOnly(req);
  if (blocked) return blocked;

  const r = await callAsUser(req, "/api/v1/users/me/onboarding/complete", { method: "POST", body: "{}" });
  if (r.kind === "response") return r.response;
  if (!r.res.ok) return mapUpstream(r.res, "me");
  const out = NextResponse.json(await r.res.json());
  if (r.renewed) setAccessCookie(out, r.renewed);
  return out;
}
