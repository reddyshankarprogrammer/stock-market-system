"use client";

import { useMemo, useRef, useState } from "react";
import { ArrowDown, ArrowUp, ArrowUpDown, Columns3, Download } from "lucide-react";
import { CSV_COLUMNS, tradesToCsv, type Trade } from "@/lib/terminal/sample-data";

type Col = { id: (typeof CSV_COLUMNS)[number]; sort: (t: Trade) => string | number; cell: (t: Trade) => string; numeric?: boolean };

const usd = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
const dateText = (d: string | null) => (d ? d : "-");

const COLS: Col[] = [
  { id: "Status", sort: (t) => t.status, cell: (t) => t.status },
  { id: "Entry date", sort: (t) => t.entryDate, cell: (t) => t.entryDate },
  { id: "Exit date", sort: (t) => t.exitDate ?? "", cell: (t) => dateText(t.exitDate) },
  { id: "Symbol", sort: (t) => t.symbol, cell: (t) => t.symbol },
  { id: "Company", sort: (t) => t.company, cell: (t) => t.company },
  { id: "Strategy", sort: (t) => t.strategy, cell: (t) => t.strategy },
  { id: "Buy price", sort: (t) => t.buyPrice, cell: (t) => usd.format(t.buyPrice), numeric: true },
  { id: "Sell price", sort: (t) => t.sellPrice ?? -1, cell: (t) => (t.sellPrice === null ? "-" : usd.format(t.sellPrice)), numeric: true },
  { id: "Quantity", sort: (t) => t.quantity, cell: (t) => String(t.quantity), numeric: true },
  { id: "Profit/Loss", sort: (t) => t.pnl, cell: (t) => (t.pnl > 0 ? "+" : t.pnl < 0 ? "-" : "") + usd.format(Math.abs(t.pnl)), numeric: true },
  { id: "ROI %", sort: (t) => t.roi, cell: (t) => `${t.roi > 0 ? "+" : ""}${t.roi.toFixed(2)}%`, numeric: true },
  { id: "Holding days", sort: (t) => t.holdingDays, cell: (t) => String(t.holdingDays), numeric: true },
];

const PAGE = 10;

