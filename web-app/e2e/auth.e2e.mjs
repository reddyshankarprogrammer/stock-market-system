// End-to-end test of the authentication pages against a mock gateway (no database or Kafka needed).
//   npm run build
//   $env:API_GATEWAY_URL="http://localhost:8099"; npx next start -p 3100      # terminal 1
//   npm run e2e:auth                                                             # terminal 2
// It checks behaviour in a real browser: validation, error states, cookies, protected routes, terms gating, theme, layout.
import { chromium } from "playwright";
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
    for (const [name, path] of [["Trades", "/trades"], ["Portfolio", "/portfolio"], ["Settings", "/settings"], ["Dashboard", "/dashboard"]]) {
      const link = p.locator('nav[aria-label="Main"]:visible').getByRole("link", { name });
      const box = await link.boundingBox(); ok(box && box.height >= 43.5, `${label} ${name} target ${box?.height}`);
      await link.click(); await p.waitForURL(new RegExp(`${path}(\\?|$)`));
      ok((await link.getAttribute("aria-current")) === "page", `${label} ${name} not marked current`);
      await p.getByRole("heading", { level: 1 }).first().waitFor();
    }
    await c.close();
  }
});
await test("shell: page skeletons say plainly that the screen is not built yet", async () => {
  const c = await newCtx(); const p = await c.newPage(); await login(p); await p.waitForURL(/\/dashboard/);
  await p.goto(`${BASE}/trades`); await p.getByRole("heading", { name: "Trades" }).waitFor(); await p.getByText(/planned/).waitFor(); await c.close();
});

await browser.close();
mock.close();
const failed = results.filter(([p]) => !p);
for (const [p, n] of results) console.log(`${p ? "PASS" : "FAIL"}  ${n}`);
console.log(`\n${results.length - failed.length}/${results.length} passed`);
process.exit(failed.length ? 1 : 0);
