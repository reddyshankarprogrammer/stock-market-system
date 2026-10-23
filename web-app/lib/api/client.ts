import { AuthError, defaultMessages } from "@/lib/auth/errors";

// Shared by the typed clients. They call our own /api/gw route handler (same origin, cookies), never the gateway.
export const API_BASE = "/api/gw";
const CLIENT_TIMEOUT_MS = 10_000;

/** fetch for openapi-fetch: same-origin cookies, a time limit (longer for uploads), and a network failure turned into UNAVAILABLE. */
export const apiFetch = async (input: RequestInfo | URL, init?: RequestInit, timeoutMs = CLIENT_TIMEOUT_MS): Promise<Response> => {
  try {
    return await fetch(input, { ...init, credentials: "same-origin", signal: AbortSignal.timeout(timeoutMs) });
  } catch {
    throw new AuthError("UNAVAILABLE", defaultMessages.UNAVAILABLE);
  }
};

type Result<T> = { data?: T; error?: unknown; response: Response };

/**
 * Unwrap an openapi-fetch result: return the data or throw. 401 means the session is over (the BFF already cleared
 * the cookies); other failures carry the service's own message ({timestamp, status, error, message}).
 */
export async function unwrap<T>(pending: Promise<Result<T>>): Promise<T> {
  const { data, error, response } = await pending;
  if (response.ok && data !== undefined) return data;
  if (response.ok) return undefined as T; // 204 or an empty body
  const body = (error ?? {}) as { code?: string; message?: string };
  if (response.status === 401) throw new AuthError("UNAUTHENTICATED", body.message ?? defaultMessages.UNAUTHENTICATED, 401);
  if (response.status === 503) throw new AuthError("UNAVAILABLE", body.message ?? defaultMessages.UNAVAILABLE, 503);
  throw new AuthError("UNKNOWN", body.message ?? defaultMessages.UNKNOWN, response.status);
}
