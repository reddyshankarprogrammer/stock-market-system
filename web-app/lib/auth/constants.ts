// Cookie names for the session. The tokens live only in httpOnly cookies set by the Next.js route handlers
// (app/api/auth/*); browser JavaScript never sees them.
export const ACCESS_COOKIE = "qv_access";
export const REFRESH_COOKIE = "qv_refresh";

// Version of the Terms and Risk Disclaimer text in lib/auth/legal-content.ts. Bump it when the text changes.
export const TERMS_VERSION = "2026-10-24-draft";

export const REMEMBER_EMAIL_KEY = "qv-remember-email";
export const THEME_KEY = "qv-theme";
export const DEFAULT_AFTER_LOGIN = "/dashboard";

// Only same-site relative paths are allowed as a post-login destination (blocks open redirects).
export function safeNext(next: string | null | undefined): string {
  if (!next || !next.startsWith("/") || next.startsWith("//") || next.includes("\\")) return DEFAULT_AFTER_LOGIN;
  return next;
}
