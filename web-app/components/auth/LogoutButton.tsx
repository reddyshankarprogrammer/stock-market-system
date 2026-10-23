"use client";

import { useLogout } from "@/lib/auth/auth-hooks";

export function LogoutButton() {
  const logout = useLogout();
  return (
    <button
      type="button"
      onClick={() => logout.mutate()}
      disabled={logout.isPending}
      className="min-h-11 rounded-brand border border-line px-4 text-sm font-semibold text-ink hover:bg-soft disabled:opacity-60"
    >
      {logout.isPending ? "Logging out…" : "Log out"}
    </button>
  );
}
