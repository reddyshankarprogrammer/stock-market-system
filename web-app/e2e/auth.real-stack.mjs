// End-to-end test of the authentication journey against the REAL local stack (no mocks):
// web app -> api-gateway -> user-service -> MySQL, and the reset email through Mailpit.
//
// Needs running: docker compose up -d (mysql, kafka, mailpit), user-service (8081), api-gateway (8080) and the web app
// on BASE_URL (default http://localhost:3000, started WITHOUT API_GATEWAY_URL so it uses the gateway on 8080).
//   npm run e2e:auth:real
//
// It CREATES test users in your local database (emails e2e-<timestamp>@example.com) and sends mail to Mailpit.
// Delete them afterwards:  DELETE FROM users WHERE email LIKE 'e2e-%@example.com';  (and their rows in password_reset_tokens).
import { chromium } from "playwright";

const BASE = process.env.BASE_URL ?? "http://localhost:3000";
const MAILPIT = process.env.MAILPIT_URL ?? "http://localhost:8025";
const stamp = Date.now();
const email = `e2e-${stamp}@example.com`;
const unknown = `e2e-nobody-${stamp}@example.com`;
const PW1 = "FirstPassword123";
const PW2 = "SecondPassword456";

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

async function mailTo(address, { wait = 15000 } = {}) {
  const end = Date.now() + wait;
  while (Date.now() < end) {
    const list = await (await fetch(`${MAILPIT}/api/v1/search?query=${encodeURIComponent(`to:${address}`)}`)).json();
    if (list.messages?.length) {
      const msg = await (await fetch(`${MAILPIT}/api/v1/message/${list.messages[0].ID}`)).json();
      return { count: list.messages.length, subject: msg.Subject, text: msg.Text, from: msg.From?.Address };
    }
    await new Promise((r) => setTimeout(r, 500));
  }
  return null;
}

const browser = await chromium.launch();
const newCtx = () => browser.newContext({ viewport: { width: 1280, height: 900 } });

async function login(page, mail, password) {
  await page.goto(`${BASE}/login`);
  await page.getByLabel("Email", { exact: true }).fill(mail);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Login" }).click();
}

const c = await newCtx();
const p = await c.newPage();

await test("register through the real gateway and user-service, with every profile field", async () => {
  await p.goto(`${BASE}/register`);
  await p.getByLabel("First name").fill("Asha");
  await p.getByLabel("Last name").fill("Rao");
  await p.getByLabel("Email", { exact: true }).fill(email);
  await p.getByLabel("Password", { exact: true }).fill(PW1);
  await p.getByLabel("Confirm password").fill(PW1);
  await p.getByLabel("Country").selectOption("IN");
  await p.getByLabel("Mobile number").fill("9876543210");
  await p.getByLabel("Female", { exact: true }).check();
  await p.getByRole("button", { name: "Read Terms & Risk Disclaimer" }).click();
  await p.getByRole("region", { name: /Terms and risk disclaimer text/ }).evaluate((el) => el.scrollTo(0, el.scrollHeight));
  await p.getByRole("button", { name: "I have read and accept" }).click();
  await p.getByRole("button", { name: "Create Account" }).click();
  await p.waitForURL("**/dashboard?welcome=1", { timeout: 20000 });
  await p.getByText("Welcome to Quantiva, Asha").waitFor();
  ok((await p.getByText("Some details were not saved yet").count()) === 0, "unexpected 'not saved' notice");
});

await test("the session cookies are httpOnly and the tokens are not visible to page scripts", async () => {
  const names = (await c.cookies()).filter((x) => x.name.startsWith("qv_"));
  ok(names.length === 2 && names.every((x) => x.httpOnly), `cookies: ${JSON.stringify(names.map((x) => [x.name, x.httpOnly]))}`);
  ok(!(await p.evaluate(() => document.cookie)).includes("qv_"), "cookie visible to scripts");
});

