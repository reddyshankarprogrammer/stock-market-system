// SAMPLE DATA for the trading terminal screens. None of this is a real account: it is generated from a fixed seed so
// the screens (and their tests) always show the same numbers, and every screen that uses it says "Sample data".
// Nothing here imports from "@/..." so the pure functions can be tested with `node --test`.

export const AS_OF = "2026-10-24"; // the day the sample data "ends"
export const BASE_CAPITAL = 250_000;

export type Status = "Open" | "Closed";
export type Risk = "Low" | "Medium" | "High";

export type Instrument = { symbol: string; company: string; sector: string; market: string; exchange: string; assetClass: string; region: string; price: number };

export const INSTRUMENTS: Instrument[] = [
  { symbol: "AAPL", company: "Apple Inc.", sector: "Technology", market: "US", exchange: "NASDAQ", assetClass: "Equity", region: "North America", price: 195 },
  { symbol: "MSFT", company: "Microsoft Corporation", sector: "Technology", market: "US", exchange: "NASDAQ", assetClass: "Equity", region: "North America", price: 410 },
  { symbol: "NVDA", company: "NVIDIA Corporation", sector: "Technology", market: "US", exchange: "NASDAQ", assetClass: "Equity", region: "North America", price: 120 },
  { symbol: "AMZN", company: "Amazon.com Inc.", sector: "Consumer", market: "US", exchange: "NASDAQ", assetClass: "Equity", region: "North America", price: 185 },
  { symbol: "JPM", company: "JPMorgan Chase & Co.", sector: "Financials", market: "US", exchange: "NYSE", assetClass: "Equity", region: "North America", price: 205 },
  { symbol: "XOM", company: "Exxon Mobil Corporation", sector: "Energy", market: "US", exchange: "NYSE", assetClass: "Equity", region: "North America", price: 112 },
  { symbol: "UNH", company: "UnitedHealth Group", sector: "Healthcare", market: "US", exchange: "NYSE", assetClass: "Equity", region: "North America", price: 520 },
  { symbol: "SPY", company: "SPDR S&P 500 ETF", sector: "Broad market", market: "US", exchange: "NYSE", assetClass: "ETF", region: "North America", price: 540 },
  { symbol: "INFY", company: "Infosys Ltd.", sector: "Technology", market: "India", exchange: "NSE", assetClass: "Equity", region: "Asia", price: 1600 },
  { symbol: "TCS", company: "Tata Consultancy Services", sector: "Technology", market: "India", exchange: "NSE", assetClass: "Equity", region: "Asia", price: 3700 },
  { symbol: "HDFCBANK", company: "HDFC Bank Ltd.", sector: "Financials", market: "India", exchange: "NSE", assetClass: "Equity", region: "Asia", price: 1650 },
  { symbol: "RELIANCE", company: "Reliance Industries", sector: "Energy", market: "India", exchange: "BSE", assetClass: "Equity", region: "Asia", price: 2900 },
  { symbol: "SUNPHARMA", company: "Sun Pharmaceutical", sector: "Healthcare", market: "India", exchange: "BSE", assetClass: "Equity", region: "Asia", price: 1700 },
  { symbol: "NIFTYBEES", company: "Nippon India Nifty 50 ETF", sector: "Broad market", market: "India", exchange: "NSE", assetClass: "ETF", region: "Asia", price: 270 },
  { symbol: "ASML", company: "ASML Holding", sector: "Technology", market: "Europe", exchange: "NASDAQ", assetClass: "Equity", region: "Europe", price: 900 },
  { symbol: "NVO", company: "Novo Nordisk", sector: "Healthcare", market: "Europe", exchange: "NYSE", assetClass: "Equity", region: "Europe", price: 130 },
];

export const STRATEGIES = ["Momentum", "Mean reversion", "Breakout", "Swing", "Value"] as const;
export const PORTFOLIOS = ["Core", "Growth"] as const;
export const SECTORS = [...new Set(INSTRUMENTS.map((i) => i.sector))];

export type Trade = {
  id: string; status: Status; entryDate: string; exitDate: string | null; symbol: string; company: string; sector: string; market: string;
  exchange: string; assetClass: string; region: string; strategy: string; portfolio: string; risk: Risk;
  buyPrice: number; sellPrice: number | null; markPrice: number; quantity: number; pnl: number; roi: number; holdingDays: number; dayPnl: number;
};

