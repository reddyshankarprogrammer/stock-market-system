"use client";

import { useMemo, useState } from "react";
import { DataTable, type Column } from "@/components/terminal/DataTable";
import { PageFrame, Stat, fieldClass, panel, signedPct, signedUsd, tone, usd, usd0 } from "@/components/terminal/PageFrame";
import { STRATEGIES, TRADES } from "@/lib/terminal/sample-data";
import { positionsOf, type Position } from "@/lib/terminal/sample-extra";

const all = positionsOf(TRADES);

const columns: Column<Position>[] = [
  { id: "symbol", header: "Symbol", cell: (p) => <strong>{p.symbol}</strong>, sort: (p) => p.symbol },
  { id: "strategy", header: "Strategy", cell: (p) => p.strategy, sort: (p) => p.strategy },
  { id: "entry", header: "Entry date", cell: (p) => p.entryDate, sort: (p) => p.entryDate },
  { id: "qty", header: "Quantity", cell: (p) => String(p.quantity), sort: (p) => p.quantity, numeric: true },
  { id: "buy", header: "Entry", cell: (p) => usd.format(p.buyPrice), sort: (p) => p.buyPrice, numeric: true },
  { id: "price", header: "Price", cell: (p) => usd.format(p.markPrice), sort: (p) => p.markPrice, numeric: true },
  { id: "stop", header: "Stop", cell: (p) => usd.format(p.stop), sort: (p) => p.stop, numeric: true },
  { id: "target", header: "Target", cell: (p) => usd.format(p.target), sort: (p) => p.target, numeric: true },
  { id: "rr", header: "Reward : risk", cell: (p) => p.riskReward.toFixed(2), sort: (p) => p.riskReward, numeric: true },
  { id: "dist", header: "To stop", cell: (p) => `${p.distanceToStop.toFixed(1)}%`, sort: (p) => p.distanceToStop, numeric: true },
  { id: "pnl", header: "Unrealised P&L", cell: (p) => signedUsd(p.unrealized), sort: (p) => p.unrealized, numeric: true, tone: (p) => tone(p.unrealized) },
  { id: "roi", header: "ROI", cell: (p) => signedPct(p.roi), sort: (p) => p.roi, numeric: true, tone: (p) => tone(p.roi) },
  { id: "risk", header: "Risk", cell: (p) => p.risk, sort: (p) => p.risk },
];

export function PositionsPage() {
  const [strategy, setStrategy] = useState("");
  const [risk, setRisk] = useState("");
  const rows = useMemo(() => all.filter((p) => (!strategy || p.strategy === strategy) && (!risk || p.risk === risk)), [strategy, risk]);
  const exposure = rows.reduce((s, p) => s + p.markPrice * p.quantity, 0);
  const pnl = rows.reduce((s, p) => s + p.unrealized, 0);
  const nearest = [...rows].sort((a, b) => a.distanceToStop - b.distanceToStop)[0];
  return (
    <PageFrame title="Positions" description="Open trades with their stop, target and distance to the stop.">
      <dl aria-label="Position figures (sample data)" className="grid grid-cols-2 gap-3 xl:grid-cols-5">
        <Stat label="Open positions" value={String(rows.length)} />
        <Stat label="Exposure" value={usd0.format(exposure)} />
        <Stat label="Unrealised P&L" value={signedUsd(pnl, 0)} tone={tone(pnl)} />
        <Stat label="High risk" value={String(rows.filter((p) => p.risk === "High").length)} hint="positions rated high" />
        <Stat label="Closest to its stop" value={nearest ? nearest.symbol : "-"} hint={nearest ? `${nearest.distanceToStop.toFixed(1)}% above the stop` : undefined} />
      </dl>
      <form aria-label="Filter the positions" onSubmit={(e) => e.preventDefault()} className={`${panel} grid grid-cols-1 gap-3 sm:grid-cols-2`}>
        <div className="grid gap-1">
          <label htmlFor="p-strategy" className="text-xs font-semibold text-term-muted">Strategy</label>
          <select id="p-strategy" value={strategy} onChange={(e) => setStrategy(e.target.value)} className={fieldClass}>
            <option value="">All strategies</option>
            {STRATEGIES.map((s) => <option key={s}>{s}</option>)}
          </select>
        </div>
        <div className="grid gap-1">
          <label htmlFor="p-risk" className="text-xs font-semibold text-term-muted">Risk</label>
          <select id="p-risk" value={risk} onChange={(e) => setRisk(e.target.value)} className={fieldClass}>
            <option value="">All risk levels</option>
            {["Low", "Medium", "High"].map((r) => <option key={r}>{r}</option>)}
          </select>
        </div>
      </form>
      <section aria-labelledby="pos-title" className={`${panel} min-w-0`}>
        <h2 id="pos-title" className="mb-3 font-display text-lg font-bold">Open positions</h2>
        <DataTable rows={rows} columns={columns} rowKey={(p) => p.id} label="Positions table" pageSize={10} initialSort={{ id: "dist", dir: "asc" }} empty="No open positions match the filters." />
      </section>
    </PageFrame>
  );
}
