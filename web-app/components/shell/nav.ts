// The signed-in navigation. One list drives the sidebar (laptop and desktop) and the bottom bar (phones).
export type NavItem = { href: string; label: string; icon: "home" | "accounts" | "trades" | "portfolio" | "settings" };

export const NAV_ITEMS: NavItem[] = [
  { href: "/dashboard", label: "Dashboard", icon: "home" },
  { href: "/accounts", label: "Accounts", icon: "accounts" },
  { href: "/trades", label: "Trades", icon: "trades" },
  { href: "/portfolio", label: "Portfolio", icon: "portfolio" },
  { href: "/settings", label: "Settings", icon: "settings" },
];

/** Paths that belong to the app shell: proxy.ts protects them and the shell highlights the matching item. */
export const APP_PATHS = NAV_ITEMS.map((i) => i.href);
