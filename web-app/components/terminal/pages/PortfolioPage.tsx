"use client";

import { Allocations, EquityCurve } from "@/components/terminal/Charts";
import { DataTable, type Column } from "@/components/terminal/DataTable";
import { PageFrame, Stat, panel, signedPct, signedUsd, tone, usd, usd0 } from "@/components/terminal/PageFrame";
import { TRADES } from "@/lib/terminal/sample-data";
import { holdingsOf, type Holding } from "@/lib/terminal/sample-extra";

const holdings = holdingsOf(TRADES);

const columns: Column<Holding>[] = [
  { id: "symbol", header: "Symbol", cell: (h) => <strong>{h.symbol}</strong>, sort: (h) => h.symbol },
  { id: "company", header: "Company", cell: (h) => h.company, sort: (h) => h.company },
  { id: "sector", header: "Sector", cell: (h) => h.sector, sort: (h) => h.sector },
  { id: "qty", header: "Quantity", cell: (h) => String(h.quantity), sort: (h) => h.quantity, numeric: true },
  { id: "avg", header: "Average cost", cell: (h) => usd.format(h.avgCost), sort: (h) => h.avgCost, numeric: true },
  { id: "price", header: "Price", cell: (h) => usd.format(h.price), sort: (h) => h.price, numeric: true },
  { id: "value", header: "Market value", cell: (h) => usd0.format(h.value), sort: (h) => h.value, numeric: true },
  { id: "pnl", header: "Unrealised P&L", cell: (h) => signedUsd(h.pnl), sort: (h) => h.pnl, numeric: true, tone: (h) => tone(h.pnl) },
  { id: "pct", header: "Return", cell: (h) => signedPct(h.pnlPercent), sort: (h) => h.pnlPercent, numeric: true, tone: (h) => tone(h.pnlPercent) },
  { id: "weight", header: "Weight", cell: (h) => `${h.weight.toFixed(1)}%`, sort: (h) => h.weight, numeric: true },
];

export function PortfolioPage() {
  const value = holdings.reduce((s, h) => s + h.value, 0);
  const invested = holdings.reduce((s, h) => s + h.invested, 0);
  const pnl = value - invested;
  const largest = holdings[0];
  return (
    <PageFrame title="Portfolio" description="What the sample portfolio holds, what it is worth and where it is concentrated.">
      <dl aria-label="Portfolio figures (sample data)" className="grid grid-cols-2 gap-3 xl:grid-cols-5">
        <Stat label="Market value" value={usd0.format(value)} />
        <Stat label="Invested" value={usd0.format(invested)} />
        <Stat label="Unrealised P&L" value={signedUsd(pnl, 0)} hint={signedPct(invested ? (pnl / invested) * 100 : 0)} tone={tone(pnl)} />
        <Stat label="Holdings" value={String(holdings.length)} hint="distinct symbols" />
        <Stat label="Largest holding" value={largest ? largest.symbol : "-"} hint={largest ? `${largest.weight.toFixed(1)}% of the portfolio` : undefined} />
      </dl>
      <section aria-labelledby="hold-title" className={`${panel} min-w-0`}>
        <h2 id="hold-title" className="mb-3 font-display text-lg font-bold">Holdings</h2>
        <DataTable rows={holdings} columns={columns} rowKey={(h) => h.symbol} label="Holdings table" pageSize={8} initialSort={{ id: "value", dir: "desc" }} />
      </section>
      <section aria-labelledby="palloc-title" className="grid grid-cols-1 gap-4">
        <h2 id="palloc-title" className="font-display text-lg font-bold">Allocation</h2>
        <Allocations trades={TRADES} />
      </section>
      <EquityCurve trades={TRADES} />
    </PageFrame>
  );
}
