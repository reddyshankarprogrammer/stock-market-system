"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { NavIcon } from "./NavIcon";
import { NAV_ITEMS } from "./nav";

const isActive = (pathname: string, href: string) => pathname === href || pathname.startsWith(`${href}/`);

/** Sidebar from the lg breakpoint up, a fixed bottom bar below it. The same links in both, so the layout is the only difference. */
export function SidebarNav({ collapsed = false }: { collapsed?: boolean }) {
  const pathname = usePathname();
  return (
    <nav aria-label="Main" className="hidden lg:block">
      <ul className="grid gap-1">
        {NAV_ITEMS.map((item) => {
          const active = isActive(pathname, item.href);
          return (
            <li key={item.href}>
              <Link
                href={item.href}
                aria-current={active ? "page" : undefined}
                title={collapsed ? item.label : undefined}
                className={`flex min-h-11 items-center gap-3 rounded-brand px-3 text-sm font-semibold hover:bg-soft ${active ? "bg-soft text-link" : "text-ink"}`}
              >
                <NavIcon name={item.icon} />
                <span className={collapsed ? "sr-only" : ""}>{item.label}</span>
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}

export function BottomNav() {
  const pathname = usePathname();
  return (
    <nav aria-label="Main" className="fixed inset-x-0 bottom-0 z-20 border-t border-line bg-canvas pb-[env(safe-area-inset-bottom)] lg:hidden">
      <ul className="grid grid-cols-5">
        {NAV_ITEMS.map((item) => {
          const active = isActive(pathname, item.href);
          return (
            <li key={item.href}>
              <Link
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={`flex min-h-14 flex-col items-center justify-center gap-0.5 px-1 text-[0.7rem] font-semibold ${active ? "text-link" : "text-ink"}`}
              >
                <NavIcon name={item.icon} />
                <span className="max-w-full truncate">{item.label}</span>
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
