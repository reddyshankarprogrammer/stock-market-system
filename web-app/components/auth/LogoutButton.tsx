"use client";

import { LogOut } from "lucide-react";
import { useLogout } from "@/lib/auth/auth-hooks";

export function LogoutButton({ compact = false }: { compact?: boolean }) {
  const logout = useLogout();
  return (
    <button
      type="button"
      onClick={() => logout.mutate()}
      disabled={logout.isPending}
      aria-label={compact ? "Log out" : undefined}
      title={compact ? "Log out" : undefined}
      className={`inline-flex min-h-11 items-center justify-center gap-2 rounded-brand border border-line text-sm font-semibold text-ink hover:bg-soft disabled:opacity-60 ${compact ? "min-w-11 px-0" : "px-4"}`}
    >
      {compact ? <LogOut aria-hidden="true" size={18} /> : logout.isPending ? "Logging out…" : "Log out"}
    </button>
  );
}
