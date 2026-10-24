"use client";

import { useEffect, useRef, useState, useSyncExternalStore, type ReactNode } from "react";
import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { ModeSwitch } from "@/components/shell/ModeSwitch";
import { Activity, BarChart3, BookOpen, Briefcase, CalendarClock, ChevronsLeft, ChevronsRight, FlaskConical, LayoutDashboard, LineChart, ListChecks, Menu, Percent, PieChart, ScanSearch, Settings, ShieldAlert, Sparkles, Target, X, type LucideIcon } from "lucide-react";

type Item = { label: string; icon: LucideIcon; href?: string };

// Dashboard, Markets, Portfolio, Positions, Orders and Journal exist. The other entries are shown, as the brief asks, but marked as not built yet.
const NAV: Item[] = [
  { label: "Dashboard", icon: LayoutDashboard, href: "/terminal" },
  { label: "Markets", icon: LineChart, href: "/terminal/markets" },
  { label: "Portfolio", icon: Briefcase, href: "/terminal/portfolio" },
  { label: "Positions", icon: Target, href: "/terminal/positions" },
  { label: "Orders", icon: ListChecks, href: "/terminal/orders" },
  { label: "Journal", icon: BookOpen, href: "/terminal/journal" },
  { label: "Analytics", icon: BarChart3 },
  { label: "AI Signals", icon: Sparkles },
  { label: "Screener", icon: ScanSearch },
  { label: "Option Analytics", icon: Percent },
  { label: "Futures Analytics", icon: CalendarClock },
  { label: "Strategy Builder", icon: FlaskConical },
  { label: "Backtesting", icon: Activity },
  { label: "Risk Management", icon: ShieldAlert },
  { label: "Reports", icon: PieChart },
  { label: "Settings", icon: Settings },
];

const KEY = "qv-terminal-collapsed";
const listeners = new Set<() => void>();
const read = () => {
  try {
    return localStorage.getItem(KEY) === "1";
  } catch {
    return false;
  }
};
const subscribe = (cb: () => void) => {
  listeners.add(cb);
  window.addEventListener("storage", cb);
  return () => {
    listeners.delete(cb);
    window.removeEventListener("storage", cb);
  };
};
function writeCollapsed(v: boolean) {
  try {
    localStorage.setItem(KEY, v ? "1" : "0");
  } catch {
    /* storage blocked: the choice still applies until the page is closed */
  }
  listeners.forEach((l) => l());
}

function NavList({ collapsed, onNavigate }: { collapsed: boolean; onNavigate?: () => void }) {
  const pathname = usePathname();
  return (
    <nav aria-label="Terminal">
      <ul className="grid gap-1">
        {NAV.map(({ label, icon: Icon, href }) => (
          <li key={label}>
            {href ? (
              <Link href={href} aria-current={pathname === href ? "page" : undefined} onClick={onNavigate} title={collapsed ? label : undefined} className={`flex min-h-11 items-center gap-3 rounded-xl px-3 text-sm font-semibold ${pathname === href ? "bg-term-panel-strong text-term-teal" : "text-term-text hover:bg-term-panel-strong"}`}>
                <Icon aria-hidden="true" size={20} />
                <span className={collapsed ? "sr-only" : ""}>{label}</span>
              </Link>
            ) : (
              <button type="button" aria-disabled="true" title={`${label}: not built yet`} onClick={(e) => e.preventDefault()} className="flex min-h-11 w-full cursor-not-allowed items-center gap-3 rounded-xl px-3 text-left text-sm text-term-muted">
                <Icon aria-hidden="true" size={20} />
                <span className={collapsed ? "sr-only" : "flex flex-1 items-center justify-between gap-2"}>
                  {label}
                  {!collapsed && <span className="rounded-md border border-term-line px-1.5 text-[0.65rem] uppercase tracking-wide">Soon</span>}
                </span>
                {collapsed && <span className="sr-only"> (not built yet)</span>}
              </button>
            )}
          </li>
        ))}
      </ul>
    </nav>
  );
}

