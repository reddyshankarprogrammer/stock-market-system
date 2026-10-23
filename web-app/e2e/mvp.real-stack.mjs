// MVP walk-through against the REAL local stack (no mocks): a trader opens a tracking account, imports a Zerodha
// statement, reviews the month on the dashboard, filters the trades, and trades at the market price on a paper account.
// Every figure on screen is compared with what account-service itself returns.
//
// Needs running: docker compose up -d (mysql, redis, kafka), user-service (8081), account-service (8086),
// market-data-service (8082), api-gateway (8080) and the web app on BASE_URL (default http://localhost:3000, started
// WITHOUT API_GATEWAY_URL so it uses the gateway on 8080).
//   npm run e2e:mvp:real
//
// It CREATES one user (e2e-mvp-<timestamp>@example.com), two accounts, one statement (and its stored file under
// account-service/uploads/statements) and a few trades. Delete them afterwards (see docs/changelog.md, "cleanup").
import { chromium } from "playwright";

const BASE = process.env.BASE_URL ?? "http://localhost:3000";
const stamp = Date.now();
const email = `e2e-mvp-${stamp}@example.com`;
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

// The same formatting the app uses, so a figure can be compared as text.
const fmt = (n, currency) => new Intl.NumberFormat("en", { style: "currency", currency }).format(n);
const signed = (n, currency) => (n > 0 ? `+${fmt(Math.abs(n), currency)}` : n < 0 ? `-${fmt(Math.abs(n), currency)}` : fmt(0, currency));
const round2 = (n) => Math.round(n * 100) / 100;

const CSV = [
  "symbol,isin,trade_date,exchange,segment,series,trade_type,auction,quantity,price,trade_id,order_id,order_execution_time",
  "INFY,INE009A01021,2026-09-25,NSE,EQ,EQ,buy,false,10,1500.50,5001,9001,2026-09-25T09:20:11",
  "INFY,INE009A01021,2026-09-30,NSE,EQ,EQ,sell,false,4,1550,5002,9002,2026-09-30T10:01:00",
  "TCS,INE467B01029,2026-10-01,NSE,EQ,EQ,buy,false,5,3600,5003,9003,2026-10-01T11:00:00",
  "INFY,INE009A01021,2026-10-03,NSE,EQ,EQ,sell,false,3,1600,5004,9004,2026-10-03T10:30:00",
  "WIPRO,INE075A01022,2026-10-03,NSE,EQ,EQ,hold,false,1,250,5005,9005,2026-10-03T11:30:00",
].join("\n") + "\n";

const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
const p = await ctx.newPage();
const api = (path) => ctx.request.get(`${BASE}/api/gw${path}`).then((r) => r.json());
let zerodhaId, paperId, trades, performance, summary;

await test("register and finish onboarding", async () => {
  await p.goto(`${BASE}/register`);
  await p.getByLabel("First name").fill("Asha");
  await p.getByLabel("Last name").fill("Rao");
  await p.getByLabel("Email", { exact: true }).fill(email);
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
  ok((await ctx.request.put(`${BASE}/api/onboarding`, { headers: JSON_HEADERS, data: { tradingExperience: "ADVANCED", brokerPreferences: ["ZERODHA"], plan: "FREE" } })).ok(), "save");
  ok((await ctx.request.post(`${BASE}/api/onboarding/complete`, { headers: JSON_HEADERS, data: {} })).ok(), "complete");
});

await test("open a Zerodha tracking account in the UI and land on its page", async () => {
  await p.goto(`${BASE}/accounts`);
  await p.getByText("No accounts yet").waitFor({ timeout: 20000 });
  await p.getByRole("button", { name: "Open an account" }).click();
  await p.getByLabel("Account name").fill("E2E Zerodha");
  await p.getByLabel("Held at").selectOption("ZERODHA");
  await p.getByLabel("Currency").selectOption("INR");
  await p.getByRole("button", { name: "Open account" }).click();
  await p.waitForURL(/\/accounts\/[0-9a-f-]{36}$/, { timeout: 20000 });
  zerodhaId = p.url().split("/").pop();
  await p.getByRole("heading", { name: "E2E Zerodha", level: 1 }).waitFor();
  await p.getByLabel("Upload a Zerodha statement").waitFor();
});

