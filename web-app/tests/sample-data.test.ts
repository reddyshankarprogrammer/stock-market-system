import test from "node:test";
import assert from "node:assert/strict";
import { AS_OF, BASE_CAPITAL, NO_FILTERS, TRADES, allocation, applyFilters, computeKpis, equityCurve, generateTrades, monthlyReturns, sectorPerformance, tradesToCsv, winLoss } from "../lib/terminal/sample-data.ts";

const r2 = (n: number) => Math.round(n * 100) / 100;

test("the sample is deterministic and has both open and closed trades", () => {
  assert.deepEqual(generateTrades(), TRADES);
  assert.ok(TRADES.length >= 90);
  assert.ok(TRADES.some((t) => t.status === "Open") && TRADES.some((t) => t.status === "Closed"));
  assert.ok(TRADES.every((t) => (t.status === "Open") === (t.exitDate === null) && (t.status === "Open") === (t.sellPrice === null)));
  assert.ok(TRADES.every((t) => t.entryDate <= AS_OF && (t.exitDate === null || (t.exitDate >= t.entryDate && t.exitDate <= AS_OF))));
});

test("profit and ROI follow from the prices", () => {
  for (const t of TRADES) {
    assert.equal(t.pnl, r2((t.markPrice - t.buyPrice) * t.quantity));
    assert.equal(t.roi, r2(((t.markPrice - t.buyPrice) / t.buyPrice) * 100));
  }
});

test("the equity curve ends at base capital plus the closed profit and the open profit, and sector profit adds up to total profit", () => {
  const curve = equityCurve(TRADES);
  assert.equal(curve.length, 366);
  const closed = TRADES.filter((t) => t.status === "Closed").reduce((s, t) => s + t.pnl, 0);
  const open = TRADES.filter((t) => t.status === "Open").reduce((s, t) => s + t.pnl, 0); // by AS_OF every open trade has been open for exactly holdingDays
  assert.equal(r2(curve.at(-1)!.equity - BASE_CAPITAL), r2(closed + open));
  assert.equal(r2(sectorPerformance(TRADES).reduce((s, x) => s + x.pnl, 0)), r2(TRADES.reduce((s, t) => s + t.pnl, 0)));
});

test("monthly returns cover 12 months and add up to the closed profit of those months", () => {
  const months = monthlyReturns(TRADES);
  assert.equal(months.length, 12);
  assert.equal(months.at(-1)!.month, "2026-10");
  const inWindow = TRADES.filter((t) => t.status === "Closed" && t.exitDate && t.exitDate.slice(0, 7) >= months[0].month);
  assert.equal(r2(months.reduce((s, m) => s + m.pnl, 0)), r2(inWindow.reduce((s, t) => s + t.pnl, 0)));
});

test("win rate is wins over closed trades; allocation adds up to 100%", () => {
  const wl = winLoss(TRADES);
  assert.equal(wl.wins + wl.losses, wl.closed);
  assert.equal(wl.winRate, r2((wl.wins / wl.closed) * 100));
  for (const key of ["sector", "assetClass", "region"] as const) assert.ok(Math.abs(allocation(TRADES, key).reduce((s, a) => s + a.percent, 0) - 100) < 0.1);
});

test("seven KPIs, open positions equals the open trades, and every card has a 30 point sparkline", () => {
  const kpis = computeKpis(TRADES);
  assert.deepEqual(kpis.map((k) => k.key), ["value", "today", "month", "winrate", "open", "risk", "sharpe"]);
  assert.equal(kpis.find((k) => k.key === "open")!.value, TRADES.filter((t) => t.status === "Open").length);
  for (const k of kpis) assert.equal(k.spark.length, 30, k.key);
});

test("filters narrow the list and the KPIs follow the filtered trades", () => {
  const momentum = applyFilters(TRADES, { ...NO_FILTERS, strategy: "Momentum" });
  assert.ok(momentum.length > 0 && momentum.length < TRADES.length && momentum.every((t) => t.strategy === "Momentum"));
  assert.equal(computeKpis(momentum).find((k) => k.key === "open")!.value, momentum.filter((t) => t.status === "Open").length);
  const recent = applyFilters(TRADES, { ...NO_FILTERS, range: "30d" });
  assert.ok(recent.every((t) => t.entryDate >= "2026-09-24"));
  assert.ok(applyFilters(TRADES, { ...NO_FILTERS, search: "nvidia" }).every((t) => t.symbol === "NVDA"));
  assert.deepEqual(applyFilters(TRADES, { ...NO_FILTERS, market: "India", exchange: "NASDAQ" }), []);
});

test("CSV has a header, one line per trade, quotes cells that need it and neutralises formulas", () => {
  const lines = tradesToCsv(TRADES.slice(0, 3)).trim().split("\r\n");
  assert.equal(lines.length, 4);
  assert.ok(lines[0].startsWith("Status,Entry date,Exit date,Symbol"));
  const out = tradesToCsv([{ ...TRADES[0], company: '=HYPERLINK("http://x","click")', strategy: "A, B" }]);
  assert.ok(out.includes(`"'=HYPERLINK(""http://x"",""click"")"`), out);
  assert.ok(out.includes('"A, B"'));
  assert.ok(tradesToCsv([{ ...TRADES[0], pnl: -125.5 }]).includes(",-125.5,"), "a negative number is not a formula");
});
