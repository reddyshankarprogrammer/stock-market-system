// End-to-end test of the authentication pages against a mock gateway (no database or Kafka needed).
//   npm run build
//   $env:API_GATEWAY_URL="http://localhost:8099"; npx next start -p 3100      # terminal 1
//   npm run e2e:auth                                                             # terminal 2
// It checks behaviour in a real browser: validation, error states, cookies, protected routes, terms gating, theme, layout.
import { chromium } from "playwright";
import { startMockGateway } from "./mock-gateway.mjs";

const BASE = process.env.BASE_URL ?? "http://localhost:3100";
const results = [];
const test = async (name, fn) => {
  try {
    await fn();
    results.push([true, name]);
  } catch (e) {
    results.push([false, `${name}: ${String(e.message).split("\n")[0]}`]);
  }
};
const ok = (cond, msg) => {
  if (!cond) throw new Error(msg);
};

const mock = await startMockGateway(8099);
const browser = await chromium.launch();
const newCtx = (o = {}) => browser.newContext({ viewport: { width: 1280, height: 900 }, ...o });

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
await test("register success: dashboard with 'not saved yet' notice", async () => {
  const c = await newCtx(); const p = await c.newPage(); await fillRegister(p); await acceptTerms(p);
  await p.getByRole("button", { name: "Create Account" }).click(); await p.waitForURL("**/dashboard?welcome=1");
  await p.getByText("Welcome to Quantiva, Asha").waitFor(); await p.getByText("Some details were not saved yet").waitFor();
  const t = await p.getByRole("status").filter({ hasText: "not saved yet" }).innerText(); ok(/mobile number/.test(t) && /gender/.test(t) && /country/.test(t), t); await c.close();
});

// ---- pending screens ----
await test("pending pages are honest about what is not available", async () => {
  const c = await newCtx(); const p = await c.newPage();
  for (const path of ["forgot-password", "reset-password", "verify-email"]) { await p.goto(`${BASE}/${path}`); await p.getByText("Not available yet", { exact: true }).waitFor(); }
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

await browser.close();
mock.close();
const failed = results.filter(([p]) => !p);
for (const [p, n] of results) console.log(`${p ? "PASS" : "FAIL"}  ${n}`);
console.log(`\n${results.length - failed.length}/${results.length} passed`);
process.exit(failed.length ? 1 : 0);
