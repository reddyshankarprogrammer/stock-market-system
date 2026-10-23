// Minimal stand-in for api-gateway + user-service, for the auth end-to-end test only. It mimics the documented behaviour
// in user-service/README.md: register (duplicate email -> 400), login (unknown email -> 404, wrong password -> 401),
// refresh (refresh token in the body; access tokens, expired, unknown or blocked users -> 401) and /me (Bearer access token).
// Special emails trigger behaviour: down@ -> login 500, slow@ -> login answers after 9 s (longer than the 8 s limit),
// shortlived@ -> the access token is already expired, suspended@ -> refresh is refused, refresh-down@ -> refresh answers 500.
import { createServer } from "node:http";

const b64 = (o) => Buffer.from(JSON.stringify(o)).toString("base64url");
const now = () => Math.floor(Date.now() / 1000);
let seq = 0; // unique token id, so two tokens issued in the same second still differ
const jwt = (email, type, ttl, role = "USER") => `${b64({ alg: "HS256" })}.${b64({ sub: "u-1", email, role, tokenType: type, exp: now() + ttl, jti: ++seq })}.sig`;
const claimsOf = (token) => {
  try {
    return JSON.parse(Buffer.from(token.split(".")[1], "base64url").toString());
  } catch {
    return null;
  }
};

