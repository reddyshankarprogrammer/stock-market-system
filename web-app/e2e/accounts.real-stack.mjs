// End-to-end test of the typed account API through the REAL local stack (no mocks):
// browser -> web app (/api/gw BFF) -> api-gateway -> account-service -> MySQL, with users from user-service.
//
// Needs running: docker compose up -d (mysql, kafka), user-service (8081), account-service (8086), api-gateway (8080)
// and the web app on BASE_URL (default http://localhost:3000, started WITHOUT API_GATEWAY_URL so it uses the gateway).
//   npm run e2e:accounts:real
//
// It CREATES two users (e2e-acc-<timestamp>-a|b@example.com) and their accounts in your local databases.
// Delete them afterwards: users by e-mail pattern in user-service, accounts by user_id in account-service.
import { chromium } from "playwright";

const BASE = process.env.BASE_URL ?? "http://localhost:3000";
const stamp = Date.now();
const emailA = `e2e-acc-${stamp}-a@example.com`;
const emailB = `e2e-acc-${stamp}-b@example.com`;
const PW = "FirstPassword123";
const JSON_HEADERS = { "Content-Type": "application/json", Origin: new URL(BASE).origin };

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

const browser = await chromium.launch();
const newCtx = () => browser.newContext({ viewport: { width: 1280, height: 900 } });

/** Register through the real form (it is the only way to get a valid session), then finish onboarding through the BFF. */
async function registerAndOnboard(ctx, mail) {
  const p = await ctx.newPage();
  await p.goto(`${BASE}/register`);
  await p.getByLabel("First name").fill("Asha");
  await p.getByLabel("Last name").fill("Rao");
  await p.getByLabel("Email", { exact: true }).fill(mail);
  await p.getByLabel("Password", { exact: true }).fill(PW);
  await p.getByLabel("Confirm password").fill(PW);
  await p.getByLabel("Country").selectOption("IN");
  await p.getByLabel("Mobile number").fill("9876543210");
  await p.getByLabel("Female", { exact: true }).check();
  await p.getByRole("button", { name: "Read Terms & Risk Disclaimer" }).click();
  await p.getByRole("region", { name: /Terms and risk disclaimer text/ }).evaluate((el) => el.scrollTo(0, el.scrollHeight));
  await p.getByRole("button", { name: "I have read and accept" }).click();
  await p.getByRole("button", { name: "Create Account" }).click();
  await p.waitForURL("**/onboarding", { timeout: 20000 });
  const save = await ctx.request.put(`${BASE}/api/onboarding`, { headers: JSON_HEADERS, data: { tradingExperience: "ADVANCED", brokerPreferences: ["ZERODHA"], plan: "FREE" } });
  ok(save.ok(), `onboarding save ${save.status()}`);
  const done = await ctx.request.post(`${BASE}/api/onboarding/complete`, { headers: JSON_HEADERS, data: {} });
  ok(done.ok(), `onboarding complete ${done.status()}`);
  return p;
}

const api = (ctx) => ({
  get: (path) => ctx.request.get(`${BASE}/api/gw${path}`),
  post: (path, data, headers = JSON_HEADERS) => ctx.request.post(`${BASE}/api/gw${path}`, { headers, data }),
});

const ca = await newCtx();
const cb = await newCtx();
let pa, userIdA, userIdB, platformId;

await test("two users register through the real stack", async () => {
  pa = await registerAndOnboard(ca, emailA);
  await registerAndOnboard(cb, emailB);
  userIdA = (await (await ca.request.get(`${BASE}/api/auth/me`)).json()).user.userId;
  userIdB = (await (await cb.request.get(`${BASE}/api/auth/me`)).json()).user.userId;
  ok(userIdA && userIdB && userIdA !== userIdB, "ids");
});

await test("/accounts for a new user shows the empty state, not an error", async () => {
  await pa.goto(`${BASE}/accounts`);
  await pa.getByRole("heading", { name: "Accounts" }).waitFor({ timeout: 15000 });
  await pa.getByText("No accounts yet").waitFor({ timeout: 15000 });
});

