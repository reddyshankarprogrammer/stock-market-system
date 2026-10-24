// End-to-end test of the authentication pages against a mock gateway (no database or Kafka needed).
//   npm run build
//   $env:API_GATEWAY_URL="http://localhost:8099"; npx next start -p 3100      # terminal 1
//   npm run e2e:auth                                                             # terminal 2
// It checks behaviour in a real browser: validation, error states, cookies, protected routes, terms gating, theme, layout.
import { chromium } from "playwright";
import AxeBuilder from "@axe-core/playwright";
import { startMockGateway } from "./mock-gateway.mjs";

const BASE = process.env.BASE_URL ?? "http://localhost:3100";
const results = [];
const only = process.env.ONLY; // run just the tests whose name contains this text, e.g. ONLY="invalid refresh"
const test = async (name, fn) => {
  if (only && !name.includes(only)) return;
  try {
    await fn();
    results.push([true, name]);
  } catch (e) {
    results.push([false, `${name}: ${String(e.message).split("\n").filter(Boolean).slice(0, 3).join(" | ")}`]);
  }
};
const ok = (cond, msg) => {
  if (!cond) throw new Error(msg);
};

const mock = await startMockGateway(8099);
const browser = await chromium.launch();
// PAGE_LOG=1 prints console errors, failed requests and navigations of every page, to debug a failing test.
const watch = (page) => {
  page.on("console", (m) => m.type() === "error" && console.log("  [page] console.error", m.text().slice(0, 200)));
  page.on("pageerror", (e) => console.log("  [page] error", e.message.slice(0, 300)));
  page.on("requestfailed", (r) => console.log("  [page] request failed", r.url(), r.failure()?.errorText));
  page.on("framenavigated", (f) => f === page.mainFrame() && console.log("  [page] navigated", f.url()));
};
const newCtx = async (o = {}) => {
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 }, ...o });
  if (process.env.PAGE_LOG) ctx.on("page", watch);
  return ctx;
};

async function login(page, email = "trader@example.com", password = "GoodPass123", remember = false) {
  await page.goto(`${BASE}/login`);
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  if (remember) await page.getByLabel("Remember me").check();
  await page.getByRole("button", { name: "Login" }).click();
}

async function fillRegister(page, over = {}) {
  const v = { first: "Asha", last: "Rao", email: "asha@example.com", pw: "Secret12345", pw2: "Secret12345", country: "IN", mobile: "9876543210", gender: "Female", ...over };
  await page.goto(`${BASE}/register`);
  await page.getByLabel("First name").fill(v.first);
  await page.getByLabel("Last name").fill(v.last);
  await page.getByLabel("Email", { exact: true }).fill(v.email);
  await page.getByLabel("Password", { exact: true }).fill(v.pw);
  await page.getByLabel("Confirm password").fill(v.pw2);
  await page.getByLabel("Country").selectOption(v.country);
  await page.getByLabel("Mobile number").fill(v.mobile);
  await page.getByLabel(v.gender, { exact: true }).check();
}
async function acceptTerms(page) {
  await page.getByRole("button", { name: "Read Terms & Risk Disclaimer" }).click();
  await page.getByRole("region", { name: /Terms and risk disclaimer text/ }).evaluate((el) => el.scrollTo(0, el.scrollHeight));
  await page.getByRole("button", { name: "I have read and accept" }).click();
}

// ---- routes and API ----
await test("proxy: /dashboard without a session redirects to /login?next", async () => {
  const c = await newCtx(); const p = await c.newPage();
  await p.goto(`${BASE}/dashboard`);
  ok(p.url().includes("/login?next=%2Fdashboard"), `got ${p.url()}`); await c.close();
});
await test("API: /api/auth/me without a cookie is 401 UNAUTHENTICATED", async () => {
  const c = await newCtx(); const r = await c.request.get(`${BASE}/api/auth/me`);
  ok(r.status() === 401 && (await r.json()).code === "UNAUTHENTICATED", `status ${r.status()}`); await c.close();
});
await test("API: login from a foreign Origin is blocked (403)", async () => {
  const c = await newCtx();
  const r = await c.request.post(`${BASE}/api/auth/login`, { headers: { Origin: "https://evil.example", "Content-Type": "application/json" }, data: { email: "a@b.co", password: "x" } });
  ok(r.status() === 403, `status ${r.status()}`); await c.close();
});

// ---- login ----
await test("login: empty submit shows both errors and focuses the first field", async () => {
  const c = await newCtx(); const p = await c.newPage(); await p.goto(`${BASE}/login`);
  await p.getByRole("button", { name: "Login" }).click();
  await p.getByText("Email is required").waitFor(); await p.getByText("Password is required").waitFor();
  ok((await p.evaluate(() => document.activeElement?.id)) === "email", "focus not on email"); await c.close();
});
await test("login: invalid email format is rejected", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "abc");
  await p.getByText("Enter a valid email address").waitFor(); await c.close();
});
await test("login: wrong password and unknown email give the same message", async () => {
  const c = await newCtx(); const p = await c.newPage();
  await login(p, "trader@example.com", "WrongPass123");
  const alert = p.locator("form [role=alert]"); // not getByRole("alert"): Next.js adds an empty route announcer with that role
  await alert.waitFor();
  const a = await alert.innerText();
  await login(p, "unknown@example.com", "GoodPass123");
  await alert.waitFor();
  const b = await alert.innerText();
  ok(/email or password is incorrect/i.test(a) && a === b, `"${a}" vs "${b}"`); await c.close();
});
await test("login: a blocked account sees its own message only after the correct password", async () => {
  const c = await newCtx(); const p = await c.newPage();
  await login(p, "blocked-login@example.com", "WrongPass123"); // wrong password: must NOT reveal the status
  await p.locator("form [role=alert]").waitFor();
  ok(/email or password is incorrect/i.test(await p.locator("form [role=alert]").innerText()), "status leaked to a wrong password");
  await login(p, "blocked-login@example.com", "GoodPass123");
  await p.getByText(/account is not active/i).waitFor();
  ok(!(await c.cookies()).some((x) => x.name.startsWith("qv_")), "a blocked account must not get a session"); await c.close();
});
await test("login: gateway error (500) shows 'temporarily unavailable'", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "down@example.com");
  await p.getByText(/temporarily unavailable/i).waitFor(); await c.close();
});
await test("login: a gateway that takes 9 s fails cleanly within the 8 s limit", async () => {
  const c = await newCtx(); const p = await c.newPage(); const t0 = Date.now(); await login(p, "slow@example.com");
  await p.getByText(/temporarily unavailable/i).waitFor({ timeout: 12000 });
  const s = (Date.now() - t0) / 1000; ok(s < 10, `took ${s}s`); await c.close();
});
await test("login success: dashboard, httpOnly cookie, no token in page storage", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p);
  await p.waitForURL("**/dashboard"); await p.getByText("Hello, Test").waitFor();
  const cookie = (await c.cookies()).find((x) => x.name === "qv_access");
  ok(cookie?.httpOnly && cookie.sameSite === "Lax", "cookie missing or not httpOnly/Lax");
  const leak = await p.evaluate(() => ({ cookie: document.cookie, ls: JSON.stringify({ ...localStorage }), ss: JSON.stringify({ ...sessionStorage }) }));
  ok(!leak.cookie.includes("qv_") && !/eyJ|sig/.test(leak.ls + leak.ss), "token visible to page scripts"); await c.close();
});
await test("login: signed-in visitors are sent away from /login and /register", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p); await p.waitForURL("**/dashboard");
  await p.goto(`${BASE}/login`); ok(p.url().endsWith("/dashboard"), p.url());
  await p.goto(`${BASE}/register`); ok(p.url().endsWith("/dashboard"), p.url()); await c.close();
});
await test("login: ?next=//evil.com is ignored (no open redirect)", async () => {
  const c = await newCtx(); const p = await c.newPage(); await p.goto(`${BASE}/login?next=//evil.com`);
  await p.getByLabel("Email", { exact: true }).fill("t@example.com"); await p.getByLabel("Password", { exact: true }).fill("GoodPass123");
  await p.getByRole("button", { name: "Login" }).click(); await p.waitForURL("**/dashboard"); await c.close();
});
await test("login: a valid ?next= path is honoured", async () => {
  const c = await newCtx(); const p = await c.newPage(); await p.goto(`${BASE}/login?next=/dashboard`);
  await p.getByLabel("Email", { exact: true }).fill("t@example.com"); await p.getByLabel("Password", { exact: true }).fill("GoodPass123");
  await p.getByRole("button", { name: "Login" }).click(); await p.waitForURL("**/dashboard"); await c.close();
});
await test("logout clears the session; remember me pre-fills the email", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "keep@example.com", "GoodPass123", true);
  await p.waitForURL("**/dashboard"); await p.getByRole("button", { name: "Log out" }).click(); await p.waitForURL("**/login");
  ok(!(await c.cookies()).some((x) => x.name === "qv_access"), "cookie not cleared");
  await p.goto(`${BASE}/dashboard`); ok(p.url().includes("/login"), "dashboard still reachable");
  await p.goto(`${BASE}/login`);
  ok((await p.getByLabel("Email", { exact: true }).inputValue()) === "keep@example.com" && (await p.getByLabel("Remember me").isChecked()), "not remembered"); await c.close();
});

// ---- session renewal (refresh token) ----
const stats = async () => (await (await fetch("http://localhost:8099/__stats")).json()).refreshCalls;
const cookieNamed = async (c, n) => (await c.cookies()).find((x) => x.name === n);

