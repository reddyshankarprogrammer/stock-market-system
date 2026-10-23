"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

export const SETTINGS_CATEGORIES = [
  { href: "/settings/general", label: "General" },
  { href: "/settings/notifications", label: "Notifications" },
  { href: "/settings/security", label: "Security" },
  { href: "/settings/downloads", label: "Downloads" },
  { href: "/settings/about", label: "About" },
] as const;

/** Category list: a column on laptops, a row that scrolls sideways on phones (the row scrolls, the page does not). */
export function SettingsNav() {
  const pathname = usePathname();
  return (
    <nav aria-label="Settings" className="min-w-0 lg:sticky lg:top-8 lg:self-start">
      <ul className="flex gap-1 overflow-x-auto pb-1 lg:grid lg:overflow-visible lg:pb-0">
        {SETTINGS_CATEGORIES.map((c) => {
          const active = pathname === c.href;
          return (
            <li key={c.href} className="shrink-0">
              <Link
                href={c.href}
                aria-current={active ? "page" : undefined}
                className={`flex min-h-11 items-center rounded-brand px-4 text-sm font-semibold hover:bg-soft ${active ? "bg-soft text-link" : "text-ink"}`}
              >
                {c.label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
