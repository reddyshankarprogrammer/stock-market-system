import type { ReactNode } from "react";
import { AS_OF } from "@/lib/terminal/sample-data";

export const panel = "rounded-2xl border border-term-line bg-term-panel p-4 backdrop-blur";
export const fieldClass = "block min-h-11 w-full rounded-xl border border-term-field bg-term-bg px-3 text-sm text-term-text";

/** Title, one line of description and the sample-data notice that every terminal page carries. */
export function PageFrame({ title, description, actions, children }: { title: string; description: string; actions?: ReactNode; children: ReactNode }) {
  return (
    <div className="grid grid-cols-1 gap-5">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-extrabold tracking-tight">{title}</h1>
          <p className="mt-1 text-sm text-term-muted">
            {description} As of {AS_OF}.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {actions}
          <p className="rounded-xl border border-term-teal px-3 py-2 text-sm font-semibold text-term-teal" role="note">
            Sample data: these are not your accounts or your trades
          </p>
        </div>
      </header>
      {children}
    </div>
  );
}

export function Stat({ label, value, hint, tone }: { label: string; value: string; hint?: string; tone?: "gain" | "loss" }) {
  return (
    <div className="min-w-0 rounded-2xl border border-term-line bg-term-panel p-4 backdrop-blur">
      <dt className="text-xs font-semibold text-term-muted">{label}</dt>
      <dd className={`mt-1 break-words font-display text-2xl font-extrabold tabular-nums ${tone === "gain" ? "text-term-teal" : tone === "loss" ? "text-term-loss" : ""}`}>{value}</dd>
      {hint && <dd className="text-xs text-term-muted">{hint}</dd>}
    </div>
  );
}

export const usd = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
export const usd0 = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 });
export const signedUsd = (n: number, digits = 2) => (n > 0 ? "+" : n < 0 ? "-" : "") + (digits === 0 ? usd0 : usd).format(Math.abs(n));
export const signedPct = (n: number) => `${n > 0 ? "+" : n < 0 ? "-" : ""}${Math.abs(n).toFixed(2)}%`;
export const tone = (n: number): "gain" | "loss" | undefined => (n > 0 ? "gain" : n < 0 ? "loss" : undefined);
