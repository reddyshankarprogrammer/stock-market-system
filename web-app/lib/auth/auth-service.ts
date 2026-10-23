import { AuthError, defaultMessages, type AuthErrorCode } from "@/lib/auth/errors";
import type { Onboarding, OnboardingInput } from "@/lib/onboarding";
import type { ChangePasswordValues, Preferences, PreferencesInput } from "@/lib/settings";
import type { ForgotValues, LoginValues, RegisterParsed, ResetValues } from "@/lib/auth/schemas";

// Browser-side API for authentication. It talks only to our own route handlers (/api/auth/*, same origin),
// which hold the tokens in httpOnly cookies and call user-service through the gateway. To move to another
// identity system later (Keycloak), change the route handlers; pages and hooks keep using this file.
export type Profile = { userId: string; email: string; firstName: string; lastName: string; role: "USER" | "ADMIN"; onboardingCompleted?: boolean };
export type RegisterResult = { user: Profile; notSaved: string[] };

const CLIENT_TIMEOUT_MS = 10_000; // a little above the 8 s server-side limit

async function call<T>(path: string, init: RequestInit = {}): Promise<T> {
  let res: Response;
  try {
    res = await fetch(path, {
      ...init,
      credentials: "same-origin",
      headers: { "Content-Type": "application/json", ...init.headers },
      signal: AbortSignal.timeout(CLIENT_TIMEOUT_MS),
    });
  } catch {
    throw new AuthError("UNAVAILABLE", defaultMessages.UNAVAILABLE);
  }
  const body = (await res.json().catch(() => ({}))) as { code?: AuthErrorCode; message?: string };
  if (!res.ok) {
    const code = body.code ?? "UNKNOWN";
    throw new AuthError(code, body.message ?? defaultMessages[code], res.status);
  }
  return body as T;
}

export const authService = {
  login: async (v: LoginValues): Promise<Profile> => (await call<{ user: Profile }>("/api/auth/login", { method: "POST", body: JSON.stringify(v) })).user,

  register: (v: RegisterParsed): Promise<RegisterResult> => call<RegisterResult>("/api/auth/register", { method: "POST", body: JSON.stringify(v) }),

  logout: async (): Promise<void> => {
    await call("/api/auth/logout", { method: "POST", body: "{}" });
  },

  /** Always resolves the same way for any email, so the page cannot reveal whether an account exists. */
  forgotPassword: async (v: ForgotValues): Promise<void> => {
    await call("/api/auth/forgot-password", { method: "POST", body: JSON.stringify(v) });
  },

  resetPassword: async (v: ResetValues): Promise<void> => {
    await call("/api/auth/reset-password", { method: "POST", body: JSON.stringify(v) });
  },

  getOnboarding: (): Promise<Onboarding> => call<Onboarding>("/api/onboarding"),

  /** Saves one step or several; only the fields sent change. */
  saveOnboarding: (v: OnboardingInput): Promise<Onboarding> => call<Onboarding>("/api/onboarding", { method: "PUT", body: JSON.stringify(v) }),

  completeOnboarding: (): Promise<Onboarding> => call<Onboarding>("/api/onboarding/complete", { method: "POST", body: "{}" }),

  getPreferences: (): Promise<Preferences> => call<Preferences>("/api/preferences"),

  savePreferences: (v: PreferencesInput): Promise<Preferences> => call<Preferences>("/api/preferences", { method: "PUT", body: JSON.stringify(v) }),

  changePassword: async (v: ChangePasswordValues): Promise<void> => {
    await call("/api/auth/change-password", { method: "POST", body: JSON.stringify(v) });
  },

  me: async (): Promise<Profile> => (await call<{ user: Profile }>("/api/auth/me")).user,
};
