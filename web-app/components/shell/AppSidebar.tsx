"use client";

import { Suspense } from "react";
import { ChevronsLeft, ChevronsRight } from "lucide-react";
import { Brand } from "@/components/auth/Brand";
import { LogoutButton } from "@/components/auth/LogoutButton";
import { SidebarNav } from "@/components/shell/AppNav";
import { ModeSwitch } from "@/components/shell/ModeSwitch";
import { ShellThemeToggle } from "@/components/shell/ShellThemeToggle";
import { useCollapsed } from "@/lib/use-collapsed";

/** The left menu of the signed-in app from 1024 px up. It collapses to icons and the choice is remembered, like the terminal's. */
export function AppSidebar() {
  const [collapsed, setCollapsed] = useCollapsed("qv-app-sidebar-collapsed");
  return (
    <aside aria-label="App menu" className={`hidden shrink-0 border-r border-line transition-[width] duration-200 motion-reduce:transition-none lg:block ${collapsed ? "w-[4.5rem]" : "w-60"}`}>
      <div className="sticky top-0 flex h-dvh flex-col gap-5 overflow-y-auto overflow-x-hidden p-3">
        <Brand compact={collapsed} />
        <ModeSwitch mode="app" compact={collapsed} />
        {/* usePathname() needs a boundary on routes with a dynamic segment (/accounts/[id]) */}
        <Suspense fallback={null}>
          <SidebarNav collapsed={collapsed} />
        </Suspense>
        <div className="mt-auto grid gap-3">
          {!collapsed && <ShellThemeToggle />}
          <LogoutButton compact={collapsed} />
          <button
            type="button"
            onClick={() => setCollapsed(!collapsed)}
            aria-pressed={collapsed}
            aria-label={collapsed ? "Expand the menu" : "Collapse the menu"}
            className="flex min-h-11 items-center gap-3 rounded-brand px-3 text-sm font-semibold text-subtle hover:bg-soft hover:text-ink"
          >
            {collapsed ? <ChevronsRight aria-hidden="true" size={20} /> : <ChevronsLeft aria-hidden="true" size={20} />}
            <span className={collapsed ? "sr-only" : ""}>Collapse</span>
          </button>
        </div>
      </div>
    </aside>
  );
}
