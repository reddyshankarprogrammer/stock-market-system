import { NextResponse, type NextRequest } from "next/server";
import { clearSession, sameOriginOnly } from "@/lib/auth/server";

// POST /api/auth/logout. user-service has no logout or token revocation endpoint, so this clears the cookies.
export async function POST(req: NextRequest) {
  const blocked = sameOriginOnly(req);
  if (blocked) return blocked;
  const out = NextResponse.json({ ok: true });
  clearSession(out);
  return out;
}