function Brand({ collapsed }: { collapsed: boolean }) {
  return (
    <Link href="/dashboard" aria-label="Quantiva home" className="flex min-h-11 items-center gap-2.5">
      <span className="rounded-xl bg-white p-1">
        <Image src="/brand/quantiva-icon-192.png" alt="" width={28} height={28} />
      </span>
      <span className={collapsed ? "sr-only" : "font-display text-lg font-extrabold tracking-tight"}>Quantiva</span>
    </Link>
  );
}

/** Dark-only terminal frame: collapsible sidebar from 1024 px, a drawer below. */
export function TerminalShell({ children }: { children: ReactNode }) {
  const collapsed = useSyncExternalStore(subscribe, read, () => false);
  const [open, setOpen] = useState(false);
  const drawerRef = useRef<HTMLDivElement>(null);
  const opener = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;
    const close = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setOpen(false);
        opener.current?.focus();
      }
    };
    window.addEventListener("keydown", close);
    drawerRef.current?.querySelector<HTMLElement>("a, button")?.focus();
    return () => window.removeEventListener("keydown", close);
  }, [open]);

  return (
    <div className="min-h-dvh bg-term-bg text-term-text [color-scheme:dark] lg:flex">
      <aside className={`sticky top-0 hidden h-dvh shrink-0 flex-col gap-4 overflow-y-auto border-r border-term-line bg-term-panel p-3 transition-[width] duration-200 motion-reduce:transition-none lg:flex ${collapsed ? "w-[4.5rem]" : "w-64"}`}>
        <Brand collapsed={collapsed} />
        <ModeSwitch mode="terminal" dark compact={collapsed} />
        <NavList collapsed={collapsed} />
        <button type="button" onClick={() => writeCollapsed(!collapsed)} aria-pressed={collapsed} aria-label={collapsed ? "Expand the sidebar" : "Collapse the sidebar"} className="mt-auto flex min-h-11 items-center gap-3 rounded-xl px-3 text-sm text-term-muted hover:bg-term-panel-strong hover:text-term-text">
          {collapsed ? <ChevronsRight aria-hidden="true" size={20} /> : <ChevronsLeft aria-hidden="true" size={20} />}
          <span className={collapsed ? "sr-only" : ""}>Collapse</span>
        </button>
      </aside>

      <div className="min-w-0 flex-1">
        <header className="flex items-center justify-between gap-3 border-b border-term-line px-4 py-2 lg:hidden">
          <Brand collapsed />
          <ModeSwitch mode="terminal" dark />
          <button ref={opener} type="button" onClick={() => setOpen(true)} aria-label="Open the menu" aria-expanded={open} className="inline-flex min-h-11 min-w-11 items-center justify-center rounded-xl border border-term-field">
            <Menu aria-hidden="true" size={22} />
          </button>
        </header>
        {open && (
          <div className="fixed inset-0 z-40 lg:hidden">
            <div className="absolute inset-0 bg-black/70" onClick={() => setOpen(false)} aria-hidden="true" />
            <div ref={drawerRef} role="dialog" aria-modal="true" aria-label="Terminal menu" className="absolute inset-y-0 left-0 grid w-72 max-w-[85vw] content-start gap-4 overflow-y-auto border-r border-term-line bg-term-bg p-3">
              <div className="flex items-center justify-between">
                <Brand collapsed={false} />
                <button type="button" onClick={() => { setOpen(false); opener.current?.focus(); }} aria-label="Close the menu" className="inline-flex min-h-11 min-w-11 items-center justify-center rounded-xl border border-term-field">
                  <X aria-hidden="true" size={22} />
                </button>
              </div>
              <ModeSwitch mode="terminal" dark />
              <NavList collapsed={false} onNavigate={() => setOpen(false)} />
            </div>
          </div>
        )}
        <main className="mx-auto w-full max-w-[1800px] px-4 py-6 sm:px-6">{children}</main>
      </div>
    </div>
  );
}