await test("registering the same email again is refused", async () => {
  const c2 = await newCtx(); const p2 = await c2.newPage();
  await p2.goto(`${BASE}/register`);
  await p2.getByLabel("First name").fill("Asha"); await p2.getByLabel("Last name").fill("Rao");
  await p2.getByLabel("Email", { exact: true }).fill(email);
  await p2.getByLabel("Password", { exact: true }).fill(PW1); await p2.getByLabel("Confirm password").fill(PW1);
  await p2.getByLabel("Country").selectOption("IN"); await p2.getByLabel("Mobile number").fill("9876543210"); await p2.getByLabel("Female", { exact: true }).check();
  await p2.getByRole("button", { name: "Read Terms & Risk Disclaimer" }).click();
  await p2.getByRole("region", { name: /Terms and risk disclaimer text/ }).evaluate((el) => el.scrollTo(0, el.scrollHeight));
  await p2.getByRole("button", { name: "I have read and accept" }).click();
  await p2.getByRole("button", { name: "Create Account" }).click();
  await p2.getByText(/already exists/i).waitFor({ timeout: 15000 }); await c2.close();
});

await test("the session renews itself through the real refresh endpoint", async () => {
  const before = (await c.cookies()).find((x) => x.name === "qv_access").value;
  await c.clearCookies({ name: "qv_access" }); // what the browser does when the 15-minute cookie expires
  await p.goto(`${BASE}/dashboard`);
  await p.getByText("Hello, Asha").waitFor({ timeout: 15000 });
  const after = (await c.cookies()).find((x) => x.name === "qv_access");
  ok(after?.httpOnly && after.value !== before, "no new access cookie after renewal");
});

await test("logout, then login with the password", async () => {
  await p.getByRole("button", { name: "Log out" }).click(); await p.waitForURL("**/login");
  ok(!(await c.cookies()).some((x) => x.name.startsWith("qv_")), "cookies not cleared");
  await login(p, email, PW1); await p.waitForURL("**/dashboard", { timeout: 15000 }); await p.getByText("Hello, Asha").waitFor();
  await p.getByRole("button", { name: "Log out" }).click(); await p.waitForURL("**/login");
});