await test("refresh: an expired access cookie is renewed from the refresh cookie, and the user stays signed in", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p); await p.waitForURL("**/dashboard");
  await p.waitForLoadState("networkidle"); // the dashboard's own data requests must be finished: they would renew the session too
  const before = await stats(); const oldToken = (await cookieNamed(c, "qv_access")).value;
  await c.clearCookies({ name: "qv_access" }); // what the browser does when the 15-minute cookie expires
  await p.goto(`${BASE}/dashboard`); await p.getByText("Hello, Test").waitFor();
  ok(p.url().endsWith("/dashboard"), `redirected to ${p.url()}`);
  const fresh = await cookieNamed(c, "qv_access"); ok(fresh?.httpOnly && fresh.value !== oldToken, "no new httpOnly access cookie");
  ok((await stats()) === before + 1, "expected exactly one refresh call"); await c.close();
});
await test("refresh: a 401 from user-service is renewed once and the call is retried", async () => {
  const c = await newCtx(); const p = await c.newPage(); const before = await stats();
  await login(p, "shortlived@example.com"); // its access token is already expired
  await p.waitForURL("**/dashboard"); await p.getByText("Hello, Test").waitFor();
  ok((await stats()) >= before + 1, "no refresh happened"); await c.close();
});
await test("refresh: an invalid refresh token ends the session and clears both cookies", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p); await p.waitForURL("**/dashboard");
  await c.clearCookies({ name: "qv_access" }); await c.clearCookies({ name: "qv_refresh" });
  await c.addCookies([{ name: "qv_refresh", value: "garbage.value.here", url: BASE, httpOnly: true }]);
  await p.goto(`${BASE}/dashboard`); await p.waitForURL("**/login**");
  ok(!(await cookieNamed(c, "qv_refresh")) && !(await cookieNamed(c, "qv_access")), "cookies not cleared"); await c.close();
});
await test("refresh: a blocked user cannot renew", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "suspended@example.com"); await p.waitForURL("**/dashboard");
  await c.clearCookies({ name: "qv_access" }); await p.goto(`${BASE}/dashboard`); await p.waitForURL("**/login**"); await c.close();
});
await test("refresh: a gateway outage while renewing shows an error but does NOT log the user out", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "refresh-down@example.com"); await p.waitForURL("**/dashboard");
  await c.clearCookies({ name: "qv_access" }); await p.goto(`${BASE}/dashboard`);
  await p.getByText(/temporarily unavailable/i).waitFor(); ok(!!(await cookieNamed(c, "qv_refresh")), "refresh cookie was dropped"); await c.close();
});
await test("refresh cookie: a session cookie without Remember me, 7 days with it; tokens still unreadable by scripts", async () => {
  const a = await newCtx(); const pa = await a.newPage(); await login(pa); await pa.waitForURL("**/dashboard");
  const plain = await cookieNamed(a, "qv_refresh"); ok(plain.expires === -1 && plain.httpOnly, `plain: expires ${plain.expires}`); await a.close();
  const b = await newCtx(); const pb = await b.newPage(); await login(pb, "keep2@example.com", "GoodPass123", true); await pb.waitForURL("**/dashboard");
  const kept = await cookieNamed(b, "qv_refresh"); ok(kept.expires > Date.now() / 1000 + 6 * 86400 && kept.httpOnly, `remembered: expires ${kept.expires}`);
  ok(!(await pb.evaluate(() => document.cookie)).includes("qv_"), "cookie visible to scripts"); await b.close();
});

// ---- forgot and reset password ----
const fullStats = async () => (await fetch("http://localhost:8099/__stats")).json();
const VALID_TOKEN = "valid-reset-token-0123456789abcdef";

await test("forgot password: validation, and the same confirmation for a known and an unknown email", async () => {
  const c = await newCtx(); const p = await c.newPage(); await p.goto(`${BASE}/forgot-password`);
  await p.getByRole("button", { name: "Send Reset Link" }).click(); await p.getByText("Email is required").waitFor();
  await p.getByLabel("Email", { exact: true }).fill("nope"); await p.getByRole("button", { name: "Send Reset Link" }).click(); await p.getByText("Enter a valid email address").waitFor();
  const seen = [];
  for (const email of ["known@example.com", "nobody-at-all@example.com"]) {
    await p.goto(`${BASE}/forgot-password`); await p.getByLabel("Email", { exact: true }).fill(email);
    await p.getByRole("button", { name: "Send Reset Link" }).click(); await p.getByText("Check your email").waitFor();
    seen.push((await p.locator("main").innerText()).replace(email, "<email>"));
  }
  ok(seen[0] === seen[1], "the screen differs between a known and an unknown email");
  ok(/If an account exists/.test(seen[0]) && !/not registered|no account|not found/i.test(seen[0]), seen[0]);
  const asked = (await fullStats()).forgotRequests; ok(asked.includes("known@example.com") && asked.includes("nobody-at-all@example.com"), "requests did not reach the gateway"); await c.close();
});
await test("forgot password: an unreachable service is reported (nothing was sent)", async () => {
  const c = await newCtx(); const p = await c.newPage(); await p.goto(`${BASE}/forgot-password`);
  await p.getByLabel("Email", { exact: true }).fill("forgot-down@example.com"); await p.getByRole("button", { name: "Send Reset Link" }).click();
  await p.getByText(/temporarily unavailable/i).waitFor(); await c.close();
});
await test("reset: the token is read once and removed from the address bar; the page sends no referrer and is not cached", async () => {
  const c = await newCtx(); const p = await c.newPage();
  const res = await p.goto(`${BASE}/reset-password?token=${VALID_TOKEN}`); await p.getByRole("heading", { name: "Choose a new password" }).waitFor();
  await p.waitForFunction(() => !location.search.includes("token"));
  ok(!p.url().includes("token"), `token still in the URL: ${p.url()}`);
  const h = res.headers(); ok(h["referrer-policy"] === "no-referrer" && /no-store/.test(h["cache-control"] ?? ""), `headers: ${h["referrer-policy"]} | ${h["cache-control"]}`); await c.close();
});
await test("reset: without a token the page says so and offers a new link", async () => {
  const c = await newCtx(); const p = await c.newPage(); await p.goto(`${BASE}/reset-password`);
  await p.getByText("This reset link is incomplete").waitFor(); await p.getByRole("link", { name: "Request a new link" }).waitFor(); await c.close();
});
await test("reset: password rules and mismatch are checked before anything is sent", async () => {
  const c = await newCtx(); const p = await c.newPage(); await p.goto(`${BASE}/reset-password?token=${VALID_TOKEN}`);
  await p.getByLabel("New password", { exact: true }).fill("short"); await p.getByLabel("Confirm new password").fill("different1");
  await p.getByRole("button", { name: "Change Password" }).click();
  await p.locator("#password-error").filter({ hasText: "Use at least 10 characters" }).waitFor(); await p.locator("#confirmPassword-error").filter({ hasText: "Passwords do not match" }).waitFor(); await c.close();
});
await test("reset: an invalid or blocked token shows one message with a link to ask again", async () => {
  for (const token of ["this-token-does-not-exist-0123456789", "valid-reset-token-for-blocked-0123456789"]) {
    const c = await newCtx(); const p = await c.newPage(); await p.goto(`${BASE}/reset-password?token=${token}`);
    await p.getByLabel("New password", { exact: true }).fill("BrandNewPass9"); await p.getByLabel("Confirm new password").fill("BrandNewPass9");
    await p.getByRole("button", { name: "Change Password" }).click();
    await p.getByText("This reset link is invalid or has expired").first().waitFor(); await p.getByRole("link", { name: "Request a new link" }).waitFor(); await c.close();
  }
});
await test("reset: a valid token changes the password once; the new password logs in and the old one does not", async () => {
  const c = await newCtx(); const p = await c.newPage(); await p.goto(`${BASE}/reset-password?token=${VALID_TOKEN}`);
  await p.getByLabel("New password", { exact: true }).fill("BrandNewPass9"); await p.getByLabel("Confirm new password").fill("BrandNewPass9");
  await p.getByRole("button", { name: "Change Password" }).click(); await p.getByText("Your password has been changed").first().waitFor();
  ok((await fullStats()).passwords["reset-me@example.com"] === "BrandNewPass9", "gateway did not receive the new password");
  ok(!(await c.cookies()).some((x) => x.name.startsWith("qv_")), "a reset must not sign anyone in");
  // the same link cannot be used again
  await p.goto(`${BASE}/reset-password?token=${VALID_TOKEN}`);
  await p.getByLabel("New password", { exact: true }).fill("AnotherNewPass8"); await p.getByLabel("Confirm new password").fill("AnotherNewPass8");
  await p.getByRole("button", { name: "Change Password" }).click(); await p.getByText("This reset link is invalid or has expired").first().waitFor();
  // old password rejected, new accepted
  await login(p, "reset-me@example.com", "GoodPass123"); await p.locator("form [role=alert]").waitFor();
  ok(/email or password is incorrect/i.test(await p.locator("form [role=alert]").innerText()), "old password still works");
  await login(p, "reset-me@example.com", "BrandNewPass9"); await p.waitForURL("**/dashboard"); await c.close();
});

// ---- register ----
await test("register: empty submit lists the required fields", async () => {
  const c = await newCtx(); const p = await c.newPage(); await p.goto(`${BASE}/register`);
  await p.getByRole("button", { name: "Create Account" }).click();
  // Errors are found by their error ids: "Select your country" is also the text of a hidden <option>.
  for (const [id, t] of [["firstName-error", "First name is required"], ["lastName-error", "Last name is required"], ["email-error", "Email is required"], ["password-error", "Use at least 10 characters"],
    ["confirmPassword-error", "Confirm your password"], ["country-error", "Select your country"], ["mobile-error", "Mobile number is required"], ["gender-error", "Select a gender"], ["terms-error", "Read the Terms and Risk Disclaimer to the end"]])
    await p.locator(`#${id}`).filter({ hasText: t }).waitFor({ timeout: 3000 });
  await c.close();
});
await test("register: password rules, mismatch and Indian mobile rule", async () => {
  const c = await newCtx(); const p = await c.newPage();
  await fillRegister(p, { pw: "short", pw2: "different1", mobile: "12345" });
  await p.getByRole("button", { name: "Create Account" }).click();
  await p.getByText("Use at least 10 characters").waitFor(); await p.getByText("Passwords do not match").waitFor();
  await p.getByText(/10-digit Indian mobile number/).waitFor(); await c.close();
});
await test("register: terms cannot be accepted before scrolling to the end", async () => {
  const c = await newCtx(); const p = await c.newPage(); await p.goto(`${BASE}/register`);
  await p.getByRole("button", { name: "Read Terms & Risk Disclaimer" }).click();
  const accept = p.getByRole("button", { name: "I have read and accept" });
  ok(await accept.isDisabled(), "accept enabled too early");
  await p.getByRole("region", { name: /Terms and risk disclaimer text/ }).evaluate((el) => el.scrollTo(0, el.scrollHeight));
  await p.waitForFunction(() => !document.querySelector("dialog button:disabled")?.textContent?.includes("accept"));
  ok(await accept.isEnabled(), "accept still disabled at the end"); await accept.click();
  await p.getByText("You read and accepted the Terms and Risk Disclaimer").waitFor(); await c.close();
});
await test("register: disclaimer states education-only, risk and the market notice date", async () => {
  const c = await newCtx(); const p = await c.newPage(); await p.goto(`${BASE}/register`);
  await p.getByRole("button", { name: "Read Terms & Risk Disclaimer" }).click();
  const text = await p.locator("dialog").innerText();
  for (const t of ["education and information only", "not investment advice", "as of 24 October 2026", "Draft for legal review"]) ok(text.includes(t), `missing "${t}"`); await c.close();
});
await test("register: duplicate email shows an error on the email field", async () => {
  const c = await newCtx(); const p = await c.newPage(); await fillRegister(p, { email: "taken@example.com" }); await acceptTerms(p);
  await p.getByRole("button", { name: "Create Account" }).click(); await p.getByText(/already exists/i).waitFor(); await c.close();
});
await test("register success: dashboard, no 'not saved' notice, and every profile field is forwarded to user-service", async () => {
  const c = await newCtx(); const p = await c.newPage(); await fillRegister(p); await acceptTerms(p);
  await p.getByRole("button", { name: "Create Account" }).click(); await p.waitForURL("**/onboarding"); // a new user goes to the wizard
  await p.getByRole("heading", { name: "What is your trading experience?" }).waitFor();
  ok((await p.getByText("Some details were not saved yet").count()) === 0, "unexpected 'not saved' notice");
  const sent = await (await fetch("http://localhost:8099/__last-register")).json();
  const want = { firstName: "Asha", lastName: "Rao", email: "asha@example.com", mobileCountryCode: "+91", mobileNumber: "9876543210", gender: "FEMALE", country: "IN" };
  for (const [k, v] of Object.entries(want)) ok(sent[k] === v, `${k}: sent ${JSON.stringify(sent[k])}, expected ${JSON.stringify(v)}`);
  ok(/^\d{4}-\d{2}-\d{2}-draft$/.test(sent.termsVersion), `termsVersion ${sent.termsVersion}`);
  ok(!("confirmPassword" in sent) && !("termsAccepted" in sent) && !("dialCode" in sent), "internal form fields leaked to the API"); await c.close();
});

