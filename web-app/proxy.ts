import { NextResponse, type NextRequest } from "next/server";
import { ACCESS_COOKIE } from "@/lib/auth/constants";

// Route protection (Next.js 16 "proxy", formerly middleware). It only checks that the session cookie exists; the
// token itself is verified by the gateway on every API call, and /api/auth/me clears an expired cookie.
const PROTECTED = ["/dashboard", "/onboarding"];
const GUEST_ONLY = ["/login", "/register"];

export function proxy(req: NextRequest) {
  const signedIn = Boolean(req.cookies.get(ACCESS_COOKIE)?.value);
  const { pathname } = req.nextUrl;

  if (!signedIn && PROTECTED.some((p) => pathname === p || pathname.startsWith(`${p}/`))) {
    const url = new URL("/login", req.url);
    url.searchParams.set("next", pathname);
    return NextResponse.redirect(url);
  }
  if (signedIn && GUEST_ONLY.includes(pathname)) {
    return NextResponse.redirect(new URL("/dashboard", req.url));
  }
  return NextResponse.next();
}

export const config = { matcher: ["/dashboard/:path*", "/onboarding/:path*", "/login", "/register"] };
