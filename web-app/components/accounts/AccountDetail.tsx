"use client";

import Link from "next/link";
import { StatementsPanel } from "@/components/accounts/StatementsPanel";
import { QueryState } from "@/components/ui/QueryState";
import { canUploadStatement } from "@/lib/accounts";
import { useAccount, useAccountSummary } from "@/lib/api/account-hooks";
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

const linkButton = "inline-flex min-h-11 items-center rounded-brand border border-link px-4 text-sm font-semibold text-link hover:bg-link hover:text-canvas";

export function AccountDetail({ id }: { id: string }) {
  const account = useAccount(id);
  const summary = useAccountSummary(id);
  const a = account.data;
  const s = summary.data;
  const cur = s?.currency ?? a?.currency;

  return (
    <div className="grid gap-8">
      <div>
        <p>
          <Link href="/accounts" className="inline-flex min-h-11 items-center text-sm font-semibold text-link underline underline-offset-4">
            ← All accounts
          </Link>
        </p>
        <QueryState query={[account, summary]} what="the account" next={`/accounts/${id}`}>
          {a && s && (
            <div className="grid gap-8">
              <div className="grid gap-2">
                <h1 className="font-display text-3xl font-extrabold tracking-tight">{a.accountName}</h1>
                <p className="text-subtle">
                  {brokerLabel[a.brokerName ?? ""] ?? a.brokerName} · {a.accountNumber} · {a.status === "ACTIVE" ? "Active" : a.status}
                </p>
                <div className="mt-2 flex flex-wrap gap-3">
                  <Link href={`/trades?account=${id}`} className={linkButton}>
                    Trades
                  </Link>
                  <Link href={`/portfolio?account=${id}`} className={linkButton}>
                    Holdings
                  </Link>
                </div>
              </div>

              <section aria-labelledby="summary-heading">
                <h2 id="summary-heading" className="mb-3 font-display text-xl font-bold">
                  Summary
                </h2>
                {!s.pricesLive && (s.openPositions ?? 0) > 0 && <p className="mb-3 text-sm text-subtle">Live prices are not available right now: positions are shown at their last known price.</p>}
                <dl className="grid grid-cols-2 gap-3 lg:grid-cols-4">
                  <Kpi label="Total equity" value={money(s.totalEquity, cur)} />
                  <Kpi label={isTracking(a.brokerName) ? "Cash (not tracked)" : "Cash balance"} value={money(s.cashBalance, cur)} />
                  <Kpi label="Market value" value={money(s.marketValue, cur)} hint={`${s.openPositions ?? 0} open position${s.openPositions === 1 ? "" : "s"}`} />
                  <Kpi label="Invested" value={money(s.investedAmount, cur)} />
                  <Kpi label="Realised profit" value={signed(s.realizedProfit, cur)} />
                  <Kpi label="Unrealised profit" value={signed(s.unrealizedProfit, cur)} />
                  <Kpi label="Fees" value={money(s.totalFees, cur)} />
                  <Kpi label="Taxes" value={money(s.totalTaxes, cur)} />
                </dl>
              </section>

              {canUploadStatement(a.brokerName) ? (
                <StatementsPanel accountId={id} brokerLabel={brokerLabel[a.brokerName ?? ""] ?? "broker"} currency={cur} />
              ) : isTracking(a.brokerName) ? (
                <p className="text-subtle">
                  Statements cannot be imported into a manual account. Record trades by hand on the{" "}
                  <Link href={`/trades?account=${id}`} className="font-semibold text-link underline underline-offset-4">
                    Trades
                  </Link>{" "}
                  page.
                </p>
              ) : null}
            </div>
          )}
        </QueryState>
      </div>
    </div>
  );
}
