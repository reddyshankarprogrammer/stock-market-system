"use client";

import { useMemo, useState } from "react";
import { Allocations, EquityCurve, MonthlyReturns, SectorPerformance, WinLoss } from "@/components/terminal/Charts";
import { FilterBar } from "@/components/terminal/FilterBar";
import { KpiCards } from "@/components/terminal/KpiCards";
import { RightPanel } from "@/components/terminal/RightPanel";
import { TradingActivity } from "@/components/terminal/TradingActivity";
import { AS_OF, NO_FILTERS, TRADES, applyFilters, computeKpis, type Filters } from "@/lib/terminal/sample-data";

/** The trading terminal dashboard. Every figure and chart is computed from the SAMPLE trades after the filters. */
export function TerminalDashboard() {
  const [filters, setFilters] = useState<Filters>(NO_FILTERS);
  const trades = useMemo(() => applyFilters(TRADES, filters), [filters]);
  const kpis = useMemo(() => computeKpis(trades), [trades]);

  return (
    <div className="grid grid-cols-1 gap-5">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-extrabold tracking-tight">Dashboard</h1>
          <p className="mt-1 text-sm text-term-muted">Trading terminal preview. As of {AS_OF}.</p>
        </div>
        <p className="rounded-xl border border-term-teal px-3 py-2 text-sm font-semibold text-term-teal" role="note">
          Sample data: these are not your accounts or your trades
        </p>
      </header>

      <FilterBar filters={filters} onChange={setFilters} shown={trades.length} total={TRADES.length} />
      <KpiCards kpis={kpis} />

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_20rem]">
        <div className="grid min-w-0 grid-cols-1 gap-5">
          <TradingActivity trades={trades} />
          <section aria-labelledby="perf-title" className="grid grid-cols-1 gap-4">
            <h2 id="perf-title" className="font-display text-lg font-bold">
              Performance analytics
            </h2>
            <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
              <EquityCurve trades={trades} />
              <MonthlyReturns trades={trades} />
              <WinLoss trades={trades} />
              <SectorPerformance trades={trades} />
            </div>
          </section>
          <section aria-labelledby="alloc-title" className="grid grid-cols-1 gap-4">
            <h2 id="alloc-title" className="font-display text-lg font-bold">
              Portfolio allocation
            </h2>
            <Allocations trades={trades} />
          </section>
        </div>
        <RightPanel />
      </div>
    </div>
  );
}
