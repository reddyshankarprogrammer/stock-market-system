"use client";

import { useMemo, useState, type ReactNode } from "react";
import { ArrowDown, ArrowUp, ArrowUpDown } from "lucide-react";

export type Column<T> = {
  id: string;
  header: string;
  cell: (row: T) => ReactNode;
  /** A value to sort by; a column without it is not sortable. */
  sort?: (row: T) => string | number;
  numeric?: boolean;
  /** "gain" shows the cell in teal, "loss" in red (the text always carries the sign too). */
  tone?: (row: T) => "gain" | "loss" | undefined;
};

/** The table of every terminal page: sortable headers with aria-sort, paging, a scrollable focusable region, an empty state. */
export function DataTable<T>({ rows, columns, rowKey, label, pageSize = 10, initialSort, empty = "Nothing to show." }: { rows: T[]; columns: Column<T>[]; rowKey: (row: T) => string; label: string; pageSize?: number; initialSort?: { id: string; dir: "asc" | "desc" }; empty?: string }) {
  const [sort, setSort] = useState(initialSort ?? null);
  const [page, setPage] = useState(1);
  const sorted = useMemo(() => {
    const col = columns.find((c) => c.id === sort?.id);
    if (!col?.sort || !sort) return rows;
    const sign = sort.dir === "asc" ? 1 : -1;
    const by = col.sort;
    return [...rows].sort((a, b) => {
      const x = by(a), y = by(b);
      return (typeof x === "number" && typeof y === "number" ? x - y : String(x).localeCompare(String(y))) * sign;
    });
  }, [rows, columns, sort]);
  const pages = Math.max(1, Math.ceil(sorted.length / pageSize));
  const current = Math.min(page, pages);
  const shown = sorted.slice((current - 1) * pageSize, current * pageSize);

  if (rows.length === 0) {
    return (
      <p role="status" className="rounded-xl border border-term-line p-4 text-term-muted">
        {empty}
      </p>
    );
  }
  return (
    <div>
      <div role="region" aria-label={label} tabIndex={0} className="relative overflow-x-auto rounded-xl border border-term-line">
        <table className="w-full min-w-[40rem] text-left text-sm">
          <caption className="sr-only">
            {label}, {rows.length} rows{sort ? `, sorted by ${columns.find((c) => c.id === sort.id)?.header} ${sort.dir === "asc" ? "ascending" : "descending"}` : ""}
          </caption>
          <thead className="border-b border-term-line bg-term-panel-strong">
            <tr>
              {columns.map((c) => (
                <th key={c.id} scope="col" aria-sort={c.sort ? (sort?.id === c.id ? (sort.dir === "asc" ? "ascending" : "descending") : "none") : undefined} className={`p-0 ${c.numeric ? "text-right" : ""}`}>
                  {c.sort ? (
                    <button type="button" onClick={() => { setSort((s) => ({ id: c.id, dir: s?.id === c.id && s.dir === "desc" ? "asc" : "desc" })); setPage(1); }} className={`flex min-h-11 w-full items-center gap-1 whitespace-nowrap px-3 font-semibold hover:bg-term-panel-strong ${c.numeric ? "justify-end" : ""}`}>
                      {c.header}
                      {sort?.id === c.id ? sort.dir === "asc" ? <ArrowUp aria-hidden="true" size={14} /> : <ArrowDown aria-hidden="true" size={14} /> : <ArrowUpDown aria-hidden="true" size={14} className="text-term-muted" />}
                    </button>
                  ) : (
                    <span className="flex min-h-11 items-center whitespace-nowrap px-3 font-semibold">{c.header}</span>
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {shown.map((r) => (
              <tr key={rowKey(r)} className="border-b border-term-line last:border-0 hover:bg-term-panel">
                {columns.map((c) => {
                  const tone = c.tone?.(r);
                  return (
                    <td key={c.id} className={`whitespace-nowrap px-3 py-2.5 tabular-nums ${c.numeric ? "text-right" : ""} ${tone === "gain" ? "text-term-teal" : tone === "loss" ? "text-term-loss" : ""}`}>
                      {c.cell(r)}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {pages > 1 && (
        <nav aria-label={`${label}: pages`} className="mt-3 flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-term-muted" data-testid="range">
            Showing {(current - 1) * pageSize + 1} to {Math.min(current * pageSize, sorted.length)} of {sorted.length}
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
      )}
    </div>
  );
}
