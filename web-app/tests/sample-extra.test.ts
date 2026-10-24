import test from "node:test";
import assert from "node:assert/strict";
import { TRADES } from "../lib/terminal/sample-data.ts";
import { INDICES, JOURNAL, ORDERS, QUOTES, generateOrders, generateQuotes, holdingsOf, journalByEmotion, newOrder, positionsOf, topMovers } from "../lib/terminal/sample-extra.ts";

const r2 = (n: number) => Math.round(n * 100) / 100;

test("quotes are deterministic; the series ends at the price; the day range contains the price and the previous close", () => {
  assert.deepEqual(generateQuotes(), QUOTES);
  assert.equal(QUOTES.length, 16);
  for (const q of QUOTES) {
    assert.equal(q.series.length, 30);
    assert.equal(q.series.at(-1), q.price);
    assert.ok(q.dayLow <= Math.min(q.price, q.prevClose) && q.dayHigh >= Math.max(q.price, q.prevClose), q.symbol);
    assert.ok(Math.abs(q.change - r2(q.price - q.prevClose)) < 0.011, q.symbol);
  }
  assert.equal(INDICES.length, 4);
});

test("gainers are the best movers and losers the worst", () => {
  const { gainers, losers } = topMovers(QUOTES, 3);
  const sorted = [...QUOTES].map((q) => q.changePercent).sort((a, b) => b - a);
  assert.deepEqual(gainers.map((q) => q.changePercent), sorted.slice(0, 3));
  assert.deepEqual(losers.map((q) => q.changePercent), sorted.slice(-3).reverse());
});

test("holdings add up to the open trades and the weights to 100", () => {
  const open = TRADES.filter((t) => t.status === "Open");
  const h = holdingsOf(TRADES);
  assert.equal(h.reduce((s, x) => s + x.quantity, 0), open.reduce((s, t) => s + t.quantity, 0));
  assert.equal(r2(h.reduce((s, x) => s + x.pnl, 0)), r2(open.reduce((s, t) => s + t.pnl, 0)));
  assert.ok(Math.abs(h.reduce((s, x) => s + x.weight, 0) - 100) < 0.2);
  assert.equal(new Set(h.map((x) => x.symbol)).size, h.length);
});

test("positions are the open trades, with a stop below and a target above the entry", () => {
  const p = positionsOf(TRADES);
  assert.equal(p.length, TRADES.filter((t) => t.status === "Open").length);
  for (const x of p) assert.ok(x.stop < x.buyPrice && x.target > x.buyPrice && x.riskReward > 0, x.id);
});

test("orders: fills agree with the status", () => {
  assert.deepEqual(generateOrders(), ORDERS);
  for (const o of ORDERS) {
    if (o.status === "Filled") assert.equal(o.filledQuantity, o.quantity);
    if (o.status === "Partially filled") assert.ok(o.filledQuantity > 0 && o.filledQuantity < o.quantity);
    if (["Open", "Cancelled", "Rejected"].includes(o.status)) assert.equal(o.filledQuantity, 0);
    assert.equal(o.avgFill === null, o.filledQuantity === 0);
    assert.equal(o.limitPrice === null, o.type === "Market");
    assert.equal(o.note !== "", o.status === "Rejected");
  }
  assert.ok(new Set(ORDERS.map((o) => o.status)).size === 5, "every status appears in the sample");
});

test("a ticket order starts open, unfilled and says it is only a sample", () => {
  const o = newOrder({ symbol: "AAPL", side: "Buy", type: "Limit", quantity: 5, limitPrice: 190 }, "2026-10-24T10:00:00", 1);
  assert.equal(o.status, "Open"); assert.equal(o.filledQuantity, 0); assert.equal(o.limitPrice, 190); assert.ok(o.note.includes("Sample"));
  assert.equal(newOrder({ symbol: "AAPL", side: "Sell", type: "Market", quantity: 1, limitPrice: 5 }, "t", 2).limitPrice, null);
});

test("the journal has one entry per closed trade it covers, with the trade's own result", () => {
  assert.ok(JOURNAL.length > 0 && JOURNAL.length <= 40);
  for (const e of JOURNAL) {
    assert.ok(e.rating >= 1 && e.rating <= 5);
    const trade = TRADES.find((t) => t.symbol === e.symbol && t.exitDate === e.date && t.pnl === e.pnl);
    assert.ok(trade, `${e.id} has no matching trade`);
  }
  const byEmotion = journalByEmotion(JOURNAL);
  assert.equal(byEmotion.reduce((s, x) => s + x.count, 0), JOURNAL.length);
  assert.equal(r2(byEmotion.reduce((s, x) => s + x.pnl, 0)), r2(JOURNAL.reduce((s, e) => s + (e.pnl ?? 0), 0)));
});