// ---- onboarding ----
let nth = 0;
const onboardEmail = () => `new-user-${Date.now()}-${++nth}@example.com`;
async function registerNew(browserCtx) {
  const page = await browserCtx.newPage();
  await fillRegister(page, { email: onboardEmail() }); await acceptTerms(page);
  await page.getByRole("button", { name: "Create Account" }).click(); await page.waitForURL("**/onboarding");
  await page.getByRole("heading", { name: "What is your trading experience?" }).waitFor();
  return page;
}

await test("onboarding: a new user starts at step 1; Continue without a choice explains what is missing", async () => {
  const c = await newCtx(); const p = await registerNew(c);
  await p.getByText("Step 1 of 4: Trading experience").waitFor();
  ok((await p.locator("li[aria-current=step]").count()) === 1, "stepper must mark exactly one current step");
  await p.getByRole("button", { name: "Continue" }).click();
  await p.getByText("Choose your trading experience").waitFor(); await c.close();
});
await test("onboarding: all four steps, saved as you go, then the dashboard without a redirect loop", async () => {
  const c = await newCtx(); const p = await registerNew(c);
  await p.getByLabel("Intermediate").check(); await p.getByRole("button", { name: "Continue" }).click();
  await p.getByRole("heading", { name: "Which brokers do you use?" }).waitFor();
  ok(await p.evaluate(() => document.activeElement?.tagName === "H1"), "focus did not move to the new step's heading");
  await p.getByRole("button", { name: "Continue" }).click(); await p.getByText("Choose at least one broker").waitFor();
  await p.getByLabel("Groww").check(); await p.getByLabel("Zerodha").check(); await p.getByRole("button", { name: "Continue" }).click();
  await p.getByRole("heading", { name: "Which plan interests you?" }).waitFor();
  for (const name of ["Free", "Pro", "AI Pro", "Quant Pro", "Enterprise"]) await p.getByText(new RegExp(`^${name} `)).first().waitFor();
  await p.getByText("price TBC").first().waitFor(); await p.getByText("Not available yet. Your interest is saved").first().waitFor();
  await p.getByLabel(/^Pro /).check(); await p.getByRole("button", { name: "Continue" }).click();
  await p.getByRole("heading", { name: "Check your answers" }).waitFor();
  const summary = await p.locator("dl").innerText(); ok(/Intermediate/.test(summary) && /Groww, Zerodha/.test(summary) && /Pro/.test(summary), summary);
  await p.getByRole("button", { name: "Complete setup" }).click(); await p.waitForURL(/\/dashboard(\?|$)/);
  await p.getByText(/Hello|Welcome/).first().waitFor(); await p.waitForTimeout(800);
  ok(p.url().includes("/dashboard"), `ended on ${p.url()}`); await c.close();
});
await test("onboarding: 'I do not use a broker yet' excludes the others, and picking a broker clears it", async () => {
  const c = await newCtx(); const p = await registerNew(c);
  await p.getByLabel("Beginner").check(); await p.getByRole("button", { name: "Continue" }).click(); await p.getByRole("heading", { name: "Which brokers do you use?" }).waitFor();
  await p.getByLabel("Groww").check(); await p.getByLabel("I do not use a broker yet").check();
  ok(!(await p.getByLabel("Groww").isChecked()), "Groww should be cleared by 'no broker'");
  await p.getByLabel("Exness").check();
  ok(!(await p.getByLabel("I do not use a broker yet").isChecked()), "'no broker' should be cleared by a broker"); await c.close();
});
await test("onboarding: progress is saved, so a reload resumes at the next step with the earlier answer kept", async () => {
  const c = await newCtx(); const p = await registerNew(c);
  await p.getByLabel("Advanced").check(); await p.getByRole("button", { name: "Continue" }).click(); await p.getByRole("heading", { name: "Which brokers do you use?" }).waitFor();
  await p.reload(); await p.getByRole("heading", { name: "Which brokers do you use?" }).waitFor(); // resumed at step 2
  await p.getByRole("button", { name: "Back" }).click(); await p.getByRole("heading", { name: "What is your trading experience?" }).waitFor();
  ok(await p.getByLabel("Advanced").isChecked(), "the saved experience was lost"); await c.close();
});
await test("onboarding: skipping lasts for this browser session; a new session is asked again", async () => {
  const c = await newCtx(); const p = await registerNew(c);
  await p.getByRole("button", { name: "Skip for now" }).click(); await p.waitForURL(/\/dashboard(\?|$)/);
  await p.getByText(/Hello|Welcome/).first().waitFor(); await p.reload(); await p.getByText(/Hello|Welcome/).first().waitFor();
  ok(p.url().includes("/dashboard"), "skipped session was sent back to onboarding");
  const cookies = await c.cookies(); await c.close();
  const c2 = await newCtx(); await c2.addCookies(cookies); const p2 = await c2.newPage(); await p2.goto(`${BASE}/dashboard`);
  await p2.waitForURL("**/onboarding"); await c2.close();
});
await test("onboarding: a finished user is not sent back, and /onboarding says the setup is complete", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p); await p.waitForURL("**/dashboard"); await p.getByText("Hello, Test").waitFor();
  await p.goto(`${BASE}/onboarding`); await p.getByText("Your setup is complete").waitFor(); await c.close();
});
await test("onboarding: needs a session (redirects to login) and the wizard is keyboard and screen-reader friendly", async () => {
  const c = await newCtx(); const p = await c.newPage(); await p.goto(`${BASE}/onboarding`); ok(p.url().includes("/login?next=%2Fonboarding"), p.url()); await c.close();
  const c2 = await newCtx(); const p2 = await registerNew(c2);
  const radios = await p2.locator("input[type=radio]").evaluateAll((els) => els.filter((e) => e.offsetParent !== null).map((e) => ({ named: !!e.labels?.length, size: e.closest("label").getBoundingClientRect().height })));
  ok(radios.length === 4 && radios.every((r) => r.named && r.size >= 44), JSON.stringify(radios)); await c2.close();
});

// ---- pending screens ----
await test("verify-email is still pending and says so", async () => {
  const c = await newCtx(); const p = await c.newPage();
  for (const path of ["verify-email"]) { await p.goto(`${BASE}/${path}`); await p.getByText("Not available yet", { exact: true }).waitFor(); }
  await c.close();
});

// ---- theme ----
await test("dark and light mode switch and persist", async () => {
  const c = await newCtx({ colorScheme: "light" }); const p = await c.newPage(); await p.goto(`${BASE}/login`);
  const bg = () => p.locator(".theme-app").evaluate((e) => getComputedStyle(e).backgroundColor);
  ok((await bg()) === "rgb(255, 255, 255)", `light bg ${await bg()}`);
  await p.getByRole("button", { name: "dark", exact: true }).click(); ok((await bg()) === "rgb(11, 31, 58)", `dark bg ${await bg()}`);
  await p.reload(); ok((await bg()) === "rgb(11, 31, 58)", "dark not persisted");
  await p.getByRole("button", { name: "light", exact: true }).click(); ok((await bg()) === "rgb(255, 255, 255)", "light not applied");
  await c.close();
  const c2 = await newCtx({ colorScheme: "dark" }); const p2 = await c2.newPage(); await p2.goto(`${BASE}/login`);
  ok((await p2.locator(".theme-app").evaluate((e) => getComputedStyle(e).backgroundColor)) === "rgb(11, 31, 58)", "system dark not followed"); await c2.close();
});

await test("links and outline buttons stay readable in dark mode (silver, 11:1) and light mode (electric, 4.59:1)", async () => {
  for (const [scheme, want] of [["dark", "rgb(220, 227, 234)"], ["light", "rgb(20, 110, 245)"]]) {
    const c = await newCtx({ colorScheme: scheme }); const p = await c.newPage(); await p.goto(`${BASE}/register`);
    const colours = await p.evaluate(() => ["a[href='/login'][class*='text-link']", "button[aria-haspopup='dialog']", "#password ~ button"].map((s) => { const e = document.querySelector(s); return e ? getComputedStyle(e).color : "missing " + s; }));
    ok(colours.every((x) => x === want), `${scheme}: ${colours.join(" | ")}`); await c.close();
  }
});

// ---- layout and accessibility ----
for (const path of ["login", "register"]) {
  await test(`${path}: no horizontal overflow at 320, 375, 768, 1024, 1440, 1920 px`, async () => {
    for (const w of [320, 375, 768, 1024, 1440, 1920]) {
      const c = await newCtx({ viewport: { width: w, height: 900 } }); const p = await c.newPage(); await p.goto(`${BASE}/${path}`);
      const o = await p.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth); ok(!o, `overflow at ${w}px`); await c.close();
    }
  });
}
await test("register: every field has a name and every control is at least 44 px", async () => {
  const c = await newCtx({ viewport: { width: 375, height: 900 } }); const p = await c.newPage(); await p.goto(`${BASE}/register`);
  const r = await p.evaluate(() => {
    const unnamed = [...document.querySelectorAll("input,select")].filter((e) => !(e.labels?.length || e.getAttribute("aria-label"))).map((e) => e.id || e.name);
    const small = [...document.querySelectorAll("main button, main select, main input:not([type=radio]):not([type=checkbox]), main a, footer a, header button")]
      .filter((e) => { const b = e.getBoundingClientRect(); return b.width > 0 && b.height < 43.5 && !e.closest(".sr-only"); }).map((e) => (e.textContent || e.id || e.tagName).trim().slice(0, 25) + ` ${Math.round(e.getBoundingClientRect().height)}px`);
    return { unnamed, small };
  });
  ok(r.unnamed.length === 0, `unnamed: ${r.unnamed}`); ok(r.small.length === 0, `small targets: ${r.small.join(", ")}`); await c.close();
});