// mulberry32: a tiny seeded generator, so the sample never changes between runs
function rng(seed: number) {
  return () => {
    seed |= 0; seed = (seed + 0x6d2b79f5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const r2 = (n: number) => Math.round(n * 100) / 100;
const dayMs = 86_400_000;
const asDate = (isoDate: string) => new Date(isoDate + "T00:00:00Z");
export const iso = (d: Date) => d.toISOString().slice(0, 10);
export const addDays = (isoDate: string, days: number) => iso(new Date(asDate(isoDate).getTime() + days * dayMs));
export const daysBetween = (a: string, b: string) => Math.round((asDate(b).getTime() - asDate(a).getTime()) / dayMs);

export function generateTrades(count = 96, seed = 20261024): Trade[] {
  const rand = rng(seed);
  const trades: Trade[] = [];
  for (let n = 0; n < count; n++) {
    const inst = INSTRUMENTS[Math.floor(rand() * INSTRUMENTS.length)];
    const strategy = STRATEGIES[Math.floor(rand() * STRATEGIES.length)];
    const portfolio = PORTFOLIOS[rand() < 0.6 ? 0 : 1];
    const risk: Risk = rand() < 0.35 ? "Low" : rand() < 0.7 ? "Medium" : "High";
    const age = Math.floor(rand() * 420) + 1; // entered up to 14 months before AS_OF
    const entryDate = addDays(AS_OF, -age);
    const isOpen = age < 25 ? rand() < 0.8 : rand() < 0.04;
    const buyPrice = r2(inst.price * (0.9 + rand() * 0.2));
    const drift = (rand() - 0.42) * 0.22; // slightly more winners than losers
    const quantity = Math.max(1, Math.round((6000 + rand() * 24000) / buyPrice));
    const holdingDays = isOpen ? age : Math.max(1, Math.min(age - 1, Math.floor(2 + rand() * 40)));
    const exitDate = isOpen ? null : addDays(entryDate, holdingDays);
    const markPrice = r2(buyPrice * (1 + drift)); // the exit price of a closed trade, the current price of an open one
    trades.push({
      id: `T${String(n + 1).padStart(4, "0")}`, status: isOpen ? "Open" : "Closed", entryDate, exitDate, symbol: inst.symbol, company: inst.company, sector: inst.sector,
      market: inst.market, exchange: inst.exchange, assetClass: inst.assetClass, region: inst.region, strategy, portfolio, risk, buyPrice,
      sellPrice: isOpen ? null : markPrice, markPrice, quantity, pnl: r2((markPrice - buyPrice) * quantity), roi: r2(((markPrice - buyPrice) / buyPrice) * 100), holdingDays,
      dayPnl: isOpen ? r2(quantity * markPrice * (rand() - 0.46) * 0.012) : 0,
    });
  }
  return trades.sort((a, b) => b.entryDate.localeCompare(a.entryDate));
}

export const TRADES: Trade[] = generateTrades();

// ---------- filters ----------
export type Filters = { asset: string; market: string; exchange: string; portfolio: string; strategy: string; risk: string; range: string; search: string };
export const NO_FILTERS: Filters = { asset: "", market: "", exchange: "", portfolio: "", strategy: "", risk: "", range: "all", search: "" };
export const RANGES: { value: string; label: string; days: number | null }[] = [
  { value: "30d", label: "Last 30 days", days: 30 },
  { value: "90d", label: "Last 90 days", days: 90 },
  { value: "180d", label: "Last 6 months", days: 180 },
  { value: "365d", label: "Last 12 months", days: 365 },
  { value: "all", label: "All time", days: null },
];

export function applyFilters(trades: Trade[], f: Filters): Trade[] {
  const range = RANGES.find((r) => r.value === f.range)?.days ?? null;
  const since = range === null ? null : addDays(AS_OF, -range);
  const q = f.search.trim().toLowerCase();
  return trades.filter(
    (t) =>
      (!f.asset || t.assetClass === f.asset) && (!f.market || t.market === f.market) && (!f.exchange || t.exchange === f.exchange) &&
      (!f.portfolio || t.portfolio === f.portfolio) && (!f.strategy || t.strategy === f.strategy) && (!f.risk || t.risk === f.risk) &&
      // a trade belongs to a date range by the day it was opened
      (since === null || t.entryDate >= since) && (!q || t.symbol.toLowerCase().includes(q) || t.company.toLowerCase().includes(q) || t.strategy.toLowerCase().includes(q)),
  );
}

// ---------- figures ----------
export type Kpi = { key: string; label: string; value: number; display: string; trend: number; spark: number[]; hint: string };

const money = (n: number) => new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 }).format(n);
const signedMoney = (n: number) => (n > 0 ? "+" : n < 0 ? "-" : "") + money(Math.abs(n));

/** Equity per day, oldest first: base capital plus closed profit up to that day plus the open trades' profit, building up from their entry day. */
export function equityCurve(trades: Trade[], days = 365): { date: string; equity: number }[] {
  const points: { date: string; equity: number }[] = [];
  for (let i = days; i >= 0; i--) {
    const date = addDays(AS_OF, -i);
    let equity = BASE_CAPITAL;
    for (const t of trades) {
      if (t.status === "Closed" && t.exitDate !== null && t.exitDate <= date) equity += t.pnl;
      else if (t.status === "Open" && t.entryDate <= date) equity += t.pnl * Math.min(1, daysBetween(t.entryDate, date) / Math.max(1, t.holdingDays));
    }
    points.push({ date, equity: r2(equity) });
  }
  return points;
}

export function monthlyReturns(trades: Trade[]): { month: string; pnl: number }[] {
  const by = new Map<string, number>();
  for (let m = 11; m >= 0; m--) by.set(new Date(Date.UTC(2026, 9 - m, 1)).toISOString().slice(0, 7), 0);
  for (const t of trades) {
    if (t.status === "Closed" && t.exitDate) {
      const k = t.exitDate.slice(0, 7);
      if (by.has(k)) by.set(k, r2((by.get(k) ?? 0) + t.pnl));
    }
  }
  return [...by.entries()].map(([month, pnl]) => ({ month, pnl }));
}

export function winLoss(trades: Trade[]) {
  const closed = trades.filter((t) => t.status === "Closed");
  const wins = closed.filter((t) => t.pnl > 0).length;
  return { wins, losses: closed.length - wins, closed: closed.length, winRate: closed.length ? r2((wins / closed.length) * 100) : 0 };
}

export function sectorPerformance(trades: Trade[]): { sector: string; pnl: number }[] {
  const by = new Map<string, number>();
  for (const t of trades) by.set(t.sector, r2((by.get(t.sector) ?? 0) + t.pnl));
  return [...by.entries()].map(([sector, pnl]) => ({ sector, pnl })).sort((a, b) => b.pnl - a.pnl);
}

/** Share of the invested value of the OPEN trades by a field. The percentages add up to 100. */
export function allocation(trades: Trade[], key: "sector" | "assetClass" | "region"): { name: string; value: number; percent: number }[] {
  const by = new Map<string, number>();
  for (const t of trades) if (t.status === "Open") by.set(t[key], r2((by.get(t[key]) ?? 0) + t.markPrice * t.quantity));
  const total = [...by.values()].reduce((s, v) => s + v, 0);
  return [...by.entries()].map(([name, value]) => ({ name, value, percent: total ? r2((value / total) * 100) : 0 })).sort((a, b) => b.value - a.value);
}

function sharpe(returns: number[]): number {
  if (returns.length < 2) return 0;
  const mean = returns.reduce((s, v) => s + v, 0) / returns.length;
  const sd = Math.sqrt(returns.reduce((s, v) => s + (v - mean) ** 2, 0) / (returns.length - 1));
  return sd === 0 ? 0 : r2((mean / sd) * Math.sqrt(252));
}

const tail = <T,>(a: T[], n: number) => a.slice(Math.max(0, a.length - n));
const pct = (a: number, b: number) => (b ? r2(((a - b) / Math.abs(b)) * 100) : 0);

export function computeKpis(trades: Trade[]): Kpi[] {
  const equity = equityCurve(trades, 90).map((p) => p.equity);
  const now = equity.at(-1) ?? BASE_CAPITAL;
  const prev = equity.at(-2) ?? now;
  const monthAgo = equity.at(-31) ?? equity[0] ?? now;
  const open = trades.filter((t) => t.status === "Open");
  const today = r2(trades.filter((t) => t.exitDate === AS_OF).reduce((s, t) => s + t.pnl, 0) + open.reduce((s, t) => s + t.dayPnl, 0));
  const wl = winLoss(trades);
  const dailyReturns = equity.slice(1).map((v, i) => (equity[i] ? (v - equity[i]) / equity[i] : 0));
  const sh = sharpe(dailyReturns);
  const exposure = open.reduce((s, t) => s + t.markPrice * t.quantity, 0);
  const highShare = open.length ? open.filter((t) => t.risk === "High").length / open.length : 0;
  const riskScore = Math.min(100, Math.round((exposure / Math.max(now, 1)) * 100 * 0.6 + highShare * 40));
  const last30 = tail(equity, 30);
  const wins: number[] = trades.filter((t) => t.status === "Closed").map((t) => (t.pnl > 0 ? 1 : 0));
  const runningWinRate = tail(wins, 30).map((_, i, a) => (a.slice(0, i + 1).reduce((s, v) => s + v, 0) / (i + 1)) * 100);
  while (runningWinRate.length < 30) runningWinRate.unshift(runningWinRate[0] ?? 0);
  return [
    { key: "value", label: "Portfolio value", value: now, display: money(now), trend: pct(now, monthAgo), spark: last30, hint: "vs 30 days ago" },
    { key: "today", label: "Today's P&L", value: today, display: signedMoney(today), trend: pct(now, prev), spark: last30.map((v, i, a) => (i ? v - a[i - 1] : 0)), hint: "vs yesterday" },
    { key: "month", label: "Monthly P&L", value: r2(now - monthAgo), display: signedMoney(r2(now - monthAgo)), trend: pct(now, monthAgo), spark: last30.map((v) => v - last30[0]), hint: "last 30 days" },
    { key: "winrate", label: "Win rate", value: wl.winRate, display: `${wl.winRate.toFixed(1)}%`, trend: r2(wl.winRate - 50), spark: runningWinRate, hint: `${wl.wins} wins, ${wl.losses} losses` },
    { key: "open", label: "Open positions", value: open.length, display: String(open.length), trend: r2(exposure / Math.max(now, 1) * 100), spark: last30.map((_, i) => open.length * (0.6 + 0.4 * (i / 29))), hint: "% of equity invested" },
    { key: "risk", label: "Risk score", value: riskScore, display: `${riskScore}/100`, trend: r2(highShare * 100), spark: last30.map((_, i) => riskScore * (0.85 + 0.15 * Math.sin(i / 3) ** 2)), hint: "% of open trades rated high risk" },
    { key: "sharpe", label: "Sharpe ratio", value: sh, display: sh.toFixed(2), trend: r2(sh), spark: tail(dailyReturns, 30).map((_, i, a) => sharpe(a.slice(0, Math.max(2, i + 1)))), hint: "annualised, 90 days" },
  ];
}

// ---------- CSV ----------
export const CSV_COLUMNS = ["Status", "Entry date", "Exit date", "Symbol", "Company", "Strategy", "Buy price", "Sell price", "Quantity", "Profit/Loss", "ROI %", "Holding days"] as const;

const csvCell = (v: string | number | null) => {
  const s = v === null ? "" : String(v);
  // A text cell that starts like a formula is neutralised, or a spreadsheet could run it (CSV injection)
  const safe = typeof v === "string" && /^[=+\-@\t\r]/.test(s) ? `'${s}` : s;
  return /[",\r\n]/.test(safe) ? `"${safe.replace(/"/g, '""')}"` : safe;
};

export function tradesToCsv(trades: Trade[], columns: readonly string[] = CSV_COLUMNS): string {
  const value = (t: Trade, c: string): string | number | null =>
    ({ Status: t.status, "Entry date": t.entryDate, "Exit date": t.exitDate, Symbol: t.symbol, Company: t.company, Strategy: t.strategy, "Buy price": t.buyPrice, "Sell price": t.sellPrice, Quantity: t.quantity, "Profit/Loss": t.pnl, "ROI %": t.roi, "Holding days": t.holdingDays })[c] ?? null;
  return [columns.map(csvCell).join(","), ...trades.map((t) => columns.map((c) => csvCell(value(t, c))).join(","))].join("\r\n") + "\r\n";
}
