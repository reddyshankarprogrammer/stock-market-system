"use client";

import { Area, AreaChart, ResponsiveContainer } from "recharts";
import type { Kpi } from "@/lib/terminal/sample-data";

// Each card: large metric, trend (a sign as well as a colour), a sparkline and a teal accent line on the right.
export function Spark({ data, positive, id }: { data: number[]; positive: boolean; id: string }) {
  const color = positive ? "#00e5b0" : "#ff7a8a";
  const points = data.map((v, i) => ({ i, v }));
  return (
    <div aria-hidden="true" className="h-10 w-full">
      <ResponsiveContainer width="100%" height="100%" minWidth={0}>
        <AreaChart accessibilityLayer={false} data={points} margin={{ top: 2, right: 0, bottom: 0, left: 0 }}>
          <defs>
            <linearGradient id={`spark-${id}`} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={color} stopOpacity={0.35} />
              <stop offset="100%" stopColor={color} stopOpacity={0} />
            </linearGradient>
          </defs>
          <Area type="monotone" dataKey="v" stroke={color} strokeWidth={1.5} fill={`url(#spark-${id})`} isAnimationActive={false} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

export function KpiCards({ kpis }: { kpis: Kpi[] }) {
  return (
    <ul aria-label="Key figures (sample data)" className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4 2xl:grid-cols-7">
      {kpis.map((k) => {
        const up = k.trend >= 0;
        return (
          <li key={k.key} data-kpi={k.key} className="relative min-w-0 overflow-hidden rounded-2xl border border-term-line bg-term-panel p-4 pr-5 backdrop-blur">
            <span aria-hidden="true" className="absolute inset-y-3 right-0 w-1 rounded-l-full bg-term-teal" />
            <p className="text-xs font-semibold text-term-muted">{k.label}</p>
            <p className="mt-1 break-words font-display text-2xl font-extrabold tabular-nums" data-testid={`kpi-${k.key}`}>
              {k.display}
            </p>
            <p className={`mt-0.5 text-sm font-semibold tabular-nums ${up ? "text-term-teal" : "text-term-loss"}`}>
              <span aria-hidden="true">{up ? "▲" : "▼"} </span>
              <span className="sr-only">{up ? "Up" : "Down"} </span>
              {Math.abs(k.trend).toFixed(2)}%
            </p>
            <p className="text-xs text-term-muted">{k.hint}</p>
            <Spark data={k.spark} positive={up} id={k.key} />
          </li>
        );
      })}
    </ul>
  );
}