// ---- app shell ----
const shellPaths = ["/dashboard", "/accounts", "/trades", "/portfolio", "/settings"];
await test("shell: signed-out visitors are sent to /login from every app page", async () => {
  const c = await newCtx(); const p = await c.newPage();
  for (const path of shellPaths) { await p.goto(`${BASE}${path}`); ok(p.url().includes(`/login?next=${encodeURIComponent(path)}`), `${path} -> ${p.url()}`); }
  await c.close();
});
await test("shell: sidebar from 1024 px up, bottom navigation below; no overflow at 360, 768, 1280, 1920, 2560 px", async () => {
  for (const w of [360, 768, 1023, 1024, 1280, 1920, 2560]) {
    const c = await newCtx({ viewport: { width: w, height: 900 } }); const p = await c.newPage(); await login(p); await p.waitForURL(/\/dashboard/);
    await p.getByRole("heading", { level: 1 }).first().waitFor();
    const r = await p.evaluate(() => {
      const navs = [...document.querySelectorAll('nav[aria-label="Main"]')].map((n) => ({ shown: n.getBoundingClientRect().width > 0, fixed: getComputedStyle(n).position === "fixed" }));
      return { navs, overflow: document.documentElement.scrollWidth > document.documentElement.clientWidth };
    });
    const sidebar = r.navs.find((n) => !n.fixed), bottom = r.navs.find((n) => n.fixed);
    ok(!r.overflow, `overflow at ${w}px`);
    if (w >= 1024) ok(sidebar.shown && !bottom.shown, `${w}px: sidebar ${sidebar.shown}, bottom ${bottom.shown}`);
    else ok(!sidebar.shown && bottom.shown, `${w}px: sidebar ${sidebar.shown}, bottom ${bottom.shown}`);
    await c.close();
  }
});
await test("shell: every nav item opens its page, is marked current, and has a 44 px target (phone and laptop)", async () => {
  for (const [w, label] of [[375, "bottom"], [1280, "sidebar"]]) {
    const c = await newCtx({ viewport: { width: w, height: 900 } }); const p = await c.newPage(); await login(p); await p.waitForURL(/\/dashboard/);
    for (const [name, path] of [["Trades", "/trades"], ["Portfolio", "/portfolio"], ["Settings", "/settings/general"], ["Dashboard", "/dashboard"]]) {
      const link = p.locator('nav[aria-label="Main"]:visible').getByRole("link", { name });
      const box = await link.boundingBox(); ok(box && box.height >= 43.5, `${label} ${name} target ${box?.height}`);
      await link.click(); await p.waitForURL(new RegExp(`${path}(\\?|$)`));
      ok((await link.getAttribute("aria-current")) === "page", `${label} ${name} not marked current`);
      await p.getByRole("heading", { level: 1 }).first().waitFor();
    }
    await c.close();
  }
});

// ---- typed account API (BFF /api/gw) and the Accounts page ----
const gw = (ctx, path) => ctx.request.get(`${BASE}/api/gw${path}`);
await test("accounts: the page lists accounts from the typed client with money formatting and the tracking note", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/accounts`);
  await p.getByRole("heading", { name: "Long-term growth", level: 2 }).waitFor(); await p.getByRole("heading", { name: "Zerodha equity", level: 2 }).waitFor();
  await p.getByText("$12,500.50").waitFor(); await p.getByText("₹0.00").waitFor();
  ok((await p.getByText("Tracking only: no cash moves here").count()) === 1 && (await p.getByText("Cash balance").count()) === 1, "notes");
  await c.close();
});
await test("accounts: no accounts shows an explanation, not an error", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "noaccounts@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/accounts`); await p.getByText("No accounts yet").waitFor(); await c.close();
});
await test("accounts: a failing service shows a clear error with Try again; the session is kept", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "accounts-down@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/accounts`);
  await p.getByText("We could not load your accounts").waitFor({ timeout: 15000 }); await p.getByRole("button", { name: "Try again" }).waitFor();
  ok(!!(await cookieNamed(c, "qv_refresh")), "the session was dropped"); await c.close();
});
await test("accounts: the BFF reaches only account paths, blocks other origins, and refuses signed-out calls", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p); await p.waitForURL(/\/dashboard/);
  ok((await gw(c, "/api/v1/users/me")).status() === 404, "users path");
  ok((await gw(c, "/api/v1/accounts/%2e%2e/users/me")).status() === 404, "dot-dot");
  ok((await gw(c, "/api/v1/accounts/%2e%2e%2fusers/me")).status() === 400, "encoded slash");
  const evil = await c.request.post(`${BASE}/api/gw/api/v1/accounts`, { headers: { "Content-Type": "application/json", Origin: "https://evil.example" }, data: {} });
  ok(evil.status() === 403, `cross-origin ${evil.status()}`);
  const made = await c.request.post(`${BASE}/api/gw/api/v1/accounts`, { headers: { "Content-Type": "application/json", Origin: BASE }, data: { accountName: "Made", accountType: "INDIVIDUAL", brokerName: "PAPER", currency: "USD" } });
  ok(made.status() === 201 && (await made.json()).accountName === "Made", `post ${made.status()}`);
  const other = await gw(c, "/api/v1/accounts/user/someone-else");
  ok(other.status() === 403, `foreign id ${other.status()}`);
  const anon = await newCtx(); ok((await gw(anon, "/api/v1/accounts/user/u-1")).status() === 401, "signed out"); await anon.close(); await c.close();
});
await test("accounts: an expired access cookie is renewed behind the scenes", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p); await p.waitForURL(/\/dashboard/);
  await c.clearCookies({ name: "qv_access" });
  const r = await gw(c, "/api/v1/accounts/user/u-1"); ok(r.status() === 200 && (await r.json()).length >= 2, `renewed ${r.status()}`);
  ok(!!(await cookieNamed(c, "qv_access")), "no new access cookie"); await c.close();
});
await test("accounts: a really ended session sends the page to /login", async () => {
  const c = await newCtx(); await c.addCookies([{ name: "qv_refresh", value: "garbage.value.here", url: BASE, httpOnly: true }]);
  const p = await c.newPage(); await p.goto(`${BASE}/accounts`); await p.waitForURL("**/login**"); await c.close();
});

