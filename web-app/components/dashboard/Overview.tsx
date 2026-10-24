"use client";

import Link from "next/link";
import { AccountPicker, useSelectedAccount } from "@/components/accounts/AccountPicker";
import { PnlCalendar } from "@/components/dashboard/PnlCalendar";
import { FormAlert } from "@/components/ui/form";
import { QueryState } from "@/components/ui/QueryState";
import { useAccountSummary, usePerformance } from "@/lib/api/account-hooks";
import { brokerLabel, isTracking, money, signed } from "@/lib/format";

function Kpi({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="rounded-brand border border-line bg-panel p-4">
      <dt className="text-sm text-subtle">{label}</dt>
      <dd className="mt-1 font-display text-xl font-extrabold">{value}</dd>
      {hint && <dd className="mt-0.5 text-xs text-subtle">{hint}</dd>}
    </div>
  );
}

/** Key figures and the P&L calendar of the selected account: summary and performance as account-service reports them. */
export function Overview() {
  const { accounts, selected } = useSelectedAccount();
  const summary = useAccountSummary(selected?.id);
  const performance = usePerformance(selected?.id);
  const s = summary.data, p = performance.data;
  const cur = s?.currency ?? selected?.currency;

  return (
    <QueryState query={[accounts.me, accounts]} what="your accounts" next="/dashboard">
      {!selected ? (
        <FormAlert tone="info" title="No accounts yet">
          <Link href="/accounts" className="font-semibold text-link underline underline-offset-4">
            Open an account
          </Link>{" "}
          or track your broker by importing a statement, and your figures appear here.
        </FormAlert>
      ) : (
        <div className="grid gap-8">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <AccountPicker id="dashboard-account" />
            <p className="text-sm text-subtle">
              {selected.accountName} · {brokerLabel[selected.brokerName ?? ""] ?? selected.brokerName} ·{" "}
              <Link href={`/accounts/${selected.id}`} className="inline-flex min-h-11 items-center font-semibold text-link underline underline-offset-4">
                Account details
              </Link>
            </p>
          </div>
          <QueryState query={[summary, performance]} what="the figures" next="/dashboard">
            {s && p && (
              <section aria-labelledby="kpi-heading">
                <h2 id="kpi-heading" className="mb-3 font-display text-xl font-bold">
                  Key figures
                </h2>
                {!p.pricesLive && (s.openPositions ?? 0) > 0 && <p className="mb-3 text-sm text-subtle">Live prices are not available right now: positions are valued at their last known price.</p>}
                <dl className="grid grid-cols-2 gap-3 lg:grid-cols-4">
                  <Kpi label="Total equity" value={money(s.totalEquity, cur)} />
                  <Kpi label={isTracking(selected.brokerName) ? "Cash (not tracked)" : "Cash balance"} value={money(s.cashBalance, cur)} />
                  <Kpi label="Market value" value={money(s.marketValue, cur)} hint={`${s.openPositions ?? 0} open position${s.openPositions === 1 ? "" : "s"}`} />
                  <Kpi label="Net profit" value={signed(p.netProfit, cur)} hint="Realised and unrealised, after charges" />
                  <Kpi label="Realised profit" value={signed(p.realizedProfit, cur)} />
                  <Kpi label="Unrealised profit" value={signed(p.unrealizedProfit, cur)} />
                  <Kpi label="Charges" value={money(p.totalCharges, cur)} />
                  <Kpi label="Taxes" value={money(p.totalTaxes, cur)} />
                </dl>
              </section>
            )}
          </QueryState>
          <PnlCalendar accountId={selected.id!} currency={cur} expectedTotal={p?.realizedProfit} />
        </div>
      )}
    </QueryState>
  );
}
