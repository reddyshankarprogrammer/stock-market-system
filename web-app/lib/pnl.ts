import type { Trade } from "@/lib/api/account-api";

// Daily realised profit for the P&L calendar. A day's figure is the sum of `realizedProfit` of the trades dated that day,
// exactly the numbers account-service sends, so the sum of all days equals the account's realised profit (performance).
export type Day = { profit: number; trades: number };

const cents = (n: number) => Math.round(n * 100) / 100; // 0.1 + 0.2 must show as 0.30

/** Key `YYYY-MM-DD` from a date-time such as 2026-10-05T10:00:00; the date is used as written, never shifted by a time zone. */
export const dayKey = (tradeDate: string | undefined) => (tradeDate ?? "").slice(0, 10);

export function dailyProfit(trades: Trade[]): Map<string, Day> {
  const days = new Map<string, Day>();
  for (const t of trades) {
    const key = dayKey(t.tradeDate);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(key)) continue;
    const d = days.get(key) ?? { profit: 0, trades: 0 };
    d.profit = cents(d.profit + (t.realizedProfit ?? 0));
    d.trades += 1;
    days.set(key, d);
  }
  return days;
}

export const monthKey = (year: number, month: number) => `${year}-${String(month).padStart(2, "0")}`;

/** Total realised profit and number of trades of a month (month is 1 to 12). */
export function monthTotal(days: Map<string, Day>, year: number, month: number): Day {
  const prefix = monthKey(year, month) + "-";
  let profit = 0, trades = 0;
  for (const [k, d] of days) if (k.startsWith(prefix)) { profit += d.profit; trades += d.trades; }
  return { profit: cents(profit), trades };
}

export const allTimeProfit = (days: Map<string, Day>) => cents([...days.values()].reduce((s, d) => s + d.profit, 0));

/** The weeks of a month, Monday first. Cells outside the month are null. */
export function monthGrid(year: number, month: number): (number | null)[][] {
  const first = new Date(Date.UTC(year, month - 1, 1));
  const lead = (first.getUTCDay() + 6) % 7; // Monday = 0
  const length = new Date(Date.UTC(year, month, 0)).getUTCDate();
  const cells: (number | null)[] = [...Array(lead).fill(null), ...Array.from({ length }, (_, i) => i + 1)];
  while (cells.length % 7) cells.push(null);
  return Array.from({ length: cells.length / 7 }, (_, w) => cells.slice(w * 7, w * 7 + 7));
}

/** The month to open first: the month of the latest trade, otherwise the current month. */
export function initialMonth(days: Map<string, Day>, now = new Date()): { year: number; month: number } {
  const latest = [...days.keys()].sort().at(-1);
  if (latest) return { year: Number(latest.slice(0, 4)), month: Number(latest.slice(5, 7)) };
  return { year: now.getUTCFullYear(), month: now.getUTCMonth() + 1 };
}

export const MONTH_NAMES = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