export function startMockGateway(port = 8099) {
  const users = new Map(); // email -> {firstName, lastName}, so /me returns what was registered
  let lastRegister = null; // the last register body, so the test can check exactly what the web app forwarded
  let refreshCalls = 0;
  // Onboarding answers per email. Users who logged in without registering count as already onboarded (so older tests
  // still reach the dashboard); a registered user starts blank, like a real new account.
  const onboarding = new Map();
  const blank = () => ({ tradingExperience: null, brokerPreferences: [], plan: null, completedAt: null });
  const onboardState = (email) => onboarding.get(email) ?? (users.has(email) ? blank() : { tradingExperience: "INTERMEDIATE", brokerPreferences: ["NONE"], plan: "FREE", completedAt: "2026-10-01T00:00:00" });
  const view = (s) => ({ tradingExperience: s.tradingExperience, brokerPreferences: s.brokerPreferences, plan: s.plan, completed: !!s.completedAt, completedAt: s.completedAt });
  const EXPERIENCE = ["BEGINNER", "INTERMEDIATE", "ADVANCED", "PROFESSIONAL"];
  const BROKERS = ["GROWW", "ZERODHA", "BINANCE", "EXNESS", "OTHER", "NONE"];
  const PLANS = ["FREE", "PRO", "AI_PRO", "QUANT_PRO", "ENTERPRISE"];
  const forgotRequests = []; // emails asked about, so the test can see the answer is the same for known and unknown
  const resetTokens = new Map([["valid-reset-token-0123456789abcdef", "reset-me@example.com"], ["valid-reset-token-for-blocked-0123456789", "blocked@example.com"]]);
  const passwords = new Map(); // email -> password set by a reset
  const tokens = (email) => ({ accessToken: jwt(email, "ACCESS", email === "shortlived@example.com" ? -5 : 900), refreshToken: jwt(email, "REFRESH", 7 * 86400), tokenType: "Bearer" });

  const server = createServer(async (req, res) => {
    const send = (status, body) => {
      res.writeHead(status, { "Content-Type": "application/json" });
      res.end(JSON.stringify(body));
    };
    let raw = "";
    for await (const c of req) raw += c;
    const body = raw ? JSON.parse(raw) : {};
    if (process.env.MOCK_LOG) console.log("  [mock]", req.method, req.url, raw ? raw.slice(0, 60) : "");

    if (req.method === "POST" && req.url === "/api/v1/users/login") {
      if (body.email === "down@example.com") return send(500, { status: 500, message: "boom" });
      if (body.email === "slow@example.com") return setTimeout(() => send(200, {}), 9000);
      if (body.email === "unknown@example.com") return send(401, { status: 401, message: "Invalid Credentials" }); // same as a wrong password
      if (passwords.has(body.email) && passwords.get(body.email) !== body.password) return send(401, { status: 401, message: "Invalid Credentials" });
      if (body.password === "WrongPass123") return send(401, { status: 401, message: "Invalid Credentials" });
      if (body.email === "blocked-login@example.com") return send(403, { status: 403, message: "This account is not active" }); // only after the correct password
      return send(200, { userId: "u-1", email: body.email, firstName: "Test", lastName: "User", walletBalance: 10000, ...tokens(body.email) });
    }
    if (req.method === "POST" && req.url === "/api/v1/users/register") {
      if (body.email === "taken@example.com") return send(400, { status: 400, message: `Email already registered ${body.email}` });
      lastRegister = body;
      users.set(body.email, { firstName: body.firstName, lastName: body.lastName });
      onboarding.set(body.email, blank());
      return send(200, { userId: "u-2", email: body.email, firstName: body.firstName, lastName: body.lastName, walletBalance: 10000, ...tokens(body.email) });
    }
    if (req.method === "POST" && req.url === "/api/v1/users/forgot-password") {
      forgotRequests.push(body.email);
      if (body.email === "forgot-down@example.com") return send(500, { status: 500, message: "boom" });
      return send(202, { message: "If an account exists for that email, a reset link has been sent" }); // identical for every email
    }
    if (req.method === "POST" && req.url === "/api/v1/users/reset-password") {
      const email = resetTokens.get(body.token);
      if (!email || email === "blocked@example.com") return send(400, { status: 400, message: "This reset link is invalid or has expired" });
      resetTokens.delete(body.token); // single use
      passwords.set(email, body.newPassword);
      return send(200, { message: "Your password has been changed" });
    }
    if (req.method === "POST" && req.url === "/api/v1/users/refresh") {
      refreshCalls++;
      const c = claimsOf(body.refreshToken ?? "");
      if (c?.email === "refresh-down@example.com") return send(500, { status: 500, message: "boom" });
      if (!c || c.tokenType !== "REFRESH" || c.exp < now() || c.email === "suspended@example.com") return send(401, { status: 401, message: "Invalid refresh token" });
      return send(200, { userId: c.sub, email: c.email, firstName: "Test", lastName: "User", walletBalance: 10000, accessToken: jwt(c.email, "ACCESS", 900), refreshToken: body.refreshToken, tokenType: "Bearer" });
    }
    if (req.url.startsWith("/api/v1/users/me/onboarding")) {
      const c = claimsOf((req.headers.authorization ?? "").replace("Bearer ", ""));
      if (!c || c.tokenType !== "ACCESS" || c.exp < now()) return send(401, { status: 401, message: "Unauthorized" });
      const s = onboardState(c.email);
      if (req.method === "GET" && req.url === "/api/v1/users/me/onboarding") return send(200, view(s));
      if (req.method === "PUT" && req.url === "/api/v1/users/me/onboarding") {
        if (body.tradingExperience !== undefined && !EXPERIENCE.includes(body.tradingExperience)) return send(400, { status: 400, message: "Validation failed" });
        if (body.plan !== undefined && !PLANS.includes(body.plan)) return send(400, { status: 400, message: "Validation failed" });
        if (body.brokerPreferences !== undefined) {
          const b = [...new Set(body.brokerPreferences)].sort();
          if (!b.length || b.some((x) => !BROKERS.includes(x)) || (b.includes("NONE") && b.length > 1)) return send(400, { status: 400, message: "Choose at least one broker, or NONE" });
          s.brokerPreferences = b;
        }
        if (body.tradingExperience !== undefined) s.tradingExperience = body.tradingExperience;
        if (body.plan !== undefined) s.plan = body.plan;
        onboarding.set(c.email, s);
        return send(200, view(s));
      }
      if (req.method === "POST" && req.url === "/api/v1/users/me/onboarding/complete") {
        if (!s.tradingExperience || !s.brokerPreferences.length || !s.plan) return send(400, { status: 400, message: "Choose your trading experience, your brokers and a plan first" });
        s.completedAt ??= new Date().toISOString();
        onboarding.set(c.email, s);
        return send(200, view(s));
      }
    }
    if (req.method === "GET" && req.url === "/api/v1/users/me") {
      const c = claimsOf((req.headers.authorization ?? "").replace("Bearer ", ""));
      if (!c || c.tokenType !== "ACCESS" || c.exp < now()) return send(401, { status: 401, message: "Unauthorized" });
      const who = users.get(c.email) ?? { firstName: "Test", lastName: "User" };
      return send(200, { id: c.sub, email: c.email, firstName: who.firstName, lastName: who.lastName, walletBalance: 10000, status: "ACTIVE", createdAt: "2026-10-24T00:00:00", role: c.role, onboardingCompletedAt: onboardState(c.email).completedAt });
    }
    if (req.method === "GET" && req.url === "/__last-register") return send(200, lastRegister ?? {});
    if (req.method === "GET" && req.url === "/__stats") return send(200, { refreshCalls, forgotRequests, passwords: Object.fromEntries(passwords) });
    send(404, { message: "not found" });
  });
  return new Promise((resolve) => server.listen(port, () => resolve(server)));
}