export function TradingActivity({ trades }: { trades: Trade[] }) {
  const [status, setStatus] = useState<"All" | "Open" | "Closed">("All");
  const [sort, setSort] = useState<{ id: string; dir: "asc" | "desc" }>({ id: "Entry date", dir: "desc" });
  const [page, setPage] = useState(1);
  const [hidden, setHidden] = useState<string[]>([]);
  const [menu, setMenu] = useState(false);
  const columnsButton = useRef<HTMLButtonElement>(null);

  const visible = COLS.filter((c) => !hidden.includes(c.id));
  const rows = useMemo(() => {
    const col = COLS.find((c) => c.id === sort.id) ?? COLS[1];
    const list = trades.filter((t) => status === "All" || t.status === status);
    const sign = sort.dir === "asc" ? 1 : -1;
    return [...list].sort((a, b) => {
      const x = col.sort(a), y = col.sort(b);
      const c = typeof x === "number" && typeof y === "number" ? x - y : String(x).localeCompare(String(y));
      return (c || a.id.localeCompare(b.id)) * sign;
    });
  }, [trades, status, sort]);
  const pages = Math.max(1, Math.ceil(rows.length / PAGE));
  const current = Math.min(page, pages);
  const shown = rows.slice((current - 1) * PAGE, current * PAGE);

  const exportCsv = () => {
    // The file holds what the table holds: the chosen rows, sorted as shown, with the visible columns.
    const blob = new Blob([tradesToCsv(rows, visible.map((c) => c.id))], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "quantiva-sample-trades.csv";
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <section aria-labelledby="activity-title" className="rounded-2xl border border-term-line bg-term-panel p-4 backdrop-blur">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 id="activity-title" className="font-display text-lg font-bold">
          Trading activity
        </h2>
        <div className="flex flex-wrap items-center gap-2">
          <div role="group" aria-label="Status" className="inline-flex rounded-xl border border-term-field p-0.5">
            {(["All", "Open", "Closed"] as const).map((s) => (
              <button key={s} type="button" aria-pressed={status === s} onClick={() => { setStatus(s); setPage(1); }} className={`min-h-11 rounded-[10px] px-3 text-sm font-semibold ${status === s ? "bg-term-teal text-term-bg" : "text-term-text hover:bg-term-panel-strong"}`}>
                {s}
              </button>
            ))}
          </div>
          <div className="relative" onKeyDown={(e) => { if (e.key === "Escape" && menu) { setMenu(false); columnsButton.current?.focus(); } }}>
            <button ref={columnsButton} type="button" aria-expanded={menu} aria-controls="column-menu" onClick={() => setMenu((m) => !m)} className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-term-field px-3 text-sm font-semibold hover:bg-term-panel-strong">
              <Columns3 aria-hidden="true" size={16} />
              Columns
            </button>
            {menu && (
              <fieldset id="column-menu" className="absolute right-0 z-20 mt-2 grid w-56 gap-1 rounded-xl border border-term-line bg-term-bg p-3 shadow-2xl">
                <legend className="sr-only">Show columns</legend>
                {COLS.map((c) => (
                  <label key={c.id} className="flex min-h-9 items-center gap-2 text-sm">
                    <input type="checkbox" className="size-4 accent-[#00e5b0]" checked={!hidden.includes(c.id)} disabled={visible.length === 1 && !hidden.includes(c.id)} onChange={(e) => setHidden((h) => (e.target.checked ? h.filter((x) => x !== c.id) : [...h, c.id]))} />
                    {c.id}
                  </label>
                ))}
              </fieldset>
            )}
          </div>
          <button type="button" onClick={exportCsv} disabled={rows.length === 0} className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-term-field px-3 text-sm font-semibold hover:bg-term-panel-strong disabled:opacity-50">
            <Download aria-hidden="true" size={16} />
            Export CSV
          </button>
        </div>
      </div>

      {rows.length === 0 ? (
        <p role="status" className="mt-4 rounded-xl border border-term-line p-4 text-term-muted">
          No sample trades match the filters.
        </p>
      ) : (
        <>
          <div role="region" aria-label="Trading activity table" tabIndex={0} className="relative mt-4 overflow-x-auto rounded-xl border border-term-line">
            <table className="w-full min-w-[60rem] text-left text-sm">
              <caption className="sr-only">
                Sample trades, sorted by {sort.id} {sort.dir === "asc" ? "ascending" : "descending"}, {rows.length} rows
              </caption>
              <thead className="border-b border-term-line bg-term-panel-strong">
                <tr>
                  {visible.map((c) => (
                    <th key={c.id} scope="col" aria-sort={sort.id === c.id ? (sort.dir === "asc" ? "ascending" : "descending") : "none"} className={`p-0 ${c.numeric ? "text-right" : ""}`}>
                      <button type="button" onClick={() => { setSort((s) => ({ id: c.id, dir: s.id === c.id && s.dir === "desc" ? "asc" : "desc" })); setPage(1); }} className={`flex min-h-11 w-full items-center gap-1 whitespace-nowrap px-3 font-semibold hover:bg-term-panel-strong ${c.numeric ? "justify-end" : ""}`}>
                        {c.id}
                        {sort.id === c.id ? sort.dir === "asc" ? <ArrowUp aria-hidden="true" size={14} /> : <ArrowDown aria-hidden="true" size={14} /> : <ArrowUpDown aria-hidden="true" size={14} className="text-term-muted" />}
                      </button>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {shown.map((t) => (
                  <tr key={t.id} className="border-b border-term-line last:border-0 hover:bg-term-panel">
                    {visible.map((c) => (
                      <td key={c.id} className={`whitespace-nowrap px-3 py-2.5 tabular-nums ${c.numeric ? "text-right" : ""} ${c.id === "Profit/Loss" || c.id === "ROI %" ? (t.pnl >= 0 ? "text-term-teal" : "text-term-loss") : ""} ${c.id === "Symbol" ? "font-semibold" : ""}`}>
                        {c.cell(t)}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav aria-label="Trading activity pages" className="mt-3 flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-term-muted" data-testid="range">
              Showing {(current - 1) * PAGE + 1} to {Math.min(current * PAGE, rows.length)} of {rows.length}
            </p>
            <div className="flex items-center gap-2">
              <button type="button" disabled={current <= 1} onClick={() => setPage(current - 1)} className="min-h-11 rounded-xl border border-term-field px-4 text-sm font-semibold hover:bg-term-panel-strong disabled:opacity-50">
                Previous
              </button>
              <span className="text-sm">
                Page {current} of {pages}
              </span>
              <button type="button" disabled={current >= pages} onClick={() => setPage(current + 1)} className="min-h-11 rounded-xl border border-term-field px-4 text-sm font-semibold hover:bg-term-panel-strong disabled:opacity-50">
                Next
              </button>
            </div>
          </nav>
        </>
      )}
    </section>
  );
}
