"use client";

import { useSyncExternalStore } from "react";
import { THEME_KEY } from "@/lib/auth/constants";

// System / Light / Dark. "System" follows the OS; a pick is stored in localStorage and applied as html[data-theme].
// The inline script in app/(auth)/layout.tsx applies a stored pick before paint, so there is no flash.
type Theme = "system" | "light" | "dark";
const listeners = new Set<() => void>();

function read(): Theme {
  try {
    const t = localStorage.getItem(THEME_KEY);
    return t === "light" || t === "dark" ? t : "system";
  } catch {
    return "system";
  }
}
function subscribe(cb: () => void) {
  listeners.add(cb);
  window.addEventListener("storage", cb);
  return () => {
    listeners.delete(cb);
    window.removeEventListener("storage", cb);
  };
}
/** Apply and remember a theme in this browser (localStorage and html[data-theme]). Does not touch the server. */
export function applyTheme(t: Theme) {
  try {
    if (t === "system") localStorage.removeItem(THEME_KEY);
    else localStorage.setItem(THEME_KEY, t);
  } catch {
    /* storage blocked: the choice still applies for this page view */
  }
  if (t === "system") delete document.documentElement.dataset.theme;
  else document.documentElement.dataset.theme = t;
  listeners.forEach((l) => l());
}

export type { Theme };

/** The theme of this browser, kept in step with every other ThemeToggle and with applyTheme. */
export function useTheme(): Theme {
  return useSyncExternalStore(subscribe, read, () => "system" as Theme);
}

// onPick lets the signed-in shell save the choice to the account, so it follows the user to other devices.
export function ThemeToggle({ onPick }: { onPick?: (t: Theme) => void }) {
  const theme = useTheme();
  return (
    <div role="group" aria-label="Colour theme" className="inline-flex rounded-brand border border-line p-0.5">
      {(["system", "light", "dark"] as const).map((t) => (
        <button
          key={t}
          type="button"
          aria-pressed={theme === t}
          onClick={() => {
            applyTheme(t);
            onPick?.(t);
          }}
          className={`min-h-11 min-w-11 rounded-brand px-3 text-sm font-semibold capitalize transition-colors ${
            theme === t ? "bg-electric text-white" : "text-ink hover:bg-soft"
          }`}
        >
          {t}
        </button>
      ))}
    </div>
  );
}
