"use client";

import { useMemo, useState } from "react";
import { Search } from "lucide-react";
import { DataTable, type Column } from "@/components/terminal/DataTable";
import { Spark } from "@/components/terminal/KpiCards";
import { PageFrame, Stat, fieldClass, panel, signedPct, tone, usd } from "@/components/terminal/PageFrame";
import { INDICES, QUOTES, topMovers, type Quote } from "@/lib/terminal/sample-extra";

const markets = [...new Set(QUOTES.map((q) => q.market))].sort();
const sectors = [...new Set(QUOTES.map((q) => q.sector))].sort();
const num = new Intl.NumberFormat("en-US");

const columns: Column<Quote>[] = [
  { id: "symbol", header: "Symbol", cell: (q) => <strong>{q.symbol}</strong>, sort: (q) => q.symbol },
  { id: "name", header: "Name", cell: (q) => q.name, sort: (q) => q.name },
  { id: "price", header: "Price", cell: (q) => usd.format(q.price), sort: (q) => q.price, numeric: true },
  { id: "change", header: "Change", cell: (q) => `${q.change > 0 ? "+" : q.change < 0 ? "-" : ""}${Math.abs(q.change).toFixed(2)}`, sort: (q) => q.change, numeric: true, tone: (q) => tone(q.change) },
  { id: "pct", header: "Change %", cell: (q) => signedPct(q.changePercent), sort: (q) => q.changePercent, numeric: true, tone: (q) => tone(q.changePercent) },
  { id: "volume", header: "Volume", cell: (q) => num.format(q.volume), sort: (q) => q.volume, numeric: true },
  { id: "range", header: "Day range", cell: (q) => `${q.dayLow.toFixed(2)} - ${q.dayHigh.toFixed(2)}`, numeric: true },
  { id: "market", header: "Market", cell: (q) => `${q.market} · ${q.exchange}`, sort: (q) => q.market },
  {
    id: "trend", header: "30 days",
    cell: (q) => (
      <span className="block w-28">
        <Spark data={q.series} positive={q.series[q.series.length - 1] >= q.series[0]} id={`m-${q.symbol}`} />
        <span className="sr-only">{signedPct(((q.series[q.series.length - 1] - q.series[0]) / q.series[0]) * 100)} over 30 days</span>
      </span>
    ),
  },
];

export function MarketsPage() {
  const [market, setMarket] = useState("");
  const [sector, setSector] = useState("");
  const [search, setSearch] = useState("");
  const rows = useMemo(() => QUOTES.filter((q) => (!market || q.market === market) && (!sector || q.sector === sector) && (!search || `${q.symbol} ${q.name}`.toLowerCase().includes(search.toLowerCase()))), [market, sector, search]);
  const { gainers, losers } = topMovers(rows.length ? rows : QUOTES, 3);

  return (
    <PageFrame title="Markets" description="Index levels, quotes and the day's biggest movers.">
      <dl aria-label="Indices (sample data)" className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {INDICES.map((i) => (
          <div key={i.symbol} className="min-w-0 rounded-2xl border border-term-line bg-term-panel p-4 backdrop-blur">
            <dt className="text-xs font-semibold text-term-muted">{i.name}</dt>
            <dd className="mt-1 font-display text-2xl font-extrabold tabular-nums">{new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 }).format(i.price)}</dd>
            <dd className={`text-sm font-semibold tabular-nums ${i.changePercent >= 0 ? "text-term-teal" : "text-term-loss"}`}>
              <span aria-hidden="true">{i.changePercent >= 0 ? "▲" : "▼"} </span>
              {signedPct(i.changePercent)}
            </dd>
            <Spark data={i.series} positive={i.changePercent >= 0} id={`i-${i.symbol}`} />
          </div>
        ))}
      </dl>

      <form role="search" aria-label="Filter the quotes" onSubmit={(e) => e.preventDefault()} className={`${panel} grid grid-cols-1 gap-3 sm:grid-cols-3`}>
        <div className="grid gap-1">
          <label htmlFor="q-market" className="text-xs font-semibold text-term-muted">Market</label>
          <select id="q-market" value={market} onChange={(e) => setMarket(e.target.value)} className={fieldClass}>
            <option value="">All markets</option>
            {markets.map((m) => <option key={m}>{m}</option>)}
          </select>
        </div>
        <div className="grid gap-1">
          <label htmlFor="q-sector" className="text-xs font-semibold text-term-muted">Sector</label>
          <select id="q-sector" value={sector} onChange={(e) => setSector(e.target.value)} className={fieldClass}>
            <option value="">All sectors</option>
            {sectors.map((s) => <option key={s}>{s}</option>)}
          </select>
        </div>
        <div className="grid gap-1">
          <label htmlFor="q-search" className="text-xs font-semibold text-term-muted">Search</label>
          <div className="relative">
            <Search aria-hidden="true" size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-term-muted" />
            <input id="q-search" type="search" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Symbol or name" autoComplete="off" className={`${fieldClass} pl-9`} />
          </div>
        </div>
      </form>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_18rem]">
        <section aria-labelledby="quotes-title" className={`${panel} min-w-0`}>
          <h2 id="quotes-title" className="mb-3 font-display text-lg font-bold">Quotes</h2>
          <p role="status" aria-live="polite" className="mb-2 text-sm text-term-muted">{rows.length} of {QUOTES.length} instruments</p>
          <DataTable rows={rows} columns={columns} rowKey={(q) => q.symbol} label="Quotes table" pageSize={8} initialSort={{ id: "pct", dir: "desc" }} empty="No instruments match the filters." />
        </section>
        <div className="grid min-w-0 grid-cols-1 content-start gap-4">
          {[["Top gainers", gainers], ["Top losers", losers]].map(([title, list]) => (
            <section key={title as string} aria-label={title as string} className={panel}>
              <h2 className="font-display text-base font-bold">{title as string}</h2>
              <ul className="mt-2 grid gap-1 text-sm">
                {(list as Quote[]).map((q) => (
                  <li key={q.symbol} className="flex items-center justify-between gap-2">
                    <span><strong>{q.symbol}</strong> <span className="text-term-muted">{usd.format(q.price)}</span></span>
                    <span className={`tabular-nums font-semibold ${q.changePercent >= 0 ? "text-term-teal" : "text-term-loss"}`}>{signedPct(q.changePercent)}</span>
                  </li>
                ))}
              </ul>
            </section>
          ))}
          <dl className="grid grid-cols-1 gap-3">
            <Stat label="Advancing" value={String(rows.filter((q) => q.changePercent > 0).length)} hint="instruments up today" tone="gain" />
            <Stat label="Declining" value={String(rows.filter((q) => q.changePercent < 0).length)} hint="instruments down today" tone="loss" />
          </dl>
        </div>
      </div>
    </PageFrame>
  );
}
