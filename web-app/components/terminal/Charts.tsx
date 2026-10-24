"use client";

import type { ReactNode } from "react";
import { Area, AreaChart, Bar, BarChart, Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { allocation, equityCurve, monthlyReturns, sectorPerformance, winLoss, type Trade } from "@/lib/terminal/sample-data";

const TEAL = "#00e5b0";
const LOSS = "#ff7a8a";
const MUTED = "#9aa7b4";
const PALETTE = ["#00e5b0", "#4fb6ff", "#b794f6", "#ffd166", "#ff9f6b", "#7ee0ff", "#f78fb3"];
const usd0 = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 });
const signed0 = (n: number) => (n > 0 ? "+" : n < 0 ? "-" : "") + usd0.format(Math.abs(n));
const tooltipStyle = { background: "#0f151c", border: "1px solid rgb(255 255 255 / 0.2)", borderRadius: 12, color: "#e6edf3", fontSize: 13 };

/** A chart is a picture: the same numbers are always available as a table for screen readers (and the picture is hidden from them). */
export function Figure({ id, title, note, height = 240, table, children }: { id: string; title: string; note?: string; height?: number; table: { head: string[]; rows: (string | number)[][] }; children: ReactNode }) {
  return (
    <figure aria-labelledby={`${id}-t`} className="min-w-0 rounded-2xl border border-term-line bg-term-panel p-4 backdrop-blur">
      <figcaption>
        <h3 id={`${id}-t`} className="font-display text-base font-bold">
          {title}
        </h3>
        {note && <p className="text-xs text-term-muted">{note}</p>}
      </figcaption>
      <div aria-hidden="true" className="mt-2 w-full" style={{ height }}>
        {children}
      </div>
      <div className="sr-only">
      <table>
        <caption>{title}, as a table</caption>
        <thead>
          <tr>
            {table.head.map((h) => (
              <th key={h} scope="col">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {table.rows.map((r, i) => (
            <tr key={i}>
              {r.map((c, j) => (j === 0 ? <th key={j} scope="row">{c}</th> : <td key={j}>{c}</td>))}
            </tr>
          ))}
        </tbody>
      </table>
      </div>
    </figure>
  );
}

const axis = { stroke: MUTED, fontSize: 11, tickLine: false, axisLine: { stroke: "rgb(255 255 255 / 0.14)" } } as const;

export function EquityCurve({ trades }: { trades: Trade[] }) {
  const data = equityCurve(trades, 365).filter((_, i) => i % 7 === 0 || i === 365);
  const first = data[0]?.equity ?? 0, last = data.at(-1)?.equity ?? 0;
  return (
    <Figure id="equity" title="Equity curve" note="Daily equity over 12 months, sampled weekly" table={{ head: ["Date", "Equity"], rows: data.filter((_, i) => i % 6 === 0).map((p) => [p.date, usd0.format(p.equity)]) }}>
      <ResponsiveContainer width="100%" height="100%" minWidth={0}>
        <AreaChart accessibilityLayer={false} data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          <defs>
            <linearGradient id="eq-fill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={last >= first ? TEAL : LOSS} stopOpacity={0.3} />
              <stop offset="100%" stopColor={last >= first ? TEAL : LOSS} stopOpacity={0} />
            </linearGradient>
          </defs>
          <XAxis dataKey="date" {...axis} minTickGap={48} tickFormatter={(d: string) => d.slice(5)} />
          <YAxis {...axis} width={56} domain={["auto", "auto"]} tickFormatter={(v: number) => `${Math.round(v / 1000)}k`} />
          <Tooltip contentStyle={tooltipStyle} formatter={(v) => usd0.format(Number(v))} labelStyle={{ color: MUTED }} />
          <Area type="monotone" dataKey="equity" stroke={last >= first ? TEAL : LOSS} strokeWidth={2} fill="url(#eq-fill)" isAnimationActive={false} />
        </AreaChart>
      </ResponsiveContainer>
    </Figure>
  );
}

export function MonthlyReturns({ trades }: { trades: Trade[] }) {
  const data = monthlyReturns(trades);
  return (
    <Figure id="monthly" title="Monthly returns" note="Profit and loss of trades closed in each month" table={{ head: ["Month", "Profit and loss"], rows: data.map((m) => [m.month, signed0(m.pnl)]) }}>
      <ResponsiveContainer width="100%" height="100%" minWidth={0}>
        <BarChart accessibilityLayer={false} data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          <XAxis dataKey="month" {...axis} tickFormatter={(m: string) => m.slice(5)} />
          <YAxis {...axis} width={56} tickFormatter={(v: number) => `${Math.round(v / 1000)}k`} />
          <Tooltip contentStyle={tooltipStyle} formatter={(v) => signed0(Number(v))} cursor={{ fill: "rgb(255 255 255 / 0.06)" }} />
          <Bar dataKey="pnl" radius={[4, 4, 0, 0]} isAnimationActive={false}>
            {data.map((m) => (
              <Cell key={m.month} fill={m.pnl >= 0 ? TEAL : LOSS} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </Figure>
  );
}

export function Donut({ id, title, note, data }: { id: string; title: string; note?: string; data: { name: string; value: number; label: string }[] }) {
  return (
    <Figure id={id} title={title} note={note} height={180} table={{ head: ["Name", "Share"], rows: data.map((d) => [d.name, d.label]) }}>
      <ResponsiveContainer width="100%" height="100%" minWidth={0}>
        <PieChart accessibilityLayer={false}>
          <Pie rootTabIndex={-1} data={data} dataKey="value" nameKey="name" innerRadius="58%" outerRadius="88%" paddingAngle={2} stroke="none" isAnimationActive={false}>
            {data.map((d, i) => (
              <Cell key={d.name} fill={PALETTE[i % PALETTE.length]} />
            ))}
          </Pie>
          <Tooltip contentStyle={tooltipStyle} formatter={(_v, name, item) => [(item.payload as { label: string }).label, String(name)]} />
        </PieChart>
      </ResponsiveContainer>
    </Figure>
  );
}

export function WinLoss({ trades }: { trades: Trade[] }) {
  const w = winLoss(trades);
  const ratio = w.losses ? (w.wins / w.losses).toFixed(2) : w.wins ? "all wins" : "-";
  return (
    <div className="grid grid-cols-1 gap-2">
      <Donut id="winloss" title="Win / loss ratio" note={`${w.wins} wins and ${w.losses} losses of ${w.closed} closed trades`} data={[{ name: "Wins", value: w.wins, label: `${w.wins} wins` }, { name: "Losses", value: w.losses, label: `${w.losses} losses` }]} />
      <p className="px-1 text-sm text-term-muted">
        Win/loss ratio <strong className="text-term-text" data-testid="winloss-ratio">{ratio}</strong>, win rate {w.winRate.toFixed(1)}%
      </p>
    </div>
  );
}

export function SectorPerformance({ trades }: { trades: Trade[] }) {
  const data = sectorPerformance(trades);
  return (
    <Figure id="sector" title="Sector performance" note="Profit and loss by sector, open and closed trades" height={Math.max(180, data.length * 34)} table={{ head: ["Sector", "Profit and loss"], rows: data.map((s) => [s.sector, signed0(s.pnl)]) }}>
      <ResponsiveContainer width="100%" height="100%" minWidth={0}>
        <BarChart accessibilityLayer={false} data={data} layout="vertical" margin={{ top: 4, right: 12, bottom: 0, left: 0 }}>
          <XAxis type="number" {...axis} tickFormatter={(v: number) => `${Math.round(v / 1000)}k`} />
          <YAxis type="category" dataKey="sector" {...axis} width={88} />
          <Tooltip contentStyle={tooltipStyle} formatter={(v) => signed0(Number(v))} cursor={{ fill: "rgb(255 255 255 / 0.06)" }} />
          <Bar dataKey="pnl" radius={4} isAnimationActive={false}>
            {data.map((s) => (
              <Cell key={s.sector} fill={s.pnl >= 0 ? TEAL : LOSS} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </Figure>
  );
}

export function Allocations({ trades }: { trades: Trade[] }) {
  const defs = [
    { id: "alloc-sector", title: "Sector allocation", key: "sector" as const },
    { id: "alloc-asset", title: "Asset allocation", key: "assetClass" as const },
    { id: "alloc-geo", title: "Geographic allocation", key: "region" as const },
  ];
  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
      {defs.map((d) => {
        const a = allocation(trades, d.key);
        return (
          <div key={d.id} className="grid grid-cols-1 gap-2" data-allocation={d.key}>
            <Donut id={d.id} title={d.title} note="Share of the open positions' value" data={a.map((x) => ({ name: x.name, value: x.value, label: `${x.percent.toFixed(1)}%` }))} />
            {a.length === 0 ? (
              <p className="px-1 text-sm text-term-muted">No open positions.</p>
            ) : (
              <ul className="grid gap-1 px-1 text-sm">
                {a.map((x, i) => (
                  <li key={x.name} className="flex items-center justify-between gap-2">
                    <span className="flex items-center gap-2">
                      <span aria-hidden="true" className="size-2.5 rounded-sm" style={{ background: PALETTE[i % PALETTE.length] }} />
                      {x.name}
                    </span>
                    <span className="tabular-nums text-term-muted">{x.percent.toFixed(1)}%</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        );
      })}
    </div>
  );
}

/** Bars by category, teal above zero and red below, with the numbers as a table. `format` writes a value as text. */
export function CategoryBars({ id, title, note, data, format, height }: { id: string; title: string; note?: string; data: { name: string; value: number }[]; format: (v: number) => string; height?: number }) {
  return (
    <Figure id={id} title={title} note={note} height={height ?? 220} table={{ head: ["Name", "Value"], rows: data.map((d) => [d.name, format(d.value)]) }}>
      <ResponsiveContainer width="100%" height="100%" minWidth={0}>
        <BarChart accessibilityLayer={false} data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          <XAxis dataKey="name" {...axis} interval={0} />
          <YAxis {...axis} width={56} tickFormatter={(v: number) => (Math.abs(v) >= 1000 ? `${Math.round(v / 1000)}k` : String(v))} />
          <Tooltip contentStyle={tooltipStyle} formatter={(v) => format(Number(v))} cursor={{ fill: "rgb(255 255 255 / 0.06)" }} />
          <Bar dataKey="value" radius={[4, 4, 0, 0]} isAnimationActive={false}>
            {data.map((d) => (
              <Cell key={d.name} fill={d.value >= 0 ? TEAL : LOSS} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </Figure>
  );
}

/** One or more lines over the same labels. */
export function LineSeries({ id, title, note, data, lines, format, height }: { id: string; title: string; note?: string; data: Record<string, string | number>[]; lines: { key: string; name: string; color?: string }[]; format: (v: number) => string; height?: number }) {
  return (
    <Figure id={id} title={title} note={note} height={height ?? 240} table={{ head: ["Label", ...lines.map((l) => l.name)], rows: data.filter((_, i) => i % Math.max(1, Math.floor(data.length / 8)) === 0).map((d) => [String(d.label), ...lines.map((l) => format(Number(d[l.key])))]) }}>
      <ResponsiveContainer width="100%" height="100%" minWidth={0}>
        <LineChart accessibilityLayer={false} data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          <XAxis dataKey="label" {...axis} minTickGap={40} />
          <YAxis {...axis} width={56} domain={["auto", "auto"]} />
          <Tooltip contentStyle={tooltipStyle} formatter={(v) => format(Number(v))} labelStyle={{ color: MUTED }} />
          {lines.map((l, i) => (
            <Line key={l.key} type="monotone" dataKey={l.key} name={l.name} stroke={l.color ?? PALETTE[i % PALETTE.length]} strokeWidth={2} dot={false} isAnimationActive={false} />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </Figure>
  );
}