// ---- settings ----
const settingsPages = ["general", "notifications", "security", "downloads", "about"];
const mockGet = async (path) => (await fetch(`http://localhost:8099${path}`)).json();
await test("settings: /settings opens General; five categories, current one marked, 44 px targets, no overflow at 320 to 1920 px", async () => {
  for (const w of [320, 375, 1280, 1920]) {
    const c = await newCtx({ viewport: { width: w, height: 900 } }); const p = await c.newPage(); await login(p); await p.waitForURL(/\/dashboard/);
    await p.goto(`${BASE}/settings`); await p.waitForURL("**/settings/general");
    const nav = p.getByRole("navigation", { name: "Settings" });
    for (const name of ["General", "Notifications", "Security", "Downloads", "About"]) await nav.getByRole("link", { name }).waitFor();
    ok((await nav.getByRole("link", { name: "General" }).getAttribute("aria-current")) === "page", "current");
    for (const slug of settingsPages) {
      await p.goto(`${BASE}/settings/${slug}`); await p.getByRole("heading", { level: 1 }).first().waitFor();
      const r = await p.evaluate(() => ({ overflow: document.documentElement.scrollWidth > document.documentElement.clientWidth,
        small: [...document.querySelectorAll("main a, main button, main input:not([type=radio]):not([type=checkbox]), main select")].filter((e) => { const b = e.getBoundingClientRect(); return b.width > 0 && b.height < 43.5 && !e.closest(".sr-only"); }).map((e) => (e.textContent || e.id).trim().slice(0, 20)) }));
      ok(!r.overflow, `${slug} overflows at ${w}px`); ok(r.small.length === 0, `${slug} small targets at ${w}px: ${r.small}`);
    }
    await c.close();
  }
});
await test("settings: theme is applied at once and saved to the account; another browser gets it after login", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "theme-user@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/settings/general`); await p.getByLabel("Dark").check();
  await p.waitForFunction(() => document.documentElement.dataset.theme === "dark"); await p.getByText("Saved", { exact: true }).waitFor();
  ok((await mockGet("/__last-prefs")).theme === "DARK", "not saved to the account"); await c.close();
  const c2 = await newCtx(); const p2 = await c2.newPage(); await login(p2, "theme-user@example.com"); await p2.waitForURL(/\/dashboard/);
  await p2.waitForFunction(() => document.documentElement.dataset.theme === "dark", null, { timeout: 15000 }); await c2.close();
});
await test("settings: the shell's theme switch saves to the account too", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "theme-user2@example.com"); await p.waitForURL(/\/dashboard/);
  await p.locator('[aria-label="Colour theme"]:visible').getByRole("button", { name: "light" }).click();
  await p.waitForFunction(async () => (await (await fetch("/api/preferences")).json()).theme === "LIGHT", null, { timeout: 15000 }); await c.close();
});
await test("settings: notifications start opt-in, save on each switch and persist", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "notify@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/settings/notifications`);
  const inApp = p.getByRole("switch", { name: "In-app notifications" }), push = p.getByRole("switch", { name: "Push notifications" }), email = p.getByRole("switch", { name: "Email alerts" });
  await inApp.waitFor();
  ok((await inApp.getAttribute("aria-checked")) === "true" && (await push.getAttribute("aria-checked")) === "false" && (await email.getAttribute("aria-checked")) === "false", "defaults");
  await push.click(); await p.getByText("Saved", { exact: true }).waitFor(); ok((await push.getAttribute("aria-checked")) === "true", "push on");
  await p.reload(); await p.getByRole("switch", { name: "Push notifications" }).waitFor();
  ok((await p.getByRole("switch", { name: "Push notifications" }).getAttribute("aria-checked")) === "true", "not persisted"); await c.close();
});
await test("settings: a failed save puts the switch back and says so", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "prefs-fail@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/settings/notifications`); const email = p.getByRole("switch", { name: "Email alerts" }); await email.waitFor();
  await email.click(); await p.getByText("We could not save that").waitFor({ timeout: 15000 });
  ok((await email.getAttribute("aria-checked")) === "false", "switch not put back"); await c.close();
});
await test("settings: export format offers CSV and JSON, Excel and PDF are visible but disabled", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "export@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/settings/downloads`); await p.getByLabel("CSV").waitFor();
  ok(await p.getByLabel("CSV").isChecked(), "CSV default"); ok(await p.getByLabel(/^Excel/).isDisabled() && await p.getByLabel(/^PDF/).isDisabled(), "excel/pdf");
  await p.getByLabel("JSON").check(); await p.getByText("Saved", { exact: true }).waitFor(); ok((await mockGet("/__last-prefs")).exportFormat === "JSON", "not saved");
  await p.reload(); await p.getByLabel("JSON").waitFor(); ok(await p.getByLabel("JSON").isChecked(), "not persisted"); await c.close();
});
await test("settings: change password checks the form, refuses a wrong current password without ending the session, and keeps this device signed in", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "changer@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/settings/security`);
  const fill = async (cur, next, conf) => { await p.getByLabel("Current password").fill(cur); await p.getByLabel("New password", { exact: true }).fill(next); await p.getByLabel("Confirm new password").fill(conf); await p.getByRole("button", { name: "Change Password" }).click(); };
  await p.getByLabel("Current password").waitFor();
  await fill("GoodPass123", "NewPassword789", "Different789"); await p.getByText("Passwords do not match").waitFor();
  await fill("GoodPass123", "GoodPass123", "GoodPass123"); await p.getByText("different from the current one").waitFor();
  await fill("GoodPass123", "short", "short"); await p.getByText("Use at least 10 characters").waitFor();
  await fill("WrongPass123", "NewPassword789", "NewPassword789"); await p.getByText("Your current password is incorrect").first().waitFor({ timeout: 15000 });
  ok((await mockGet("/__stats")).passwordChanges === 0, "wrong password changed something");
  await p.goto(`${BASE}/dashboard`); await p.getByRole("heading", { level: 1 }).first().waitFor(); ok(!p.url().includes("/login"), "a wrong current password ended the session");
  await p.goto(`${BASE}/settings/security`); await fill("GoodPass123", "NewPassword789", "NewPassword789");
  await p.getByText("Your password has been changed").waitFor({ timeout: 15000 });
  ok(await p.getByLabel("Current password").inputValue() === "", "form not cleared");
  ok(!!(await cookieNamed(c, "qv_access")) && !!(await cookieNamed(c, "qv_refresh")), "cookies missing after the change");
  await p.goto(`${BASE}/dashboard`); await p.getByRole("heading", { level: 1 }).first().waitFor(); ok(!p.url().includes("/login"), "this device was signed out");
  await c.close();
  const c2 = await newCtx(); const p2 = await c2.newPage(); await login(p2, "changer@example.com", "GoodPass123"); await p2.getByText("The email or password is incorrect").first().waitFor({ timeout: 15000 }); await c2.close();
  const c3 = await newCtx(); const p3 = await c3.newPage(); await login(p3, "changer@example.com", "NewPassword789"); await p3.waitForURL(/\/dashboard/); await c3.close();
});
await test("settings: security lists what is not available yet; about shows the version and the legal links", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/settings/security`); await p.getByText("Two-factor authentication").waitFor();
  ok((await p.getByText("Not available yet").count()) === 4, "four planned items");
  await p.goto(`${BASE}/settings/about`); await p.getByText("Version").waitFor();
  ok(/\d+\.\d+\.\d+/.test(await p.locator("dl").innerText()), "version number");
  for (const name of ["Privacy policy", "Terms & conditions", "Risk disclaimer", "Contact support"]) await p.getByRole("link", { name }).waitFor();
  await c.close();
});
await test("settings: the preferences route validates, ignores unknown fields, blocks other origins and refuses signed-out calls", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "bff@example.com"); await p.waitForURL(/\/dashboard/);
  const put = (data, origin = BASE) => c.request.put(`${BASE}/api/preferences`, { headers: { "Content-Type": "application/json", Origin: origin }, data });
  ok((await put({ exportFormat: "EXCEL" })).status() === 400, "EXCEL accepted"); ok((await put({ theme: "NEON" })).status() === 400, "NEON accepted");
  ok((await put({ theme: "DARK", userId: "someone-else", role: "ADMIN" })).status() === 200, "valid save"); const sent = await mockGet("/__last-prefs");
  ok(sent.theme === "DARK" && sent.userId === undefined && sent.role === undefined, `forwarded ${JSON.stringify(sent)}`);
  ok((await put({ theme: "DARK" }, "https://evil.example")).status() === 403, "cross-origin");
  const anon = await newCtx(); ok((await anon.request.get(`${BASE}/api/preferences`)).status() === 401, "signed out GET"); await anon.close(); await c.close();
});

// ---- accounts, detail and statement upload ----
const csv = (name, text) => ({ name, mimeType: "text/csv", buffer: Buffer.from(text) });
const ZERODHA_CSV = "symbol,trade_date,trade_type,quantity,price,trade_id\nINFY,2026-10-01,buy,10,1500,x1\nINFY,2026-10-03,sell,4,1550,x2\nINFY,2026-10-03,hold,1,1550,x3\n";
await test("accounts: the list links to a detail page whose figures come from the summary", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "detail@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/accounts`); await p.getByRole("link", { name: "Long-term growth" }).click(); await p.waitForURL(/\/accounts\/a-1$/);
  await p.getByRole("heading", { name: "Long-term growth", level: 1 }).waitFor();
  for (const t of ["$13,700.50", "$12,500.50", "$1,200.00", "+$80.00", "+$120.00", "1 open position"]) await p.getByText(t, { exact: true }).first().waitFor();
  ok((await p.getByText("Broker statements").count()) === 0, "a platform account has no statement upload"); await c.close();
});
await test("accounts: open an account (validation, then straight to its page); tracking text follows the broker", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "opener@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/accounts`); await p.getByRole("button", { name: "Open an account" }).click();
  await p.getByRole("button", { name: "Open account" }).click(); await p.getByText("Give the account a name").waitFor();
  await p.getByLabel("Held at").selectOption("GROWW"); await p.getByText(/only tracks trades from your broker/).waitFor();
  await p.getByLabel("Account name").fill("My Groww"); await p.getByRole("button", { name: "Open account" }).click();
  await p.waitForURL(/\/accounts\/a-3$/); await p.getByRole("heading", { name: "My Groww", level: 1 }).waitFor();
  await p.getByLabel("Upload a Groww statement").waitFor(); await c.close();
});
await test("statements: a CSV imports, shows what was imported and skipped, and the summary changes", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "uploader@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/accounts/a-2`); await p.getByLabel("Upload a Zerodha statement").setInputFiles(csv("tradebook.csv", ZERODHA_CSV));
  await p.getByText("Statement imported").waitFor({ timeout: 15000 });
  await p.getByText("Imported with warnings").waitFor(); await p.getByText(/2 trades imported, 0 already known, 1 rows skipped \(of 3 read\)/).waitFor();
  await p.getByText("1 warning").click(); await p.getByText("unknown side 'hold'").waitFor();
  await p.getByText("+₹200.00", { exact: true }).first().waitFor(); await c.close();
});
await test("statements: a duplicate, a wrong file type and a failed import each say what is wrong", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "uploader2@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/accounts/a-2`); const input = p.getByLabel("Upload a Zerodha statement"); await input.waitFor();
  await input.setInputFiles(csv("bad.txt", "x")); await p.getByText("Choose a CSV, XLS or XLSX file.").waitFor();
  await input.setInputFiles(csv("dup.csv", ZERODHA_CSV)); await p.getByText("This statement was already uploaded").waitFor({ timeout: 15000 });
  await input.setInputFiles(csv("fail.csv", "nothing")); await p.getByText("Import failed").waitFor({ timeout: 15000 }); await p.getByText("No trade table found in the file").waitFor(); await c.close();
});
await test("statements: a statement can be deleted after a confirmation, and the page is signed-out safe", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "deleter@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/accounts/a-2`); await p.getByLabel("Upload a Zerodha statement").setInputFiles(csv("one.csv", ZERODHA_CSV));
  await p.getByText("one.csv", { exact: true }).waitFor({ timeout: 15000 }); await p.getByRole("button", { name: "Delete" }).click();
  await p.getByRole("button", { name: "Keep" }).click(); await p.getByText("one.csv", { exact: true }).waitFor();
  await p.getByRole("button", { name: "Delete" }).click(); await p.getByRole("button", { name: "Yes, delete" }).click();
  await p.getByText("No statements uploaded yet.").waitFor({ timeout: 15000 }); await c.close();
  const anon = await newCtx(); const q = await anon.newPage(); await q.goto(`${BASE}/accounts/a-2`); await q.waitForURL("**/login**"); await anon.close();
});
await test("statements: uploads through the BFF need the same origin and an allowed path", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p); await p.waitForURL(/\/dashboard/);
  const form = { file: csv("tradebook.csv", ZERODHA_CSV) };
  const evil = await c.request.post(`${BASE}/api/gw/api/v1/accounts/a-2/statements/upload`, { headers: { Origin: "https://evil.example" }, multipart: form });
  ok(evil.status() === 403, `cross-origin upload ${evil.status()}`);
  const other = await c.request.post(`${BASE}/api/gw/api/v1/accounts/a-2/statements`, { headers: { Origin: BASE }, multipart: form });
  ok(other.status() === 415, `multipart elsewhere ${other.status()}`);
  const good = await c.request.post(`${BASE}/api/gw/api/v1/accounts/a-2/statements/upload`, { headers: { Origin: BASE }, multipart: form });
  ok(good.status() === 201, `upload ${good.status()}`); await c.close();
});