await test("a Zerodha tradebook imports through the real parser; the page reports what was imported and skipped", async () => {
  await p.getByLabel("Upload a Zerodha statement").setInputFiles({ name: "tradebook.csv", mimeType: "text/csv", buffer: Buffer.from(CSV) });
  await p.getByText("Statement imported").waitFor({ timeout: 40000 });
  await p.getByText(/4 trades imported, 0 already known, 1 rows skipped/).waitFor();
  trades = await api(`/api/v1/trades/account/${zerodhaId}`);
  ok(trades.length === 4, `API has ${trades.length} trades`);
  // the same file again is recognised
  await p.getByLabel("Upload a Zerodha statement").setInputFiles({ name: "tradebook.csv", mimeType: "text/csv", buffer: Buffer.from(CSV) });
  await p.getByText(/already uploaded|already imported|same file/i).first().waitFor({ timeout: 20000 });
  ok((await api(`/api/v1/trades/account/${zerodhaId}`)).length === 4, "duplicate upload added trades");
});

await test("Trades page: the table, the side filter and the date filter match what the API returns", async () => {
  await p.goto(`${BASE}/trades?account=${zerodhaId}`);
  await p.getByRole("region", { name: "Trades table" }).waitFor({ timeout: 20000 });
  ok((await p.locator("tbody tr").count()) === trades.length, "row count");
  const sells = trades.filter((t) => t.side === "SELL");
  await p.getByLabel("Side").selectOption("SELL");
  await p.getByText(new RegExp(`${sells.length} trades match your filters`)).waitFor({ timeout: 8000 }).catch(async (e) => { throw new Error(`${String(e.message).slice(0, 80)} page says: ${(await p.locator("[role=status]").allInnerTexts()).join(" / ")}`); });
  ok((await p.locator("tbody tr").count()) === sells.length, "sell rows");
  const realized = round2(sells.reduce((s, t) => s + (t.realizedProfit ?? 0), 0));
  await p.getByText(signed(realized, "INR"), { exact: true }).first().waitFor();
  await p.getByLabel("Side").selectOption("");
  // One end only: account-service refuses it (400), so the page filters the list itself and must still agree with the API.
  await p.getByLabel("From").fill("2026-10-01");
  const oneEnded = trades.filter((t) => t.tradeDate.slice(0, 10) >= "2026-10-01");
  await p.getByText(new RegExp(`${oneEnded.length} trades match your filters`)).waitFor();
  ok((await p.locator("tbody tr").count()) === oneEnded.length, "one-sided date filter");
  // Both ends: sent to account-service; the rows must equal its answer.
  await p.getByLabel("To").fill("2026-10-02");
  const fromApi = await api(`/api/v1/trades/account/${zerodhaId}?from=2026-10-01&to=2026-10-02`);
  ok(Array.isArray(fromApi), `API answer: ${JSON.stringify(fromApi)}`);
  await p.getByText(new RegExp(`${fromApi.length} trades? match(es)? your filters`)).waitFor();
  ok((await p.locator("tbody tr").count()) === fromApi.length && fromApi.length === 1, `date filter rows vs API ${fromApi.length}`);
});

await test("Trade detail shows the API's figures", async () => {
  const sell = trades.find((t) => t.side === "SELL" && t.tradeDate.startsWith("2026-10-03"));
  await p.goto(`${BASE}/trades/${sell.id}`);
  await p.getByRole("heading", { name: "Sell INFY", level: 1 }).waitFor({ timeout: 20000 });
  await p.locator("dd", { hasText: signed(sell.realizedProfit ?? 0, "INR") }).first().waitFor();
  await p.locator("dd", { hasText: "Imported from a statement" }).waitFor();
});

await test("Dashboard: key figures equal the account's summary and performance; every calendar day equals its trades", async () => {
  summary = await api(`/api/v1/accounts/${zerodhaId}/summary`);
  performance = await api(`/api/v1/accounts/${zerodhaId}/performance`);
  await p.goto(`${BASE}/dashboard?account=${zerodhaId}`);
  await p.getByRole("heading", { name: "Key figures" }).waitFor({ timeout: 20000 });
  for (const text of [signed(performance.realizedProfit, "INR"), signed(performance.netProfit, "INR"), fmt(summary.totalEquity, "INR")]) await p.getByText(text, { exact: true }).first().waitFor();
  await p.getByRole("status").filter({ hasText: "October 2026" }).waitFor();
  const byDay = {};
  for (const t of trades) byDay[t.tradeDate.slice(0, 10)] = round2((byDay[t.tradeDate.slice(0, 10)] ?? 0) + (t.realizedProfit ?? 0));
  const cells = await p.locator("td[data-day]").evaluateAll((els) => els.map((e) => [Number(e.getAttribute("data-day")), e.getAttribute("aria-label")]));
  const octDays = Object.entries(byDay).filter(([d]) => d.startsWith("2026-10"));
  ok(cells.length === octDays.length, `calendar shows ${cells.length} days, API has ${octDays.length}`);
  ok(JSON.stringify(cells.map((c) => c[0]).sort()) === JSON.stringify(octDays.map(([, v]) => v).sort()), `day values ${JSON.stringify(cells)} vs ${JSON.stringify(octDays)}`);
  await p.getByRole("button", { name: "Previous month" }).click();
  await p.getByRole("status").filter({ hasText: "September 2026" }).waitFor();
  const all = round2(Object.values(byDay).reduce((s, v) => s + v, 0));
  ok(all === round2(performance.realizedProfit), `calendar total ${all} vs performance ${performance.realizedProfit}`);
  ok((await p.getByTestId("all-time").innerText()) === signed(all, "INR"), "all-time text");
  ok((await p.getByText(/the account reports/).count()) === 0, "calendar and account disagree");
});

