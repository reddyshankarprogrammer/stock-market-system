import { z } from "zod";
import type { Trade } from "@/lib/api/account-api";

// Filtering, sorting and paging of an account's trades. account-service filters only by date (from, to); the rest is
// done here on the list it returned, so the table can never show a trade the API did not send.
export type SortKey = "tradeDate" | "symbol" | "side" | "quantity" | "price" | "realizedProfit";
export type SortDir = "asc" | "desc";
export const PAGE_SIZE = 15;

export const SORT_COLUMNS: { key: SortKey; label: string; numeric?: boolean }[] = [
  { key: "tradeDate", label: "Date" },
  { key: "symbol", label: "Symbol" },
  { key: "side", label: "Side" },
  { key: "quantity", label: "Quantity", numeric: true },
  { key: "price", label: "Price", numeric: true },
  { key: "realizedProfit", label: "Realised profit", numeric: true },
];

export type TradeFilter = { symbol?: string; side?: "BUY" | "SELL" | ""; from?: string; to?: string };

export function filterTrades(trades: Trade[], f: TradeFilter): Trade[] {
  const symbol = f.symbol?.trim().toUpperCase();
  // Dates compare as written (yyyy-MM-dd of the trade date), inclusive. account-service filters only when it is given both
  // ends, so a one-sided range is applied here; with both ends the API has already done it and this agrees with it.
  return trades.filter((t) => {
    const day = (t.tradeDate ?? "").slice(0, 10);
    return (!symbol || (t.symbol ?? "").toUpperCase().includes(symbol)) && (!f.side || t.side === f.side) && (!f.from || day >= f.from) && (!f.to || day <= f.to);
  });
}

export function sortTrades(trades: Trade[], key: SortKey, dir: SortDir): Trade[] {
  const sign = dir === "asc" ? 1 : -1;
  return [...trades].sort((a, b) => {
    const x = a[key] ?? (typeof b[key] === "number" ? 0 : "");
    const y = b[key] ?? (typeof a[key] === "number" ? 0 : "");
    const c = typeof x === "number" && typeof y === "number" ? x - y : String(x).localeCompare(String(y));
    // Ties keep a stable, readable order: newest first.
    return c !== 0 ? c * sign : String(b.tradeDate ?? "").localeCompare(String(a.tradeDate ?? ""));
  });
}

export function paginate<T>(items: T[], page: number, size = PAGE_SIZE): { rows: T[]; pages: number; page: number } {
  const pages = Math.max(1, Math.ceil(items.length / size));
  const p = Math.min(Math.max(1, page), pages);
  return { rows: items.slice((p - 1) * size, p * size), pages, page: p };
}

export const sumRealized = (trades: Trade[]) => trades.reduce((s, t) => s + (t.realizedProfit ?? 0), 0);

export const isSortKey = (v: string | null): v is SortKey => SORT_COLUMNS.some((c) => c.key === v);

// ---- entering a trade ----
const symbol = z.string().trim().min(1, "Enter the symbol").max(20, "Use at most 20 characters").regex(/^[A-Za-z0-9.\-&]+$/, "Use letters, numbers, dot, dash or &");

/** Platform and paper accounts trade at the market price: only a symbol and a whole number of shares. */
export const platformTradeSchema = z.object({
  side: z.enum(["BUY", "SELL"], { error: "Choose Buy or Sell" }),
  symbol,
  quantity: z.coerce.number({ error: "Enter a number" }).int("Use a whole number of shares").min(1, "Enter at least 1 share").max(1_000_000, "That is too many shares"),
});
export type PlatformTradeValues = z.input<typeof platformTradeSchema>;

/** Tracking accounts record what already happened at the broker: price and date are the user's. */
export const manualTradeSchema = z.object({
  side: z.enum(["BUY", "SELL"], { error: "Choose Buy or Sell" }),
  symbol,
  quantity: z.coerce.number({ error: "Enter a number" }).positive("Enter a quantity above zero").max(1_000_000_000, "That is too many"),
  price: z.coerce.number({ error: "Enter a number" }).positive("Enter a price above zero").max(1_000_000_000, "That is too large"),
  tradeDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Choose the trade date").refine((d) => d <= new Date().toISOString().slice(0, 10), "The date cannot be in the future"),
  notes: z.string().trim().max(500, "Use at most 500 characters").optional(),
});
export type ManualTradeValues = z.input<typeof manualTradeSchema>;
export type ManualTradeParsed = z.output<typeof manualTradeSchema>;
