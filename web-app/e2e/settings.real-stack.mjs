// End-to-end test of Settings against the REAL local stack (no mocks):
// browser -> web app (/api/preferences, /api/auth/change-password) -> api-gateway -> user-service -> MySQL.
//
// Needs running: docker compose up -d (mysql, kafka), user-service (8081), api-gateway (8080) and the web app on
// BASE_URL (default http://localhost:3000, started WITHOUT API_GATEWAY_URL so it uses the gateway on 8080).
//   npm run e2e:settings:real
//
// It CREATES one user (e2e-set-<timestamp>@example.com) and a row in user_preferences in your local database.
// Delete them afterwards: DELETE FROM user_preferences WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'e2e-set-%');
// then DELETE FROM users WHERE email LIKE 'e2e-set-%@example.com';
import { chromium } from "playwright";

const BASE = process.env.BASE_URL ?? "http://localhost:3000";
const stamp = Date.now();
const email = `e2e-set-${stamp}@example.com`;
const PW1 = "FirstPassword123";
const PW2 = "SecondPassword456";
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
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const browser = await chromium.launch();
const newCtx = () => browser.newContext({ viewport: { width: 1280, height: 900 } });

async function login(page, password) {
  await page.goto(`${BASE}/login`);
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Login" }).click();
}

const ca = await newCtx(); // device A: registers, changes the password
const pa = await ca.newPage();
let cb, pb; // device B: logged in before the change

await test("register and finish onboarding through the real stack", async () => {
  await pa.goto(`${BASE}/register`);
  await pa.getByLabel("First name").fill("Asha");
  await pa.getByLabel("Last name").fill("Rao");
  await pa.getByLabel("Email", { exact: true }).fill(email);
  await pa.getByLabel("Password", { exact: true }).fill(PW1);
  await pa.getByLabel("Confirm password").fill(PW1);
  await pa.getByLabel("Country").selectOption("IN");
  await pa.getByLabel("Mobile number").fill("9876543210");
  await pa.getByLabel("Female", { exact: true }).check();
  await pa.getByRole("button", { name: "Read Terms & Risk Disclaimer" }).click();
  await pa.getByRole("region", { name: /Terms and risk disclaimer text/ }).evaluate((el) => el.scrollTo(0, el.scrollHeight));
  await pa.getByRole("button", { name: "I have read and accept" }).click();
  await pa.getByRole("button", { name: "Create Account" }).click();
  await pa.waitForURL("**/onboarding", { timeout: 20000 });
  ok((await ca.request.put(`${BASE}/api/onboarding`, { headers: JSON_HEADERS, data: { tradingExperience: "ADVANCED", brokerPreferences: ["ZERODHA"], plan: "FREE" } })).ok(), "save");
  ok((await ca.request.post(`${BASE}/api/onboarding/complete`, { headers: JSON_HEADERS, data: {} })).ok(), "complete");
});

await test("a new user has the defaults: system theme, in-app on, push and email off, CSV", async () => {
  const r = await ca.request.get(`${BASE}/api/preferences`);
  ok(r.status() === 200, `status ${r.status()}`);
  const p = await r.json();
  ok(p.theme === "SYSTEM" && p.language === "en" && p.notifyInApp === true && p.notifyPush === false && p.notifyEmail === false && p.exportFormat === "CSV", JSON.stringify(p));
});

await test("settings saved in the UI persist in MySQL and follow the user to a new browser", async () => {
  await pa.goto(`${BASE}/settings/general`);
  await pa.getByLabel("Dark").check();
  await pa.getByText("Saved", { exact: true }).waitFor({ timeout: 15000 });
  await pa.goto(`${BASE}/settings/notifications`);
  await pa.getByRole("switch", { name: "Email alerts" }).click();
  await pa.getByText("Saved", { exact: true }).waitFor({ timeout: 15000 });
  await pa.goto(`${BASE}/settings/downloads`);
  await pa.getByLabel("JSON").check();
  await pa.getByText("Saved", { exact: true }).waitFor({ timeout: 15000 });
  // Device B: a brand-new browser with no localStorage. Logging in must bring the dark theme and the other choices.
  cb = await newCtx(); pb = await cb.newPage();
  await login(pb, PW1); await pb.waitForURL("**/dashboard", { timeout: 20000 });
  await pb.waitForFunction(() => document.documentElement.dataset.theme === "dark", null, { timeout: 15000 });
  const p = await (await cb.request.get(`${BASE}/api/preferences`)).json();
  ok(p.theme === "DARK" && p.notifyEmail === true && p.exportFormat === "JSON" && p.notifyPush === false, JSON.stringify(p));
});