await test("Portfolio lists the open positions the API reports", async () => {
  const holdings = await api(`/api/v1/holdings/account/${zerodhaId}`);
  await p.goto(`${BASE}/portfolio?account=${zerodhaId}`);
  if (holdings.length === 0) return void (await p.getByText("No open positions").waitFor({ timeout: 20000 }));
  await p.getByRole("region", { name: "Holdings table" }).waitFor({ timeout: 20000 });
  ok((await p.locator("tbody tr").count()) === holdings.length, "row count");
  for (const h of holdings) await p.locator("tbody tr").filter({ hasText: h.symbol }).waitFor();
});

await test("paper account: deposit, buy at the market price, and the portfolio shows the position with the API's price", async () => {
  const opened = await ctx.request.post(`${BASE}/api/gw/api/v1/accounts`, { headers: JSON_HEADERS, data: { accountName: "E2E Paper", accountType: "INDIVIDUAL", brokerName: "PAPER", currency: "USD" } });
  ok(opened.status() === 201, `open ${opened.status()}`);
  paperId = (await opened.json()).id;
  const dep = await ctx.request.post(`${BASE}/api/gw/api/v1/accounts/${paperId}/deposit`, { headers: JSON_HEADERS, data: { amount: 10000, reference: "e2e" } });
  ok(dep.ok(), `deposit ${dep.status()}`);
  await p.goto(`${BASE}/trades?account=${paperId}`);
  await p.getByRole("button", { name: "Record a trade" }).click();
  const form = p.getByRole("form", { name: "Record a trade" });
  await form.getByLabel("Symbol").fill("AAPL");
  await form.getByLabel("Shares").fill("5");
  await form.getByRole("button", { name: "Place trade" }).click();
  await p.getByRole("cell", { name: "AAPL", exact: true }).waitFor({ timeout: 30000 });
  await p.goto(`${BASE}/portfolio?account=${paperId}`);
  await p.getByRole("region", { name: "Holdings table" }).waitFor({ timeout: 20000 });
  let matched = false;
  for (let i = 0; i < 4 && !matched; i++) { // the simulated price can move between the two reads: try again
    await p.getByRole("button", { name: "Refresh prices" }).click();
    await p.getByRole("button", { name: "Refresh prices" }).waitFor();
    const list = await api(`/api/v1/holdings/account/${paperId}`);
    ok(Array.isArray(list), `holdings answer: ${JSON.stringify(list)}`);
    const h = list.find((x) => x.symbol === "AAPL");
    matched = (await p.locator("tbody tr").filter({ hasText: "AAPL" }).innerText()).includes(fmt(h.currentPrice, "USD"));
    if (!matched) await p.waitForTimeout(700);
  }
  ok(matched, "the price on screen never matched the holdings API");
  const row = await p.locator("tbody tr").filter({ hasText: "AAPL" }).innerText();
  ok(/\b5\b/.test(row), `quantity 5 missing: ${row}`);
  const s = await api(`/api/v1/accounts/${paperId}/summary`);
  ok(s.pricesLive === true, "market-data prices should be live");
});

await test("no screen overflows sideways on a 360 px phone", async () => {
  const phone = await browser.newContext({ viewport: { width: 360, height: 800 } });
  await phone.addCookies(await ctx.cookies());
  const q = await phone.newPage();
  for (const path of ["/dashboard", "/accounts", `/accounts/${zerodhaId}`, "/trades", `/trades/${trades[0].id}`, "/portfolio", "/settings/general"]) {
    await q.goto(`${BASE}${path}`);
    await q.getByRole("heading", { level: 1 }).first().waitFor({ timeout: 20000 });
    await q.waitForLoadState("networkidle");
    ok(!(await q.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)), `${path} overflows`);
  }
  await phone.close();
});

await ctx.close();
await browser.close();
const failed = results.filter(([pass]) => !pass);
for (const [pass, name] of results) console.log(`${pass ? "PASS" : "FAIL"}  ${name}`);
console.log(`\n${results.length - failed.length}/${results.length} passed  (test user: ${email}; accounts ${zerodhaId}, ${paperId})`);
process.exit(failed.length ? 1 : 0);
