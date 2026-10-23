"use client";

import Link from "next/link";
import { AccountPicker, useSelectedAccount } from "@/components/accounts/AccountPicker";
import { FormAlert } from "@/components/ui/form";
import { QueryState } from "@/components/ui/QueryState";
import { useAccountSummary, useHoldings } from "@/lib/api/account-hooks";
import { dateTime, money, percent, quantity, signed } from "@/lib/format";

const REFRESH_SECONDS = 10;

/** Open positions with live prices. account-service takes the prices from market-data; this screen asks again every 10 seconds. */
export function PortfolioView() {
  const { accounts, selected } = useSelectedAccount();
  const holdings = useHoldings(selected?.id, true);
  const summary = useAccountSummary(selected?.id);
  const cur = selected?.currency;
  const rows = [...(holdings.data ?? [])].sort((a, b) => (b.marketValue ?? 0) - (a.marketValue ?? 0));
  const updated = rows.map((h) => h.priceUpdatedAt).filter(Boolean).sort().at(-1);
  const s = summary.data;

  return (
    <div className="grid gap-6">
      <div>
        <h1 className="font-display text-3xl font-extrabold tracking-tight">Portfolio</h1>
        <p className="mt-2 text-subtle">Open positions of one account at current market prices.</p>
      </div>
      <QueryState query={[accounts.me, accounts]} what="your accounts" next="/portfolio">
        {!selected ? (
          <FormAlert tone="info" title="No accounts yet">
            Holdings belong to an account.{" "}
            <Link href="/accounts" className="font-semibold text-link underline underline-offset-4">
              Open one first
            </Link>
            .
          </FormAlert>
        ) : (
          <div className="grid gap-6">
            <div className="flex flex-wrap items-end justify-between gap-4">
              <AccountPicker />
              <button type="button" onClick={() => { holdings.refetch(); summary.refetch(); }} disabled={holdings.isFetching} className="min-h-11 rounded-brand border border-line px-4 text-sm font-semibold hover:bg-soft disabled:opacity-60">
                {holdings.isFetching ? "Refreshing…" : "Refresh prices"}
              </button>
            </div>
            <QueryState query={holdings} what="the holdings" next="/portfolio">
              {s && (
                <dl className="grid grid-cols-2 gap-3 lg:grid-cols-4">
                  {[
                    ["Market value", money(s.marketValue, cur)],
                    ["Invested", money(s.investedAmount, cur)],
                    ["Unrealised profit", signed(s.unrealizedProfit, cur)],
                    ["Open positions", String(s.openPositions ?? 0)],
                  ].map(([label, value]) => (
                    <div key={label} className="rounded-brand border border-line bg-panel p-4">
                      <dt className="text-sm text-subtle">{label}</dt>
                      <dd className="mt-1 font-display text-xl font-extrabold">{value}</dd>
                    </div>
                  ))}
                </dl>
              )}
              {rows.length === 0 ? (
                <FormAlert tone="info" title="No open positions">
                  Buy shares on the Trades page, or import a broker statement on the account page.
                </FormAlert>
              ) : (
                <>
                  <div role="region" aria-label="Holdings table" tabIndex={0} className="overflow-x-auto rounded-brand border border-line">
                    <table className="w-full min-w-[40rem] text-left text-sm">
                      <caption className="sr-only">Open positions of {selected.accountName}</caption>
                      <thead className="border-b border-line bg-soft">
                        <tr>
                          {["Symbol", "Quantity", "Average price", "Current price", "Market value", "Unrealised profit"].map((h, i) => (
                            <th key={h} scope="col" className={`px-3 py-3 font-semibold ${i > 0 ? "text-right" : ""}`}>
                              {h}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {rows.map((h) => (
                          <tr key={h.id} className="border-b border-line last:border-0">
                            <th scope="row" className="px-3 py-3 font-semibold">{h.symbol}</th>
                            <td className="px-3 py-3 text-right tabular-nums">{quantity(h.quantity)}</td>
                            <td className="px-3 py-3 text-right tabular-nums">{money(h.averagePrice, cur)}</td>
                            <td className="px-3 py-3 text-right tabular-nums" data-price={h.currentPrice}>{money(h.currentPrice, cur)}</td>
                            <td className="px-3 py-3 text-right tabular-nums">{money(h.marketValue, cur)}</td>
                            <td className="px-3 py-3 text-right tabular-nums">
                              {signed(h.unrealizedProfit, cur)} <span className="text-subtle">({percent(h.unrealizedProfitPercent)})</span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  <p className="text-sm text-subtle">
                    {s && !s.pricesLive ? "Live prices are not available right now: positions are shown at their last known price. " : ""}
                    Prices as of {dateTime(updated)}. This page checks for new prices every {REFRESH_SECONDS} seconds.
                  </p>
                </>
              )}
            </QueryState>
          </div>
        )}
      </QueryState>
    </div>
  );
}
