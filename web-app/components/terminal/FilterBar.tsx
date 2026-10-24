"use client";

import { useState } from "react";
import { RefreshCw, Search } from "lucide-react";
import { INSTRUMENTS, NO_FILTERS, PORTFOLIOS, RANGES, STRATEGIES, type Filters } from "@/lib/terminal/sample-data";

const unique = (values: string[]) => [...new Set(values)].sort();
const ASSETS = unique(INSTRUMENTS.map((i) => i.assetClass));
const MARKETS = unique(INSTRUMENTS.map((i) => i.market));
const EXCHANGES = unique(INSTRUMENTS.map((i) => i.exchange));

const field = "block min-h-11 w-full rounded-xl border border-term-field bg-term-bg px-3 text-sm text-term-text";

function Select({ id, label, value, onChange, options, any }: { id: string; label: string; value: string; onChange: (v: string) => void; options: { value: string; label: string }[]; any?: string }) {
  return (
    <div className="grid min-w-0 gap-1">
      <label htmlFor={id} className="text-xs font-semibold text-term-muted">
        {label}
      </label>
      <select id={id} value={value} onChange={(e) => onChange(e.target.value)} className={field}>
        {any !== undefined && <option value="">{any}</option>}
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </div>
  );
}

const plain = (xs: readonly string[]) => xs.map((x) => ({ value: x, label: x }));

/** The filters of the terminal. They narrow the sample trades, and every figure on the page follows them. */
export function FilterBar({ filters, onChange, shown, total }: { filters: Filters; onChange: (f: Filters) => void; shown: number; total: number }) {
  const [updated, setUpdated] = useState("not yet refreshed");
  const set = (patch: Partial<Filters>) => onChange({ ...filters, ...patch });
  const active = JSON.stringify(filters) !== JSON.stringify(NO_FILTERS);
  return (
    <form role="search" aria-label="Filter the terminal" onSubmit={(e) => e.preventDefault()} className="grid gap-3 rounded-2xl border border-term-line bg-term-panel p-4 backdrop-blur">
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-8">
        <Select id="f-asset" label="Asset" value={filters.asset} onChange={(v) => set({ asset: v })} options={plain(ASSETS)} any="All assets" />
        <Select id="f-market" label="Market" value={filters.market} onChange={(v) => set({ market: v })} options={plain(MARKETS)} any="All markets" />
        <Select id="f-exchange" label="Exchange" value={filters.exchange} onChange={(v) => set({ exchange: v })} options={plain(EXCHANGES)} any="All exchanges" />
        <Select id="f-range" label="Date range" value={filters.range} onChange={(v) => set({ range: v })} options={RANGES.map((r) => ({ value: r.value, label: r.label }))} />
        <Select id="f-portfolio" label="Portfolio" value={filters.portfolio} onChange={(v) => set({ portfolio: v })} options={plain(PORTFOLIOS)} any="All portfolios" />
        <Select id="f-strategy" label="Strategy" value={filters.strategy} onChange={(v) => set({ strategy: v })} options={plain(STRATEGIES)} any="All strategies" />
        <Select id="f-risk" label="Risk" value={filters.risk} onChange={(v) => set({ risk: v })} options={plain(["Low", "Medium", "High"])} any="All risk levels" />
        <div className="col-span-2 grid min-w-0 gap-1 md:col-span-4 xl:col-span-1">
          <label htmlFor="f-search" className="text-xs font-semibold text-term-muted">
            Search
          </label>
          <div className="relative">
            <Search aria-hidden="true" size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-term-muted" />
            <input id="f-search" type="search" value={filters.search} onChange={(e) => set({ search: e.target.value })} placeholder="Symbol or strategy" autoComplete="off" className={`${field} pl-9`} />
          </div>
        </div>
      </div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p role="status" aria-live="polite" className="text-sm text-term-muted">
          {shown} of {total} sample trades match
        </p>
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs text-term-muted">Updated: {updated}</span>
          <button type="button" disabled={!active} onClick={() => onChange(NO_FILTERS)} className="min-h-11 rounded-xl border border-term-field px-4 text-sm font-semibold hover:bg-term-panel-strong disabled:opacity-50">
            Clear filters
          </button>
          <button type="button" onClick={() => setUpdated(new Date().toLocaleTimeString("en-GB"))} className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-term-teal px-4 text-sm font-semibold text-term-bg hover:brightness-110">
            <RefreshCw aria-hidden="true" size={16} />
            Refresh
          </button>
        </div>
      </div>
    </form>
  );
}
