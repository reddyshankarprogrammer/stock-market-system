"use client";

import { useEffect } from "react";
import { applyTheme } from "@/components/auth/ThemeToggle";
import { usePreferences } from "@/lib/settings-hooks";

const toLocal = { SYSTEM: "system", LIGHT: "light", DARK: "dark" } as const;

// Renders nothing. When the account's settings arrive, the account's theme wins over this browser's, so a choice made
// on another device applies here too. A failed load is ignored: the browser keeps its own theme.
export function PreferencesSync() {
  const theme = usePreferences().data?.theme;
  useEffect(() => {
    if (theme) applyTheme(toLocal[theme]);
  }, [theme]);
  return null;
}
