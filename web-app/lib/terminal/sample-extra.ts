// More SAMPLE DATA for the terminal pages (markets, portfolio, positions, orders, journal). Like sample-data.ts it is
// generated from fixed seeds, is not a real account, and every page that shows it says so.
import { AS_OF, INSTRUMENTS, TRADES, addDays, type Trade } from "./sample-data.ts";

const r2 = (n: number) => Math.round(n * 100) / 100;

function rng(seed: number) {
  return () => {
    seed |= 0; seed = (seed + 0x6d2b79f5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

// ---------- markets ----------
export type Quote = {
  symbol: string; name: string; sector: string; market: string; exchange: string; assetClass: string;
  price: number; prevClose: number; change: number; changePercent: number; dayLow: number; dayHigh: number; volume: number;
  low52: number; high52: number; series: number[];
};

export type IndexQuote = { symbol: string; name: string; price: number; changePercent: number; series: number[] };

function walk(rand: () => number, end: number, points: number, vol: number): number[] {
  // a random walk that is built backwards from today's price, so the last point is the price
  const out = [end];
  for (let i = 1; i < points; i++) out.unshift(r2(out[0] / (1 + (rand() - 0.5) * vol)));
  return out;
}

export function generateQuotes(seed = 7001): Quote[] {
  const rand = rng(seed);
  return INSTRUMENTS.map((i) => {
    const price = r2(i.price * (0.97 + rand() * 0.06));
    const changePercent = r2((rand() - 0.47) * 4);
    const prevClose = r2(price / (1 + changePercent / 100));
    const span = Math.abs(price - prevClose) + price * (0.004 + rand() * 0.01);
    const series = walk(rand, price, 30, 0.04);
    return {
      symbol: i.symbol, name: i.company, sector: i.sector, market: i.market, exchange: i.exchange, assetClass: i.assetClass,
      price, prevClose, change: r2(price - prevClose), changePercent,
      dayLow: r2(Math.min(price, prevClose) - span * 0.4), dayHigh: r2(Math.max(price, prevClose) + span * 0.4),
      volume: Math.round((0.4 + rand() * 9) * 1_000_000), low52: r2(Math.min(...series) * 0.8), high52: r2(Math.max(...series) * 1.15), series,
    };
  });
}

export function generateIndices(seed = 7002): IndexQuote[] {
  const rand = rng(seed);
  return [
    { symbol: "SPX", name: "S&P 500", base: 5600 }, { symbol: "NDX", name: "Nasdaq 100", base: 19600 },
    { symbol: "NIFTY", name: "Nifty 50", base: 24800 }, { symbol: "SENSEX", name: "BSE Sensex", base: 81500 },
  ].map((x) => {
    const price = r2(x.base * (0.99 + rand() * 0.02));
    return { symbol: x.symbol, name: x.name, price, changePercent: r2((rand() - 0.45) * 2.2), series: walk(rand, price, 30, 0.02) };
  });
}

export const QUOTES: Quote[] = generateQuotes();
export const INDICES: IndexQuote[] = generateIndices();

export const topMovers = (quotes: Quote[], n = 3) => {
  const sorted = [...quotes].sort((a, b) => b.changePercent - a.changePercent);
  return { gainers: sorted.slice(0, n), losers: sorted.slice(-n).reverse() };
};

// ---------- portfolio and positions (from the open sample trades, so they agree with the dashboard) ----------
export type Holding = { symbol: string; company: string; sector: string; market: string; quantity: number; avgCost: number; price: number; value: number; invested: number; pnl: number; pnlPercent: number; weight: number };

export function holdingsOf(trades: Trade[]): Holding[] {
  const by = new Map<string, Trade[]>();
  for (const t of trades) if (t.status === "Open") by.set(t.symbol, [...(by.get(t.symbol) ?? []), t]);
  const rows = [...by.entries()].map(([symbol, ts]) => {
    const quantity = ts.reduce((s, t) => s + t.quantity, 0);
    const invested = r2(ts.reduce((s, t) => s + t.buyPrice * t.quantity, 0));
    const value = r2(ts.reduce((s, t) => s + t.markPrice * t.quantity, 0));
    return { symbol, company: ts[0].company, sector: ts[0].sector, market: ts[0].market, quantity, avgCost: r2(invested / quantity), price: r2(value / quantity), value, invested, pnl: r2(value - invested), pnlPercent: invested ? r2(((value - invested) / invested) * 100) : 0, weight: 0 };
  });
  const total = rows.reduce((s, h) => s + h.value, 0);
  return rows.map((h) => ({ ...h, weight: total ? r2((h.value / total) * 100) : 0 })).sort((a, b) => b.value - a.value);
}

export type Position = Trade & { stop: number; target: number; riskReward: number; distanceToStop: number; unrealized: number };

export function positionsOf(trades: Trade[]): Position[] {
  return trades.filter((t) => t.status === "Open").map((t) => {
    const stop = r2(t.buyPrice * (t.risk === "High" ? 0.88 : t.risk === "Medium" ? 0.92 : 0.95));
    const target = r2(t.buyPrice * (t.risk === "High" ? 1.25 : t.risk === "Medium" ? 1.18 : 1.12));
    return { ...t, stop, target, riskReward: r2((target - t.buyPrice) / (t.buyPrice - stop)), distanceToStop: r2(((t.markPrice - stop) / t.markPrice) * 100), unrealized: t.pnl };
  });
}

// ---------- orders ----------
export type OrderStatus = "Filled" | "Partially filled" | "Open" | "Cancelled" | "Rejected";
export type OrderType = "Market" | "Limit" | "Stop";
export type Order = {
  id: string; time: string; symbol: string; side: "Buy" | "Sell"; type: OrderType; quantity: number; limitPrice: number | null;
  status: OrderStatus; filledQuantity: number; avgFill: number | null; note: string;
};

export function generateOrders(count = 64, seed = 7003): Order[] {
  const rand = rng(seed);
  const orders: Order[] = [];
  for (let n = 0; n < count; n++) {
    const inst = INSTRUMENTS[Math.floor(rand() * INSTRUMENTS.length)];
    const type: OrderType = rand() < 0.5 ? "Market" : rand() < 0.75 ? "Limit" : "Stop";
    const quantity = Math.max(1, Math.round((3000 + rand() * 20000) / inst.price));
    const age = rand() < 0.15 ? 0 : Math.floor(rand() * 60);
    const time = `${addDays(AS_OF, -age)}T${String(age === 0 ? 8 : 9 + Math.floor(rand() * 7)).padStart(2, "0")}:${String(Math.floor(rand() * 60)).padStart(2, "0")}:00`;
    const roll = rand();
    const status: OrderStatus = age === 0 && roll < 0.5 ? "Open" : roll < 0.7 ? "Filled" : roll < 0.8 ? "Partially filled" : roll < 0.9 ? "Cancelled" : "Rejected";
    const limitPrice = type === "Market" ? null : r2(inst.price * (type === "Limit" ? 0.97 + rand() * 0.04 : 0.93 + rand() * 0.04));
    const filledQuantity = status === "Filled" ? quantity : status === "Partially filled" ? Math.max(1, Math.floor(quantity * (0.2 + rand() * 0.6))) : 0;
    orders.push({
      id: `O${String(n + 1).padStart(5, "0")}`, time, symbol: inst.symbol, side: rand() < 0.55 ? "Buy" : "Sell", type, quantity, limitPrice, status, filledQuantity,
      avgFill: filledQuantity ? r2((limitPrice ?? inst.price) * (0.998 + rand() * 0.004)) : null,
      note: status === "Rejected" ? (rand() < 0.5 ? "Insufficient buying power" : "Price outside the allowed band") : "",
    });
  }
  return orders.sort((a, b) => b.time.localeCompare(a.time));
}

export const ORDERS: Order[] = generateOrders();
export const ORDER_STATUSES: OrderStatus[] = ["Open", "Partially filled", "Filled", "Cancelled", "Rejected"];

export type TicketInput = { symbol: string; side: "Buy" | "Sell"; type: OrderType; quantity: number; limitPrice: number | null };

/** A new order from the ticket. It is only added to the sample list; it never reaches a broker or an account. */
export function newOrder(input: TicketInput, now: string, seq: number): Order {
  return { id: `N${String(seq).padStart(5, "0")}`, time: now, symbol: input.symbol, side: input.side, type: input.type, quantity: input.quantity, limitPrice: input.type === "Market" ? null : input.limitPrice, status: "Open", filledQuantity: 0, avgFill: null, note: "Sample order: not sent anywhere" };
}

// ---------- journal ----------
export const EMOTIONS = ["Calm", "Confident", "Hesitant", "FOMO", "Fearful"] as const;
export type Emotion = (typeof EMOTIONS)[number];
export type JournalEntry = { id: string; date: string; symbol: string; strategy: string; emotion: Emotion; rating: number; notes: string; lesson: string; pnl: number | null };

const WIN_NOTES = ["Waited for the setup and held to the plan.", "Entry was early but the stop was respected.", "Scaled out near the target as planned."];
const LOSS_NOTES = ["Entered before the confirmation candle.", "Moved the stop and gave back the gain.", "Position was larger than the plan allowed."];
const LESSONS = ["Wait for the close above resistance.", "Write the exit before the entry.", "Size by the stop distance, not by conviction.", "Do not trade the first ten minutes."];

export function generateJournal(trades: Trade[] = TRADES, seed = 7004): JournalEntry[] {
  const rand = rng(seed);
  return trades.filter((t) => t.status === "Closed" && t.exitDate).slice(0, 40).map((t, n) => {
    const win = t.pnl > 0;
    const emotion = EMOTIONS[Math.floor(rand() * EMOTIONS.length)];
    const pool = win ? WIN_NOTES : LOSS_NOTES;
    return { id: `J${String(n + 1).padStart(4, "0")}`, date: t.exitDate as string, symbol: t.symbol, strategy: t.strategy, emotion, rating: win ? 3 + Math.floor(rand() * 3) : 1 + Math.floor(rand() * 3), notes: pool[Math.floor(rand() * pool.length)], lesson: LESSONS[Math.floor(rand() * LESSONS.length)], pnl: t.pnl };
  }).sort((a, b) => b.date.localeCompare(a.date));
}

export const JOURNAL: JournalEntry[] = generateJournal();

/** Profit and win rate by emotion tag, over the entries that have a result. */
export function journalByEmotion(entries: JournalEntry[]): { emotion: Emotion; count: number; pnl: number; winRate: number }[] {
  return EMOTIONS.map((emotion) => {
    const list = entries.filter((e) => e.emotion === emotion && e.pnl !== null);
    const wins = list.filter((e) => (e.pnl as number) > 0).length;
    return { emotion, count: list.length, pnl: r2(list.reduce((s, e) => s + (e.pnl as number), 0)), winRate: list.length ? r2((wins / list.length) * 100) : 0 };
  });
}
