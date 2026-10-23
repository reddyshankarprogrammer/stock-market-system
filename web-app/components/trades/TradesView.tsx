"use client";

import { useState } from "react";
import Link from "next/link";
import { AccountPicker, useSelectedAccount } from "@/components/accounts/AccountPicker";
import { TradeForm } from "@/components/trades/TradeForm";
import { FormAlert } from "@/components/ui/form";
import { QueryState } from "@/components/ui/QueryState";
import { useTrades } from "@/lib/api/account-hooks";
import { dateTime, money, quantity, signed } from "@/lib/format";
import { SORT_COLUMNS, filterTrades, isSortKey, paginate, sortTrades, sumRealized, type SortDir, type SortKey } from "@/lib/trades";

const inputClass = "block min-h-11 w-full rounded-brand border border-line bg-panel px-3.5 py-2.5 text-base text-ink";

export function TradesView() {
  const { accounts, selected, url } = useSelectedAccount();
  const [entering, setEntering] = useState(false);

  const from = url.get("from"), to = url.get("to"), symbol = url.get("symbol"), side = url.get("side");
  const sortParam = url.get("sort");
  const sort: SortKey = isSortKey(sortParam) ? sortParam : "tradeDate";
  const dir: SortDir = url.get("dir") === "asc" ? "asc" : "desc";
  const page = Number(url.get("page")) || 1;

  const trades = useTrades(selected?.id, from || undefined, to || undefined);
  const filtered = filterTrades(trades.data ?? [], { symbol, side: side === "BUY" || side === "SELL" ? side : "" });
  const sorted = sortTrades(filtered, sort, dir);
  const view = paginate(sorted, page);
  const cur = selected?.currency;
  const filtering = Boolean(from || to || symbol || side);

  const toggleSort = (key: SortKey) => url.set({ sort: key, dir: sort === key && dir === "desc" ? "asc" : "desc", page: undefined });

  return (
    <div className="grid gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-extrabold tracking-tight">Trades</h1>
          <p className="mt-2 text-subtle">Every trade of one account, from the platform, a statement or your own entries.</p>
        </div>
        {selected && !entering && (
          <button type="button" onClick={() => setEntering(true)} className="min-h-11 rounded-brand bg-electric px-5 text-sm font-semibold text-white hover:brightness-110">
            Record a trade
          </button>
        )}
      </div>

      <QueryState query={[accounts.me, accounts]} what="your accounts" next="/trades">
        {!selected ? (
          <FormAlert tone="info" title="No accounts yet">
            Trades belong to an account.{" "}
            <Link href="/accounts" className="font-semibold text-link underline underline-offset-4">
              Open one first
            </Link>
            .
          </FormAlert>
        ) : (
          <div className="grid gap-6">
            <AccountPicker />
            {entering && <TradeForm account={selected} onDone={() => setEntering(false)} />}

            <form className="grid gap-4 rounded-brand border border-line p-4 sm:grid-cols-2 lg:grid-cols-5" aria-label="Filter trades" onSubmit={(e) => e.preventDefault()}>
              <div className="grid gap-1.5">
                <label htmlFor="f-from" className="text-sm font-semibold">From</label>
                <input id="f-from" type="date" value={from} max={to || undefined} onChange={(e) => url.set({ from: e.target.value, page: undefined })} className={inputClass} />
              </div>
              <div className="grid gap-1.5">
                <label htmlFor="f-to" className="text-sm font-semibold">To</label>
                <input id="f-to" type="date" value={to} min={from || undefined} onChange={(e) => url.set({ to: e.target.value, page: undefined })} className={inputClass} />
              </div>
              <div className="grid gap-1.5">
                <label htmlFor="f-symbol" className="text-sm font-semibold">Symbol</label>
                <input id="f-symbol" type="search" value={symbol} placeholder="e.g. INFY" autoComplete="off" onChange={(e) => url.set({ symbol: e.target.value, page: undefined })} className={inputClass} />
              </div>
              <div className="grid gap-1.5">
                <label htmlFor="f-side" className="text-sm font-semibold">Side</label>
                <select id="f-side" value={side} onChange={(e) => url.set({ side: e.target.value, page: undefined })} className={inputClass}>
                  <option value="">Buy and sell</option>
                  <option value="BUY">Buy</option>
                  <option value="SELL">Sell</option>
                </select>
              </div>
              <div className="flex items-end">
                <button type="button" disabled={!filtering} onClick={() => url.set({ from: undefined, to: undefined, symbol: undefined, side: undefined, page: undefined })} className="min-h-11 w-full rounded-brand border border-line px-4 text-sm font-semibold hover:bg-soft disabled:opacity-50">
                  Clear filters
                </button>
              </div>
            </form>

            <QueryState query={trades} what="the trades" next="/trades">
              <p role="status" aria-live="polite" className="text-sm text-subtle">
                {sorted.length} trade{sorted.length === 1 ? "" : "s"}
                {filtering ? " match your filters" : ""}. Realised profit in this view: <strong className="text-ink">{signed(sumRealized(sorted), cur)}</strong>
              </p>
              {sorted.length === 0 ? (
                <FormAlert tone="info" title={filtering ? "No trades match" : "No trades yet"}>
                  {filtering ? "Change or clear the filters." : "Record a trade, or import a broker statement on the account page."}
                </FormAlert>
              ) : (
                <>
                  <div role="region" aria-label="Trades table" tabIndex={0} className="overflow-x-auto rounded-brand border border-line">
                    <table className="w-full min-w-[40rem] text-left text-sm">
                      <caption className="sr-only">Trades of {selected.accountName}, sorted by {SORT_COLUMNS.find((c) => c.key === sort)?.label} {dir === "asc" ? "ascending" : "descending"}</caption>
                      <thead className="border-b border-line bg-soft">
                        <tr>
                          {SORT_COLUMNS.map((c) => (
                            <th key={c.key} scope="col" aria-sort={sort === c.key ? (dir === "asc" ? "ascending" : "descending") : "none"} className={`p-0 ${c.numeric ? "text-right" : ""}`}>
                              <button type="button" onClick={() => toggleSort(c.key)} className={`flex min-h-11 w-full items-center gap-1 px-3 font-semibold hover:bg-soft ${c.numeric ? "justify-end" : ""}`}>
                                {c.label}
                                <span aria-hidden="true">{sort === c.key ? (dir === "asc" ? "▲" : "▼") : ""}</span>
                              </button>
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {view.rows.map((t) => (
                          <tr key={t.id} className="border-b border-line last:border-0">
                            <td className="whitespace-nowrap px-3 py-2">
                              <Link href={`/trades/${t.id}`} className="inline-flex min-h-11 items-center font-semibold text-link underline underline-offset-4">
                                {dateTime(t.tradeDate)}
                              </Link>
                            </td>
                            <td className="px-3 py-2 font-semibold">{t.symbol}</td>
                            <td className="px-3 py-2">{t.side === "BUY" ? "Buy" : t.side === "SELL" ? "Sell" : t.side}</td>
                            <td className="px-3 py-2 text-right tabular-nums">{quantity(t.quantity)}</td>
                            <td className="px-3 py-2 text-right tabular-nums">{money(t.price, cur)}</td>
                            <td className="px-3 py-2 text-right tabular-nums">{t.side === "SELL" ? signed(t.realizedProfit, cur) : "-"}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  <nav aria-label="Pages" className="flex flex-wrap items-center justify-between gap-3">
                    <p className="text-sm text-subtle">
                      Showing {(view.page - 1) * 15 + 1} to {Math.min(view.page * 15, sorted.length)} of {sorted.length}
                    </p>
                    <div className="flex items-center gap-2">
                      <button type="button" disabled={view.page <= 1} onClick={() => url.set({ page: String(view.page - 1) })} className="min-h-11 rounded-brand border border-line px-4 text-sm font-semibold hover:bg-soft disabled:opacity-50">
                        Previous
                      </button>
                      <span className="text-sm">
                        Page {view.page} of {view.pages}
                      </span>
                      <button type="button" disabled={view.page >= view.pages} onClick={() => url.set({ page: String(view.page + 1) })} className="min-h-11 rounded-brand border border-line px-4 text-sm font-semibold hover:bg-soft disabled:opacity-50">
                        Next
                      </button>
                    </div>
                  </nav>
                </>
              )}
            </QueryState>
          </div>
        )}
      </QueryState>
    </div>
  );
}