// ---- trades ----
const bigCsv = (n) => "symbol,trade_date,trade_type,quantity,price,trade_id\n" + Array.from({ length: n }, (_, i) => `${i % 2 ? "TCS" : "INFY"},2026-09-${String((i % 28) + 1).padStart(2, "0")},buy,${i + 1},${1000 + i},big${i}`).join("\n") + "\n";
const tradeRows = (p) => p.locator("tbody tr");
await test("trades: the table lists the account's trades with the realised profit of the view", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "t1@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/trades`); await p.getByRole("heading", { name: "Trades", level: 1 }).waitFor();
  await p.getByRole("region", { name: "Trades table" }).waitFor(); ok((await tradeRows(p).count()) === 2, "two rows");
  await p.getByText(/2 trades\. Realised profit in this view: \+\$80\.00/).waitFor();
  ok((await p.getByLabel("Account").locator("option").count()) === 2, "account picker"); await c.close();
});
await test("trades: filters (dates go to the API, symbol and side narrow the list) and Clear filters", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "t2@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/trades`); await tradeRows(p).first().waitFor();
  await p.getByLabel("Side").selectOption("SELL"); await p.getByText(/1 trade match your filters/).waitFor(); ok((await tradeRows(p).count()) === 1, "sell only");
  await p.getByLabel("Side").selectOption(""); await p.getByLabel("From").fill("2026-10-04"); await p.getByText(/1 trade match your filters/).waitFor();
  ok((await tradeRows(p).first().innerText()).includes("Sell"), "date filter should keep only the sell");
  await p.getByLabel("To").fill("2026-10-04"); await p.getByText(/0 trades match your filters/).waitFor(); await p.getByLabel("From").fill("2026-10-01"); await p.getByText(/1 trade match your filters/).waitFor();
  ok((await tradeRows(p).first().innerText()).includes("Buy"), "both ends: only the buy lies in 1 to 4 October");
  await p.getByLabel("To").fill(""); await p.getByLabel("From").fill("2026-10-04"); await p.getByText(/1 trade match your filters/).waitFor();
  await p.getByLabel("Symbol").fill("zzz"); await p.getByText("No trades match").waitFor();
  await p.getByRole("button", { name: "Clear filters" }).click(); await p.getByText(/2 trades\./).waitFor(); ok((await p.getByLabel("From").inputValue()) === "", "from not cleared"); await c.close();
});
await test("trades: sorting by a column header toggles direction and says so (aria-sort)", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "t3@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/trades`); await tradeRows(p).first().waitFor();
  const date = p.getByRole("columnheader", { name: /Date/ });
  ok((await date.getAttribute("aria-sort")) === "descending" && (await tradeRows(p).first().innerText()).includes("Sell"), "newest first by default");
  await date.getByRole("button").click(); await p.waitForFunction(() => document.querySelector("th[aria-sort=ascending]"));
  ok((await tradeRows(p).first().innerText()).includes("Buy"), "oldest first after the click");
  await p.getByRole("columnheader", { name: /Realised profit/ }).getByRole("button").click();
  await p.waitForFunction(() => document.querySelector("th[aria-sort=descending]")?.textContent?.includes("Realised profit"));
  ok((await p.getByRole("columnheader", { name: /Realised profit/ }).getAttribute("aria-sort")) === "descending" && (await tradeRows(p).first().innerText()).includes("+$80.00"), "by profit"); await c.close();
});
await test("trades: 20 imported trades are paged 15 + 5, the page survives a reload and Previous is disabled on page 1", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "t4@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/accounts/a-2`); await p.getByLabel("Upload a Zerodha statement").setInputFiles(csv("big.csv", bigCsv(20))); await p.getByText("Statement imported").waitFor({ timeout: 20000 });
  await p.goto(`${BASE}/trades?account=a-2`); await p.getByText("Showing 1 to 15 of 20").waitFor(); ok((await tradeRows(p).count()) === 15, "15 rows");
  ok(await p.getByRole("button", { name: "Previous" }).isDisabled(), "previous on page 1");
  await p.getByRole("button", { name: "Next" }).click(); await p.getByText("Showing 16 to 20 of 20").waitFor(); ok((await tradeRows(p).count()) === 5, "5 rows"); ok(p.url().includes("page=2"), "page in the URL");
  await p.reload(); await p.getByText("Showing 16 to 20 of 20").waitFor(); ok(await p.getByRole("button", { name: "Next" }).isDisabled(), "next on the last page"); await c.close();
});
await test("trades: a trade opens its detail page with the figures and a way back", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "t5@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/trades`); await tradeRows(p).filter({ hasText: "Sell" }).getByRole("link").click(); await p.waitForURL(/\/trades\/t-/);
  await p.getByRole("heading", { name: "Sell AAPL", level: 1 }).waitFor(); await p.locator("dd", { hasText: "+$80.00" }).waitFor(); await p.locator("dd", { hasText: "Quantiva platform" }).waitFor();
  await p.getByRole("link", { name: "← All trades" }).click(); await p.waitForURL(/\/trades\?account=a-1/); await c.close();
});
await test("trades: record a trade by hand (validated) on a tracking account and trade at the market price on a platform account", async () => {
  const c = await newCtx(); const p = await c.newPage(); const form = () => p.getByRole("form", { name: "Record a trade" }); await login(p, "t6@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/trades?account=a-2`); await p.getByRole("button", { name: "Record a trade" }).click();
  await p.getByRole("button", { name: "Record trade" }).click(); await p.getByText("Enter the symbol").waitFor(); await p.getByText("Enter a price above zero").waitFor();
  await form().getByLabel("Symbol").fill("tcs"); await form().getByLabel("Quantity").fill("3"); await form().getByLabel("Price").fill("3600"); await form().getByLabel("Trade date").fill("2999-01-01");
  await p.getByRole("button", { name: "Record trade" }).click(); await p.getByText("The date cannot be in the future").waitFor();
  await form().getByLabel("Trade date").fill("2026-10-10"); await p.getByRole("button", { name: "Record trade" }).click();
  await p.getByRole("cell", { name: "TCS", exact: true }).waitFor({ timeout: 15000 }); await p.getByText("₹3,600.00").waitFor();
  await p.goto(`${BASE}/trades?account=a-1`); await p.getByRole("button", { name: "Record a trade" }).click();
  await form().getByLabel("Symbol").fill("MSFT"); await form().getByLabel("Shares").fill("2"); await p.getByRole("button", { name: "Place trade" }).click();
  await p.getByRole("cell", { name: "MSFT", exact: true }).waitFor({ timeout: 15000 }); await p.getByText("$400.00").first().waitFor();
  await p.getByRole("button", { name: "Record a trade" }).click(); await form().getByLabel("Side").selectOption("SELL"); await form().getByLabel("Symbol").fill("MSFT"); await form().getByLabel("Shares").fill("99");
  await p.getByRole("button", { name: "Place trade" }).click(); await p.getByText("Insufficient shares to sell").waitFor({ timeout: 15000 }); await c.close();
});
await test("trades: the table scrolls inside itself on a phone (no page overflow) and has no small targets", async () => {
  const c = await newCtx({ viewport: { width: 360, height: 800 } }); const p = await c.newPage(); await login(p, "t7@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/trades`); await tradeRows(p).first().waitFor();
  const r = await p.evaluate(() => ({ overflow: document.documentElement.scrollWidth > document.documentElement.clientWidth,
    small: [...document.querySelectorAll("main a, main button, main input, main select")].filter((e) => { const b = e.getBoundingClientRect(); return b.width > 0 && b.height < 43.5; }).map((e) => (e.textContent || e.id).trim().slice(0, 20)) }));
  ok(!r.overflow, "page overflows"); ok(r.small.length === 0, `small targets: ${r.small}`); await c.close();
});

// ---- dashboard and portfolio ----
const apiJson = async (c, path) => (await c.request.get(`${BASE}/api/gw${path}`)).json();
const SELLS_CSV = "symbol,trade_date,trade_type,quantity,price,trade_id\nINFY,2026-09-25,buy,10,1500,d1\nINFY,2026-09-30,sell,5,1600,d2\nINFY,2026-10-02,sell,2,1450,d3\n";
await test("dashboard: key figures are the account's own numbers and the calendar opens on the latest trade's month", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "d1@example.com"); await p.waitForURL(/\/dashboard/);
  await p.getByRole("heading", { name: "Key figures" }).waitFor();
  for (const t of ["$13,700.50", "$12,500.50", "$1,200.00", "+$200.00", "+$80.00", "+$120.00"]) await p.getByText(t, { exact: true }).first().waitFor();
  await p.getByRole("status").filter({ hasText: "October 2026" }).waitFor();
  const cell = p.getByRole("cell", { name: /^5 October: profit \$80\.00, 1 trade/ }); await cell.waitFor(); ok((await cell.innerText()).includes("+$80.00"), "day cell");
  ok((await p.getByTestId("month-total").innerText()) === "+$80.00", "month total"); ok((await p.getByTestId("all-time").innerText()) === "+$80.00", "all time");
  await p.getByRole("button", { name: "Previous month" }).click(); await p.getByRole("status").filter({ hasText: "September 2026" }).waitFor();
  ok((await p.getByTestId("month-total").innerText()) === "$0.00", "empty month"); await c.close();
});
await test("dashboard: every calendar day equals the sum of that day's trades and all days equal the account's realised profit", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "d2@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/accounts/a-2`); await p.getByLabel("Upload a Zerodha statement").setInputFiles(csv("sells.csv", SELLS_CSV)); await p.getByText("Statement imported").waitFor({ timeout: 20000 });
  const trades = await apiJson(c, "/api/v1/trades/account/a-2"), perf = await apiJson(c, "/api/v1/accounts/a-2/performance");
  const byDay = {}; for (const t of trades) byDay[t.tradeDate.slice(0, 10)] = Math.round(((byDay[t.tradeDate.slice(0, 10)] ?? 0) + t.realizedProfit) * 100) / 100;
  await p.goto(`${BASE}/dashboard?account=a-2`); await p.getByRole("status").filter({ hasText: "October 2026" }).waitFor();
  const oct = await p.locator("td[data-day]").evaluateAll((els) => els.map((e) => [e.getAttribute("aria-label"), Number(e.getAttribute("data-day"))]));
  ok(oct.length === 1 && oct[0][1] === byDay["2026-10-02"] && byDay["2026-10-02"] === -100, `october ${JSON.stringify(oct)} vs ${JSON.stringify(byDay)}`);
  ok((await p.getByTestId("month-total").innerText()) === "-₹100.00", "october total");
  await p.getByRole("button", { name: "Previous month" }).click(); await p.getByRole("status").filter({ hasText: "September 2026" }).waitFor();
  ok((await p.getByTestId("month-total").innerText()) === "+₹500.00", "september total");
  ok((await p.getByTestId("all-time").innerText()) === "+₹400.00" && perf.realizedProfit === 400, `all time vs API ${perf.realizedProfit}`);
  ok((await p.getByText(/the account reports/).count()) === 0, "calendar disagrees with the account"); await c.close();
});
await test("dashboard: the account picker switches figures; an empty account shows zeros, not errors; no overflow on a phone", async () => {
  const c = await newCtx({ viewport: { width: 360, height: 800 } }); const p = await c.newPage(); await login(p, "d3@example.com"); await p.waitForURL(/\/dashboard/);
  await p.getByRole("heading", { name: "Key figures" }).waitFor(); await p.getByLabel("Account").selectOption("a-2");
  await p.getByText("0 open positions").waitFor(); await p.getByText("+$80.00", { exact: true }).waitFor({ state: "detached" });
  ok(!(await p.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)), "page overflows");
  const small = await p.evaluate(() => [...document.querySelectorAll("main a, main button, main select")].filter((e) => { const b = e.getBoundingClientRect(); return b.width > 0 && b.height < 43.5; }).map((e) => (e.textContent || e.id).trim().slice(0, 20)));
  ok(small.length === 0, `small targets: ${small}`); await c.close();
});
await test("portfolio: open positions at current prices; prices refresh on request and by themselves", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p, "p1@example.com"); await p.waitForURL(/\/dashboard/);
  try {
    await p.goto(`${BASE}/portfolio`); await p.getByRole("region", { name: "Holdings table" }).waitFor();
    const row = p.locator("tbody tr").filter({ hasText: "AAPL" }); await row.waitFor();
    const text = await row.innerText(); for (const t of ["6", "$180.00", "$200.00", "$1,200.00", "+$120.00", "+11.11%"]) ok(text.includes(t), `row lacks ${t}: ${text}`);
    await mockGet("/__set-price?symbol=AAPL&price=250"); await p.getByRole("button", { name: "Refresh prices" }).click();
    await p.locator("tbody tr").filter({ hasText: "$250.00" }).waitFor({ timeout: 15000 }); await p.getByText("+$420.00").first().waitFor();
    await mockGet("/__set-price?symbol=AAPL&price=300"); await p.locator("tbody tr").filter({ hasText: "$300.00" }).waitFor({ timeout: 20000 }); // the 10 s poll
  } finally { await mockGet("/__set-price?symbol=AAPL&price=200"); await c.close(); }
});
await test("portfolio: an account without positions says so; the picker and a phone layout work", async () => {
  const c = await newCtx({ viewport: { width: 360, height: 800 } }); const p = await c.newPage(); await login(p, "p2@example.com"); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/portfolio?account=a-2`); await p.getByText("No open positions").waitFor();
  await p.getByLabel("Account").selectOption("a-1"); await p.getByRole("region", { name: "Holdings table" }).waitFor();
  ok(!(await p.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)), "page overflows"); await c.close();
});

// ---- landing page waitlist ----
let ipSeq = 0;
const fromIp = (o = {}) => newCtx({ ...o, extraHTTPHeaders: { "x-forwarded-for": `203.0.113.${++ipSeq}` } }); // each test is a different visitor for the rate limit
const waitlistForm = (p) => p.getByRole("form", { name: "Join the launch list" });
await test("waitlist: the form checks the email and the consent before anything is sent", async () => {
  const c = await fromIp(); const p = await c.newPage(); await p.goto(`${BASE}/`);
  const form = waitlistForm(p); await form.scrollIntoViewIfNeeded();
  await form.getByRole("button", { name: "Join the list" }).click(); await p.getByText("Enter your email").waitFor(); await p.getByText("Please agree to be contacted about the launch").waitFor();
  await form.getByLabel("Email").fill("not-an-email"); await form.getByLabel(/I agree to be contacted/).check(); await form.getByRole("button", { name: "Join the list" }).click(); await p.getByText("Enter a valid email address").waitFor();
  ok((await mockGet("/__waitlist")).length === 0 || !(await mockGet("/__waitlist")).includes("not-an-email"), "an invalid email reached the backend"); await c.close();
});
await test("waitlist: joining shows the confirmation; the same address again gets the same answer and is stored once, in lower case", async () => {
  const c = await fromIp(); const p = await c.newPage(); await p.goto(`${BASE}/`);
  const join = async (page) => { const f = waitlistForm(page); await f.getByLabel("Email").fill("Fan@Example.com"); await f.getByLabel(/I agree to be contacted/).check(); await f.getByRole("button", { name: "Join the list" }).click(); await page.getByText("You are on the list. We will write to you when Quantiva opens.").waitFor({ timeout: 15000 }); };
  await join(p); await c.close();
  const c2 = await fromIp(); const p2 = await c2.newPage(); await p2.goto(`${BASE}/`); await join(p2); await c2.close();
  ok((await mockGet("/__waitlist")).filter((e) => e === "fan@example.com").length === 1, "stored more than once or not in lower case");
});
await test("waitlist: a service outage is reported and nothing claims success", async () => {
  const c = await fromIp(); const p = await c.newPage(); await p.goto(`${BASE}/`);
  const f = waitlistForm(p); await f.getByLabel("Email").fill("down@waitlist.example"); await f.getByLabel(/I agree to be contacted/).check(); await f.getByRole("button", { name: "Join the list" }).click();
  await p.getByRole("alert").filter({ hasText: "temporarily unavailable" }).waitFor({ timeout: 15000 }); ok((await p.getByText("You are on the list").count()) === 0, "claimed success"); await c.close();
});
await test("waitlist: the route ignores bots (honeypot), blocks other origins, and slows a script down (429 with Retry-After)", async () => {
  const c = await fromIp(); const post = (data, origin = BASE) => c.request.post(`${BASE}/api/waitlist`, { headers: { "Content-Type": "application/json", Origin: origin }, data });
  const bot = await post({ email: "bot@example.com", consent: true, company: "Acme" }); ok(bot.status() === 200, `honeypot ${bot.status()}`);
  ok(!(await mockGet("/__waitlist")).includes("bot@example.com"), "the honeypot answer must store nothing");
  ok((await post({ email: "x@example.com", consent: false })).status() === 400, "consent false accepted");
  ok((await post({ email: "x@example.com", consent: true }, "https://evil.example")).status() === 403, "cross-origin");
  let limited = null; for (let i = 0; i < 12 && !limited; i++) { const r = await post({ email: `burst${i}@example.com`, consent: true }); if (r.status() === 429) limited = r; }
  ok(limited, "no 429 after 12 quick requests"); ok(limited.headers()["retry-after"] === "60", "Retry-After"); await c.close();
});
await test("waitlist: the landing page has no sideways scroll at 320 px with the form on it", async () => {
  const c = await fromIp({ viewport: { width: 320, height: 800 } }); const p = await c.newPage(); await p.goto(`${BASE}/`); await waitlistForm(p).waitFor();
  ok(!(await p.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)), "overflow");
  const small = await waitlistForm(p).evaluate((f) => [...f.querySelectorAll("button, input:not([tabindex='-1']):not([type=checkbox])")].filter((e) => e.getBoundingClientRect().height < 43.5).map((e) => e.id || e.tagName));
  ok(small.length === 0, `small targets: ${small}`); await c.close();
});

// ---- trading terminal (sample data) ----
const terminal = async (o = {}) => { const c = await newCtx(o); const p = await c.newPage(); await login(p, "term@example.com"); await p.waitForURL(/\/dashboard/); await p.goto(`${BASE}/terminal`); await p.getByRole("heading", { name: "Dashboard", level: 1 }).waitFor(); return [c, p]; };
const kpi = async (p, key) => (await p.getByTestId(`kpi-${key}`).innerText()).trim();
await test("terminal: it needs a session; the page says its data is sample data; seven KPI cards with a trend, a sparkline and the figure", async () => {
  const anon = await newCtx(); const q = await anon.newPage(); await q.goto(`${BASE}/terminal`); ok(q.url().includes("/login?next=%2Fterminal"), q.url()); await anon.close();
  const [c, p] = await terminal();
  await p.getByText("Sample data: these are not your accounts or your trades").waitFor();
  const cards = p.locator("[data-kpi]"); ok((await cards.count()) === 7, "seven KPI cards");
  for (const label of ["Portfolio value", "Today's P&L", "Monthly P&L", "Win rate", "Open positions", "Risk score", "Sharpe ratio"]) await cards.filter({ hasText: label }).waitFor();
  ok((await cards.first().locator("svg").count()) >= 1, "sparkline"); ok(/^\$\d{3},\d{3}$/.test(await kpi(p, "value")), `portfolio value ${await kpi(p, "value")}`); await c.close();
});
await test("terminal: sidebar lists the 16 destinations, only the dashboard is a link, the rest say they are not built; right panel actions are honest", async () => {
  const [c, p] = await terminal({ viewport: { width: 1440, height: 900 } });
  const nav = p.getByRole("navigation", { name: "Terminal" }); ok((await nav.locator("li").count()) === 16, "16 items");
  ok((await nav.getByRole("link").count()) === 1 && (await nav.getByRole("link", { name: "Dashboard" }).getAttribute("aria-current")) === "page", "only Dashboard is a link");
  ok((await nav.locator("button[aria-disabled=true]").count()) === 15, "15 disabled items"); await nav.getByRole("button", { name: /Markets/ }).click({ force: true }); // aria-disabled: Playwright would wait for it to be enabled ok(p.url().endsWith("/terminal"), "a disabled item navigated");
  await p.getByRole("link", { name: "Place order" }).waitFor(); ok((await p.locator("aside button[aria-disabled=true]").filter({ hasText: "Soon" }).count()) >= 3, "run screener, backtest, signal are marked soon");
  await p.getByText("Not generated from your data and not investment advice").waitFor(); await c.close();
});
await test("terminal: filters narrow the trades and every KPI follows (open positions equals the open rows)", async () => {
  const [c, p] = await terminal({ viewport: { width: 1440, height: 900 } });
  await p.getByText("96 of 96 sample trades match").waitFor(); const openAll = Number(await kpi(p, "open"));
  await p.getByLabel("Strategy").selectOption("Momentum"); await p.getByText(/^\d+ of 96 sample trades match$/).filter({ hasNotText: "96 of 96" }).waitFor();
  const shown = Number((await p.getByText(/^\d+ of 96 sample trades match$/).innerText()).split(" ")[0]); ok(shown > 0 && shown < 96, `shown ${shown}`);
  await p.getByRole("group", { name: "Status" }).getByRole("button", { name: "Open" }).click(); const openNow = Number(await kpi(p, "open"));
  ok(openNow <= openAll, "open positions should not grow"); ok((await p.getByTestId("range").innerText()).endsWith(`of ${openNow}`), `table ${await p.getByTestId("range").innerText()} vs KPI ${openNow}`);
  await p.getByLabel("Market").selectOption("India"); await p.getByLabel("Exchange").selectOption("NASDAQ"); await p.getByText("No sample trades match the filters.").waitFor();
  await p.getByRole("button", { name: "Clear filters" }).click(); await p.getByText("96 of 96 sample trades match").waitFor(); await c.close();
});
await test("terminal: the table sorts (aria-sort), pages, hides columns and exports exactly what it shows as CSV", async () => {
  const [c, p] = await terminal({ viewport: { width: 1440, height: 900 } });
  const table = p.getByRole("region", { name: "Trading activity table" }); const head = table.getByRole("columnheader");
  ok((await head.count()) === 12, "12 columns"); for (const h of ["Status", "Entry date", "Exit date", "Symbol", "Company", "Strategy", "Buy price", "Sell price", "Quantity", "Profit/Loss", "ROI %", "Holding days"]) await table.getByRole("columnheader", { name: h }).waitFor();
  await p.getByTestId("range").filter({ hasText: "Showing 1 to 10 of 96" }).waitFor(); ok(await p.getByRole("button", { name: "Previous" }).isDisabled(), "previous on page 1");
  await p.getByRole("button", { name: "Next" }).click(); await p.getByText("Showing 11 to 20 of 96").waitFor();
  const pl = table.getByRole("columnheader", { name: "Profit/Loss" }); await pl.getByRole("button").click(); await pl.getByRole("button").click();
  ok((await pl.getAttribute("aria-sort")) === "ascending", "ascending after two clicks"); await p.getByText("Showing 1 to 10 of 96").waitFor();
  const nums = await table.locator("tbody tr").evaluateAll((rows) => rows.map((r) => Number(r.children[9].textContent.replace(/[^0-9.-]/g, "").replace(/^(\d)/, "$1")) * (r.children[9].textContent.trim().startsWith("-") ? 1 : 1)));
  ok(nums.length === 10, "ten rows"); ok(nums.every((n, i) => i === 0 || Math.abs(n) >= 0), "numbers parse");
  const signedVals = await table.locator("tbody tr").evaluateAll((rows) => rows.map((r) => { const t = r.children[9].textContent.trim(); const n = Number(t.replace(/[^0-9.]/g, "")); return t.startsWith("-") ? -n : n; }));
  ok(signedVals.every((v, i) => i === 0 || signedVals[i - 1] <= v), `not ascending: ${signedVals}`);
  await p.getByRole("button", { name: "Columns" }).click(); await p.getByLabel("Company").uncheck(); await p.keyboard.press("Escape");
  ok((await table.getByRole("columnheader", { name: "Company" }).count()) === 0 && (await head.count()) === 11, "Company column hidden");
  await p.getByLabel("Strategy").selectOption("Value"); const total = Number((await p.getByText(/^\d+ of 96 sample trades match$/).innerText()).split(" ")[0]);
  const [download] = await Promise.all([p.waitForEvent("download"), p.getByRole("button", { name: "Export CSV" }).click()]);
  ok(download.suggestedFilename() === "quantiva-sample-trades.csv", download.suggestedFilename());
  const text = await (await import("node:fs/promises")).readFile(await download.path(), "utf8"); const lines = text.trim().split("\r\n");
  ok(lines.length === total + 1, `csv lines ${lines.length} vs ${total} filtered trades + header`); ok(lines[0].split(",").length === 11 && !lines[0].includes("Company"), `csv header ${lines[0]}`);
  ok(lines.slice(1).every((l) => l.includes("Value")), "csv rows are the filtered rows"); await c.close();
});
await test("terminal: every chart has its numbers as a table for screen readers; the picture is hidden from them", async () => {
  const [c, p] = await terminal({ viewport: { width: 1440, height: 900 } });
  for (const [name, rows] of [["Equity curve", null], ["Monthly returns", 12], ["Win / loss ratio", 2], ["Sector performance", null], ["Sector allocation", null], ["Asset allocation", null], ["Geographic allocation", null]]) {
    const fig = p.getByRole("figure", { name }); await fig.waitFor();
    const n = await fig.locator("table tbody tr").count(); ok(n > 0 && (rows === null || n === rows), `${name}: ${n} table rows`);
    ok((await fig.locator("[aria-hidden=true] svg, svg[aria-hidden=true]").count()) >= 1, `${name}: the chart should be hidden from assistive technology`);
  }
  ok(/^\d+\.\d{2}$/.test((await p.getByTestId("winloss-ratio").innerText()).trim()), "ratio text"); await c.close();
});
await test("terminal: the sidebar collapses to icons, the choice survives a reload; a phone gets a drawer with Escape to close", async () => {
  const [c, p] = await terminal({ viewport: { width: 1440, height: 900 } });
  const aside = p.locator("aside").first(); const w0 = (await aside.boundingBox()).width;
  await p.getByRole("button", { name: "Collapse the sidebar" }).click(); await p.getByRole("button", { name: "Expand the sidebar" }).waitFor();
  await p.waitForTimeout(400); ok((await aside.boundingBox()).width < w0 / 2, "sidebar did not shrink"); await p.reload(); await p.getByRole("button", { name: "Expand the sidebar" }).waitFor();
  await p.getByRole("button", { name: "Expand the sidebar" }).click(); await c.close();
  const [c2, q] = await terminal({ viewport: { width: 360, height: 800 } });
  ok(!(await q.getByRole("navigation", { name: "Terminal" }).isVisible()), "nav hidden before the menu opens");
  await q.getByRole("button", { name: "Open the menu" }).click(); const dlg = q.getByRole("dialog", { name: "Terminal menu" }); await dlg.waitFor(); await dlg.getByRole("link", { name: "Dashboard" }).waitFor();
  await q.keyboard.press("Escape"); await dlg.waitFor({ state: "detached" }); ok(await q.getByRole("button", { name: "Open the menu" }).evaluate((e) => e === document.activeElement), "focus did not return"); await c2.close();
});
await test("terminal: no sideways scroll at 360, 768, 1024, 1440 and 2560 px; controls are at least 44 px high", async () => {
  for (const w of [360, 768, 1024, 1440, 2560]) {
    const [c, p] = await terminal({ viewport: { width: w, height: 900 } }); await p.waitForTimeout(500);
    ok(!(await p.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)), `overflow at ${w}px`);
    const small = await p.evaluate(() => [...document.querySelectorAll("main button, main select, main input:not([type=checkbox]), aside a, aside button, header button")].filter((e) => { const b = e.getBoundingClientRect(); return b.width > 0 && b.height < 43.5 && !e.closest(".sr-only") && !e.closest("[aria-hidden=true]"); }).map((e) => (e.textContent || e.id || e.tagName).trim().slice(0, 18)));
    ok(small.length === 0, `small controls at ${w}px: ${small}`); await c.close();
  }
});
await test("terminal: no WCAG 2.1 A or AA violations found by axe, on a desktop and on a phone", async () => {
  for (const w of [1440, 360]) {
    const [c, p] = await terminal({ viewport: { width: w, height: 900 } }); await p.waitForTimeout(800);
    const r = await new AxeBuilder({ page: p }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
    ok(r.violations.length === 0, `${w}px: ` + r.violations.map((v) => `${v.id} (${v.nodes.length}): ${v.nodes[0].target.join(" ")} ${String(v.nodes[0].failureSummary).split("\n")[1] ?? ""}`).join(" | ")); await c.close();
  }
});

// ---- switching between app mode and terminal mode ----
await test("mode switch: one click between the app and the terminal, in both directions, on a laptop and on a phone", async () => {
  for (const [w, label] of [[1440, "laptop"], [360, "phone"]]) {
    const c = await newCtx({ viewport: { width: w, height: 900 } }); const p = await c.newPage(); await login(p, "mode@example.com"); await p.waitForURL(/\/dashboard/);
    const sw = () => p.getByRole("navigation", { name: "Mode" }).and(p.locator(":visible"));
    await sw().getByRole("link", { name: "App" }).waitFor(); ok((await sw().getByRole("link", { name: "App" }).getAttribute("aria-current")) === "page", `${label}: App is current in the app`);
    await sw().getByRole("link", { name: "Terminal" }).click(); await p.waitForURL(/\/terminal$/); await p.getByRole("heading", { name: "Dashboard", level: 1 }).waitFor();
    ok((await sw().getByRole("link", { name: "Terminal" }).getAttribute("aria-current")) === "page", `${label}: Terminal is current in the terminal`);
    ok(!(await p.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)), `${label}: terminal overflows`);
    await sw().getByRole("link", { name: "App" }).click(); await p.waitForURL(/\/dashboard/); await p.getByRole("heading", { level: 1 }).first().waitFor();
    ok(!(await p.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)), `${label}: app overflows`);
    await c.close();
  }
});
await test("mode switch: the phone drawer has it too, and a collapsed terminal sidebar keeps it as icons", async () => {
  const [c, q] = await terminal({ viewport: { width: 360, height: 800 } });
  await q.getByRole("button", { name: "Open the menu" }).click(); const dlg = q.getByRole("dialog", { name: "Terminal menu" });
  await dlg.getByRole("navigation", { name: "Mode" }).getByRole("link", { name: "App" }).click(); await q.waitForURL(/\/dashboard/); await c.close();
  const [c2, p] = await terminal({ viewport: { width: 1440, height: 900 } });
  await p.getByRole("button", { name: "Collapse the sidebar" }).click(); await p.getByRole("button", { name: "Expand the sidebar" }).waitFor();
  const icons = p.locator("aside").getByRole("navigation", { name: "Mode" }); const app = icons.getByRole("link", { name: "App" }); ok((await app.boundingBox()).height >= 43.5, "icon target too small");
  await app.click(); await p.waitForURL(/\/dashboard/); await p.goto(`${BASE}/terminal`); await p.getByRole("button", { name: "Expand the sidebar" }).click(); await c2.close();
});

// ---- switching between app mode and terminal mode ----
await test("mode switch: one click between the app and the terminal, in both directions, on a laptop and on a phone", async () => {
  for (const [w, label] of [[1440, "laptop"], [360, "phone"]]) {
    const c = await newCtx({ viewport: { width: w, height: 900 } }); const p = await c.newPage(); await login(p, "mode@example.com"); await p.waitForURL(/\/dashboard/);
    const sw = () => p.getByRole("navigation", { name: "Mode" }).filter({ visible: true });
    await sw().getByRole("link", { name: "App" }).waitFor(); ok((await sw().getByRole("link", { name: "App" }).getAttribute("aria-current")) === "page", `${label}: App is current in the app`);
    await sw().getByRole("link", { name: "Terminal" }).click(); await p.waitForURL(/\/terminal$/); await p.getByRole("heading", { name: "Dashboard", level: 1 }).waitFor();
    ok((await sw().getByRole("link", { name: "Terminal" }).getAttribute("aria-current")) === "page", `${label}: Terminal is current in the terminal`);
    ok(!(await p.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)), `${label}: terminal overflows`);
    await sw().getByRole("link", { name: "App" }).click(); await p.waitForURL(/\/dashboard/); await p.getByRole("heading", { level: 1 }).first().waitFor();
    ok(!(await p.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)), `${label}: app overflows`);
    await c.close();
  }
});
await test("mode switch: the phone drawer has it too, and a collapsed terminal sidebar keeps it as icons", async () => {
  const [c, q] = await terminal({ viewport: { width: 360, height: 800 } });
  await q.getByRole("button", { name: "Open the menu" }).click(); const dlg = q.getByRole("dialog", { name: "Terminal menu" });
  await dlg.getByRole("navigation", { name: "Mode" }).getByRole("link", { name: "App" }).click(); await q.waitForURL(/\/dashboard/); await c.close();
  const [c2, p] = await terminal({ viewport: { width: 1440, height: 900 } });
  await p.getByRole("button", { name: "Collapse the sidebar" }).click(); await p.getByRole("button", { name: "Expand the sidebar" }).waitFor();
  const app = p.locator("aside").getByRole("navigation", { name: "Mode" }).getByRole("link", { name: "App" }); ok((await app.boundingBox()).height >= 43.5, "icon target too small");
  await app.click(); await p.waitForURL(/\/dashboard/); await p.goto(`${BASE}/terminal`); await p.getByRole("button", { name: "Expand the sidebar" }).click(); await c2.close();
});

await browser.close();
mock.close();
const failed = results.filter(([p]) => !p);
for (const [p, n] of results) console.log(`${p ? "PASS" : "FAIL"}  ${n}`);
console.log(`\n${results.length - failed.length}/${results.length} passed`);
process.exit(failed.length ? 1 : 0);
