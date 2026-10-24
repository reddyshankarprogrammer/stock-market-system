import Link from "next/link";
import { LayoutDashboard, SquareTerminal } from "lucide-react";

/**
 * Switch between the two ways of using Quantiva after sign-in: the app (accounts, trades, settings) and the trading
 * terminal (sample-data preview). Shown in both shells; the current mode is marked with aria-current.
 * `dark` is for the terminal's black surface, `compact` shows icons only (collapsed sidebar).
 */
export function ModeSwitch({ mode, dark = false, compact = false }: { mode: "app" | "terminal"; dark?: boolean; compact?: boolean }) {
  const items = [
    { id: "app", label: "App", href: "/dashboard", Icon: LayoutDashboard },
    { id: "terminal", label: "Terminal", href: "/terminal", Icon: SquareTerminal },
  ] as const;
  const frame = dark ? "border-term-field" : "border-line";
  const active = dark ? "bg-term-teal text-term-bg" : "bg-electric text-white";
  const idle = dark ? "text-term-text hover:bg-term-panel-strong" : "text-ink hover:bg-soft";
  return (
    <nav aria-label="Mode" className={`inline-flex rounded-xl border p-0.5 ${frame} ${compact ? "flex-col" : ""}`}>
      {items.map(({ id, label, href, Icon }) => (
        <Link
          key={id}
          href={href}
          aria-current={mode === id ? "page" : undefined}
          title={compact ? label : undefined}
          className={`inline-flex min-h-11 items-center justify-center gap-2 rounded-[10px] px-3 text-sm font-semibold ${compact ? "min-w-11" : "flex-1"} ${mode === id ? active : idle}`}
        >
          <Icon aria-hidden="true" size={16} />
          <span className={compact ? "sr-only" : ""}>{label}</span>
        </Link>
      ))}
    </nav>
  );
}