await test("typed POST through the BFF creates accounts; /accounts lists them with balances and the tracking note", async () => {
  const platform = await api(ca).post("/api/v1/accounts", { accountName: "E2E Platform", accountType: "INDIVIDUAL", brokerName: "PLATFORM", currency: "USD" });
  ok(platform.status() === 201, `platform ${platform.status()} ${await platform.text()}`);
  platformId = (await platform.json()).id;
  const tracking = await api(ca).post("/api/v1/accounts", { accountName: "E2E Zerodha", accountType: "EQUITY", brokerName: "ZERODHA", currency: "INR" });
  ok(tracking.status() === 201, `tracking ${tracking.status()}`);
  await pa.goto(`${BASE}/accounts`);
  await pa.getByRole("heading", { name: "E2E Platform", level: 2 }).waitFor({ timeout: 15000 });
  await pa.getByRole("heading", { name: "E2E Zerodha", level: 2 }).waitFor();
  ok((await pa.getByText("Tracking only: no cash moves here").count()) === 1, "tracking note");
  ok((await pa.getByText("Cash balance").count()) === 1, "cash note");
  ok((await pa.getByText("$0.00").count()) >= 1 && (await pa.getByText("₹0.00").count()) >= 1, "currency formatting");
});

await test("the BFF reaches only the account paths: user-service, traversal and unknown paths are refused", async () => {
  ok((await api(ca).get("/api/v1/users/me")).status() === 404, "users path reachable");
  // "%2e%2e" is normalised to ".." by the server's URL parser and lands outside the allowed prefix (404); an encoded slash is refused (400).
  ok((await api(ca).get("/api/v1/accounts/%2e%2e/users/me")).status() === 404, "dot-dot traversal not refused");
  ok((await api(ca).get("/api/v1/accounts/%2e%2e%2fusers/me")).status() === 400, "encoded-slash traversal not refused");
  ok((await api(ca).get("/actuator/health")).status() === 404, "actuator reachable");
  ok((await api(ca).get("/api/v1/market-data/prices")).status() === 404, "market-data reachable");
});

await test("another user's accounts are refused by account-service (403) and nothing leaks", async () => {
  const r = await api(cb).get(`/api/v1/accounts/user/${userIdA}`);
  ok(r.status() === 403, `list of someone else: ${r.status()}`);
  ok(!(await r.text()).includes("E2E Platform"), "body leaked an account");
  const one = await api(cb).get(`/api/v1/accounts/${platformId}`);
  ok(one.status() === 403 || one.status() === 404, `single account of someone else: ${one.status()}`);
  const own = await api(cb).get(`/api/v1/accounts/user/${userIdB}`);
  ok(own.status() === 200 && (await own.json()).length === 0, "user B should have no accounts");
});

await test("a request from another origin is blocked (403); a signed-out request is 401", async () => {
  const evil = await api(ca).post("/api/v1/accounts", { accountName: "x", accountType: "INDIVIDUAL", brokerName: "PAPER", currency: "USD" }, { ...JSON_HEADERS, Origin: "https://evil.example" });
  ok(evil.status() === 403, `cross-origin: ${evil.status()}`);
  const anon = await browser.newContext();
  const r = await anon.request.get(`${BASE}/api/gw/api/v1/accounts/user/${userIdA}`);
  ok(r.status() === 401, `signed out: ${r.status()}`);
  await anon.close();
});

await test("an expired access cookie is renewed through the proxy (accounts still load, a new cookie is issued)", async () => {
  await ca.clearCookies({ name: "qv_access" });
  const r = await api(ca).get(`/api/v1/accounts/user/${userIdA}`);
  ok(r.status() === 200 && (await r.json()).length === 2, `after renewal: ${r.status()}`);
  ok((await ca.cookies()).some((c) => c.name === "qv_access"), "no new access cookie");
  await ca.clearCookies({ name: "qv_access" });
  await pa.goto(`${BASE}/accounts`);
  await pa.getByRole("heading", { name: "E2E Platform", level: 2 }).waitFor({ timeout: 15000 });
});

await test("a session that is really over sends the page to /login", async () => {
  const c = await newCtx();
  await c.addCookies([{ name: "qv_refresh", value: "garbage.value.here", url: BASE, httpOnly: true }]);
  const p = await c.newPage();
  await p.goto(`${BASE}/accounts`); // the proxy lets a refresh cookie through; the page then finds the session over
  await p.waitForURL("**/login**", { timeout: 20000 });
  await c.close();
});

await ca.close();
await cb.close();
await browser.close();
const failed = results.filter(([pass]) => !pass);
for (const [pass, name] of results) console.log(`${pass ? "PASS" : "FAIL"}  ${name}`);
console.log(`\n${results.length - failed.length}/${results.length} passed  (test users: ${emailA}, ${emailB}; user ids ${userIdA}, ${userIdB})`);
process.exit(failed.length ? 1 : 0);
