// Error codes shared by the route handlers and the browser. The route handlers translate the user-service
// responses into these codes, so no page depends on user-service message text.
export type AuthErrorCode =
  | "INVALID_CREDENTIALS" // wrong password (401) or unknown email (404): deliberately the same message
  | "EMAIL_TAKEN"
  | "INVALID_RESET_TOKEN" // the emailed link is wrong, used, expired or its account is blocked: one message for all
  | "VALIDATION"
  | "UNAUTHENTICATED"
  | "UNAVAILABLE" // gateway down, timed out or returned 5xx
  | "FORBIDDEN_ORIGIN"
  | "UNKNOWN";

export class AuthError extends Error {
  constructor(
    public code: AuthErrorCode,
    message: string,
    public status = 0,
  ) {
    super(message);
    this.name = "AuthError";
  }
}

export const defaultMessages: Record<AuthErrorCode, string> = {
  INVALID_CREDENTIALS: "The email or password is incorrect.",
  EMAIL_TAKEN: "An account with this email already exists. Log in instead.",
  INVALID_RESET_TOKEN: "This reset link is invalid or has expired. Request a new one.",
  VALIDATION: "Please check the highlighted fields and try again.",
  UNAUTHENTICATED: "Your session has ended. Please log in again.",
  UNAVAILABLE: "Quantiva is temporarily unavailable. Please try again in a moment.",
  FORBIDDEN_ORIGIN: "This request was blocked for your security. Reload the page and try again.",
  UNKNOWN: "Something went wrong. Please try again.",
};
