"use client";

import { useState } from "react";
import { useTrades } from "@/lib/api/account-hooks";
import { QueryState } from "@/components/ui/QueryState";
import { money, signed } from "@/lib/format";
import { MONTH_NAMES, allTimeProfit, dailyProfit, initialMonth, monthGrid, monthKey, monthTotal } from "@/lib/pnl";

const WEEKDAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

/** Realised profit per day of a month. Figures are the sums of the trades' own `realizedProfit`; nothing is recalculated. */
export function PnlCalendar({ accountId, currency, expectedTotal }: { accountId: string; currency: string | undefined; expectedTotal?: number }) {
  const trades = useTrades(accountId);
  const [shown, setShown] = useState<{ id: string; year: number; month: number } | null>(null);
  const days = dailyProfit(trades.data ?? []);
  // Open on the latest trade's month; remember the user's own choice per account.
  const start = initialMonth(days);
  const cur = shown?.id === accountId ? shown : { id: accountId, ...start };
  const move = (delta: number) => {
    const index = cur.year * 12 + (cur.month - 1) + delta;
    setShown({ id: accountId, year: Math.floor(index / 12), month: (index % 12) + 1 });
  };
  const total = monthTotal(days, cur.year, cur.month);
  const all = allTimeProfit(days);
  const weeks = monthGrid(cur.year, cur.month);

  return (
    <section aria-labelledby="pnl-heading" className="grid gap-4">
      <h2 id="pnl-heading" className="font-display text-xl font-bold">
        Profit and loss calendar
      </h2>
      <QueryState query={trades} what="the calendar" next="/dashboard">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <button type="button" onClick={() => move(-1)} aria-label="Previous month" className="min-h-11 min-w-11 rounded-brand border border-line text-lg hover:bg-soft">
              ‹
            </button>
            <p role="status" aria-live="polite" className="min-w-36 text-center font-semibold">
              {MONTH_NAMES[cur.month - 1]} {cur.year}
            </p>
            <button type="button" onClick={() => move(1)} aria-label="Next month" className="min-h-11 min-w-11 rounded-brand border border-line text-lg hover:bg-soft">
              ›
            </button>
          </div>
          <p className="text-sm">
            This month: <strong data-testid="month-total">{signed(total.profit, currency)}</strong> from {total.trades} trade{total.trades === 1 ? "" : "s"}
          </p>
        </div>

        <div role="region" aria-label="Calendar" tabIndex={0} className="overflow-x-auto rounded-brand border border-line">
          <table className="w-full min-w-[34rem] table-fixed border-collapse text-sm">
            <caption className="sr-only">
              Realised profit per day, {MONTH_NAMES[cur.month - 1]} {cur.year}
            </caption>
            <thead>
              <tr className="bg-soft">
                {WEEKDAYS.map((d) => (
                  <th key={d} scope="col" className="p-2 text-left font-semibold">
                    {d}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {weeks.map((week, w) => (
                <tr key={w}>
                  {week.map((day, i) => {
                    if (day === null) return <td key={i} className="h-16 border border-line/40 bg-soft/40" />;
                    const d = days.get(`${monthKey(cur.year, cur.month)}-${String(day).padStart(2, "0")}`);
                    const label = d ? `${day} ${MONTH_NAMES[cur.month - 1]}: ${d.profit > 0 ? "profit" : d.profit < 0 ? "loss" : "no profit or loss"} ${money(Math.abs(d.profit), currency)}, ${d.trades} trade${d.trades === 1 ? "" : "s"}` : `${day} ${MONTH_NAMES[cur.month - 1]}: no trades`;
                    return (
                      <td key={i} aria-label={label} data-day={d ? `${d.profit}` : undefined} className={`h-16 border border-line/40 p-1.5 align-top ${d ? "bg-soft" : ""}`}>
                        <span className="block text-xs text-subtle">{day}</span>
                        {d && (
                          <span className="mt-1 block text-xs font-semibold tabular-nums sm:text-sm">
                            {signed(d.profit, currency)}
                            <span className="block text-[0.7rem] font-normal text-subtle">{d.trades} trade{d.trades === 1 ? "" : "s"}</span>
                          </span>
                        )}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="text-sm text-subtle">
          All days together: <strong className="text-ink" data-testid="all-time">{signed(all, currency)}</strong>
          {expectedTotal !== undefined && Math.abs(all - expectedTotal) > 0.005 && <span> (the account reports {signed(expectedTotal, currency)}: some trades may lie outside the calendar)</span>}
        </p>
      </QueryState>
    </section>
  );
}
