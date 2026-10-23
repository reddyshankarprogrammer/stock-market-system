"use client";

import { useState } from "react";
import { RadioCard, SaveStatus, SettingsHeading } from "@/components/settings/controls";
import { FormAlert } from "@/components/ui/form";
import { applyTheme } from "@/components/auth/ThemeToggle";
import { usePreferences, useSavePreferences } from "@/lib/settings-hooks";
import { THEMES, type Preferences } from "@/lib/settings";

const toLocal = { SYSTEM: "system", LIGHT: "light", DARK: "dark" } as const;

export function GeneralSettings() {
  const prefs = usePreferences();
  const save = useSavePreferences();
  const [chosen, setChosen] = useState<string | null>(null); // shown at once, before the saved value comes back

  return (
    <div className="grid gap-6">
      <SettingsHeading title="General">Appearance and language. Your choices are saved to your account and follow you to other devices.</SettingsHeading>
      {prefs.isPending ? (
        <p role="status" aria-busy="true" className="text-subtle">
          Loading your settings…
        </p>
      ) : prefs.isError ? (
        <div className="grid gap-4">
          <FormAlert tone="error" title="We could not load your settings">
            {prefs.error.message}
          </FormAlert>
          <button type="button" onClick={() => prefs.refetch()} className="min-h-11 w-fit rounded-brand border border-link px-4 text-sm font-semibold text-link hover:bg-link hover:text-canvas">
            Try again
          </button>
        </div>
      ) : (
        <>
          <fieldset className="grid gap-2">
            <legend className="mb-1 font-display text-lg font-bold">Theme</legend>
            {THEMES.map((t) => (
              <RadioCard
                key={t.code}
                name="theme"
                value={t.code}
                label={t.label}
                text={t.text}
                checked={(chosen ?? prefs.data.theme) === t.code}
                onChange={() => {
                  applyTheme(toLocal[t.code]); // the page changes at once; the account is updated next
                  setChosen(t.code);
                  save.mutate({ theme: t.code as Preferences["theme"] }, { onSettled: () => setChosen(null) });
                }}
              />
            ))}
          </fieldset>
          <div>
            <h2 className="font-display text-lg font-bold">Language</h2>
            <p className="mt-1">English</p>
            <p className="text-sm text-subtle">More languages are planned.</p>
          </div>
          <SaveStatus mutation={save} />
        </>
      )}
    </div>
  );
}