await test("the server refuses bad values and ignores a user id in the body (it always edits the caller)", async () => {
  const put = (data) => ca.request.put(`${BASE}/api/preferences`, { headers: JSON_HEADERS, data });
  ok((await put({ theme: "NEON" })).status() === 400, "theme");
  ok((await put({ exportFormat: "EXCEL" })).status() === 400, "excel");
  const r = await put({ notifyPush: true, userId: "someone-else" });
  ok(r.status() === 200 && (await r.json()).notifyPush === true, "valid save with extra field");
  // The same value straight to the gateway with a bad theme (bypassing the web app's own check) is refused by user-service.
  const access = (await ca.cookies()).find((c) => c.name === "qv_access")?.value;
  const direct = await fetch("http://localhost:8080/api/v1/users/me/preferences", { method: "PUT", headers: { "Content-Type": "application/json", Authorization: `Bearer ${access}` }, body: JSON.stringify({ theme: "NEON" }) });
  ok(direct.status === 400, `user-service accepted a bad theme: ${direct.status}`);
});

await test("change password: a wrong current password is refused and the session survives", async () => {
  await pa.goto(`${BASE}/settings/security`);
  await pa.getByLabel("Current password").fill("NotMyPassword999");
  await pa.getByLabel("New password", { exact: true }).fill(PW2);
  await pa.getByLabel("Confirm new password").fill(PW2);
  await pa.getByRole("button", { name: "Change Password" }).click();
  await pa.getByText("Your current password is incorrect").first().waitFor({ timeout: 15000 });
  await pa.goto(`${BASE}/dashboard`);
  await pa.getByText(/Hello, Asha|Welcome to Quantiva, Asha/).first().waitFor({ timeout: 15000 });
});

await test("change password: this device stays signed in (even after its access cookie expires) and the other device is signed out", async () => {
  await sleep(1200); // refresh tokens are compared with the change time in whole seconds: make sure device B's login is older
  await pa.goto(`${BASE}/settings/security`);
  await pa.getByLabel("Current password").fill(PW1);
  await pa.getByLabel("New password", { exact: true }).fill(PW2);
  await pa.getByLabel("Confirm new password").fill(PW2);
  await pa.getByRole("button", { name: "Change Password" }).click();
  await pa.getByText("Your password has been changed").waitFor({ timeout: 15000 });
  // Device A: drop the access cookie so only the NEW refresh token can keep the session alive.
  await ca.clearCookies({ name: "qv_access" });
  await pa.goto(`${BASE}/dashboard`);
  await pa.getByText(/Hello, Asha|Welcome to Quantiva, Asha/).first().waitFor({ timeout: 15000 });
  // Device B: its refresh token is older than the change, so it can no longer be renewed.
  await cb.clearCookies({ name: "qv_access" });
  await pb.goto(`${BASE}/dashboard`);
  await pb.waitForURL("**/login**", { timeout: 20000 });
});

await test("the old password no longer logs in; the new one does", async () => {
  const c = await newCtx(); const p = await c.newPage();
  await login(p, PW1);
  await p.getByText("The email or password is incorrect").first().waitFor({ timeout: 15000 });
  await login(p, PW2);
  await p.waitForURL("**/dashboard", { timeout: 20000 });
  await c.close();
});

await test("a signed-out request to the settings routes is 401", async () => {
  const anon = await browser.newContext();
  ok((await anon.request.get(`${BASE}/api/preferences`)).status() === 401, "preferences");
  ok((await anon.request.post(`${BASE}/api/auth/change-password`, { headers: JSON_HEADERS, data: { currentPassword: PW1, newPassword: PW2, confirmPassword: PW2 } })).status() === 401, "change-password");
  await anon.close();
});

await ca.close();
await cb?.close();
await browser.close();
const failed = results.filter(([pass]) => !pass);
for (const [pass, name] of results) console.log(`${pass ? "PASS" : "FAIL"}  ${name}`);
console.log(`\n${results.length - failed.length}/${results.length} passed  (test user: ${email})`);
process.exit(failed.length ? 1 : 0);
