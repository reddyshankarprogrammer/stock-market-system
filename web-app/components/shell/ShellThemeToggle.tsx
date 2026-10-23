"use client";

import { ThemeToggle, type Theme } from "@/components/auth/ThemeToggle";
import { useSavePreferences } from "@/lib/settings-hooks";

const toServer = { system: "SYSTEM", light: "LIGHT", dark: "DARK" } as const;

// The theme switch of the signed-in shell: applies the theme here and saves it to the account, so it follows the user.
export function ShellThemeToggle() {
  const save = useSavePreferences();
  return <ThemeToggle onPick={(t: Theme) => save.mutate({ theme: toServer[t] })} />;
}