await test("a wrong password and an unknown email give the same answer (real 401)", async () => {
  const c3 = await newCtx(); const p3 = await c3.newPage();
  await login(p3, email, "WrongPassword999"); await p3.locator("form [role=alert]").waitFor({ timeout: 15000 });
  const a = await p3.locator("form [role=alert]").innerText();
  await login(p3, unknown, "WrongPassword999"); await p3.locator("form [role=alert]").waitFor({ timeout: 15000 });
  const b = await p3.locator("form [role=alert]").innerText();
  ok(/email or password is incorrect/i.test(a) && a === b, `"${a}" vs "${b}"`);
  const direct = await fetch("http://localhost:8080/api/v1/users/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email: unknown, password: "WrongPassword999" }) });
  ok(direct.status === 401, `gateway answered ${direct.status} for an unknown email (expected 401)`); await c3.close();
});

// A second browser logged in with the first password and kept open: it must stop working after the reset.
const oldCtx = await newCtx();
const oldPage = await oldCtx.newPage();
await test("start a second session with the first password (kept for the reset check)", async () => {
  await login(oldPage, email, PW1); await oldPage.waitForURL("**/dashboard", { timeout: 15000 }); await oldPage.getByText("Hello, Asha").waitFor();
  await new Promise((r) => setTimeout(r, 1500)); // refresh tokens carry second-precision issue times: make sure the reset happens in a later second
});

await test("forgot password: a real email arrives in Mailpit with a working link", async () => {
  await p.goto(`${BASE}/forgot-password`);
  await p.getByLabel("Email", { exact: true }).fill(email); await p.getByRole("button", { name: "Send Reset Link" }).click();
  await p.getByText("Check your email").waitFor({ timeout: 15000 });
  const mail = await mailTo(email);
  ok(mail, "no email arrived in Mailpit within 15 s");
  ok(/Reset your Quantiva password/.test(mail.subject), `subject: ${mail.subject}`);
  ok(mail.from === "no-reply@quantiva.local", `from: ${mail.from}`);
  const link = mail.text.match(/http:\/\/localhost:3000\/reset-password\?token=[A-Za-z0-9_-]+/);
  ok(link, "no reset link in the email body");
  globalThis.resetLink = link[0];
});

await test("forgot password for an unknown email shows the same screen and sends nothing", async () => {
  await p.goto(`${BASE}/forgot-password`);
  await p.getByLabel("Email", { exact: true }).fill(unknown); await p.getByRole("button", { name: "Send Reset Link" }).click();
  await p.getByText("Check your email").waitFor({ timeout: 15000 });
  ok((await mailTo(unknown, { wait: 3000 })) === null, "an email was sent to an unregistered address");
});

await test("asking again within the cooldown sends no second email", async () => {
  await p.goto(`${BASE}/forgot-password`);
  await p.getByLabel("Email", { exact: true }).fill(email); await p.getByRole("button", { name: "Send Reset Link" }).click();
  await p.getByText("Check your email").waitFor({ timeout: 15000 });
  await new Promise((r) => setTimeout(r, 2500));
  ok((await mailTo(email, { wait: 1000 })).count === 1, "a second email was sent inside the 60-second cooldown");
});

await test("reset: the emailed link changes the password; the old one stops working, the new one works", async () => {
  const c4 = await newCtx(); const p4 = await c4.newPage();
  await p4.goto(globalThis.resetLink); await p4.getByRole("heading", { name: "Choose a new password" }).waitFor();
  await p4.waitForFunction(() => !location.search.includes("token")); // token removed from the address bar
  await p4.getByLabel("New password", { exact: true }).fill(PW2); await p4.getByLabel("Confirm new password").fill(PW2);
  await p4.getByRole("button", { name: "Change Password" }).click();
  await p4.getByText("Your password has been changed").first().waitFor({ timeout: 15000 });
  await login(p4, email, PW1); await p4.locator("form [role=alert]").waitFor({ timeout: 15000 });
  ok(/email or password is incorrect/i.test(await p4.locator("form [role=alert]").innerText()), "old password still works");
  await login(p4, email, PW2); await p4.waitForURL("**/dashboard", { timeout: 15000 }); await p4.getByText("Hello, Asha").waitFor(); await c4.close();
});

await test("the same reset link cannot be used a second time", async () => {
  const c5 = await newCtx(); const p5 = await c5.newPage();
  await p5.goto(globalThis.resetLink);
  await p5.getByLabel("New password", { exact: true }).fill("ThirdPassword789"); await p5.getByLabel("Confirm new password").fill("ThirdPassword789");
  await p5.getByRole("button", { name: "Change Password" }).click();
  await p5.getByText("This reset link is invalid or has expired").first().waitFor({ timeout: 15000 }); await c5.close();
});

await test("a session that started BEFORE the reset can no longer be renewed (the old refresh token is refused)", async () => {
  await oldCtx.clearCookies({ name: "qv_access" }); // expire the 15-minute cookie: the only way back in is the refresh token
  await oldPage.goto(`${BASE}/dashboard`);
  await oldPage.waitForURL("**/login**", { timeout: 15000 });
  ok(!(await oldCtx.cookies()).some((x) => x.name.startsWith("qv_")), "cookies of the ended session were not cleared");
});

await test("a session that started AFTER the reset renews normally", async () => {
  const c6 = await newCtx(); const p6 = await c6.newPage();
  await login(p6, email, PW2); await p6.waitForURL("**/dashboard", { timeout: 15000 });
  await c6.clearCookies({ name: "qv_access" }); await p6.goto(`${BASE}/dashboard`); await p6.getByText("Hello, Asha").waitFor({ timeout: 15000 }); await c6.close();
});

await oldCtx.close();
await c.close();
await browser.close();
const failed = results.filter(([pass]) => !pass);
for (const [pass, name] of results) console.log(`${pass ? "PASS" : "FAIL"}  ${name}`);
console.log(`\n${results.length - failed.length}/${results.length} passed  (test user: ${email})`);
process.exit(failed.length ? 1 : 0);
