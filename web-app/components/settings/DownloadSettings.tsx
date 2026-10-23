"use client";

import { useState } from "react";
import { RadioCard, SaveStatus, SettingsHeading } from "@/components/settings/controls";
import { FormAlert } from "@/components/ui/form";
import { usePreferences, useSavePreferences } from "@/lib/settings-hooks";
import { EXPORT_FORMATS } from "@/lib/settings";

export function DownloadSettings() {
  const prefs = usePreferences();
  const save = useSavePreferences();
  const [chosen, setChosen] = useState<string | null>(null); // shown at once, before the saved value comes back

  return (
    <div className="grid gap-6">
      <SettingsHeading title="Downloads">The format used when you export trades and statements.</SettingsHeading>
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
            <legend className="mb-1 font-display text-lg font-bold">Export format</legend>
            {EXPORT_FORMATS.map((f) => (
              <RadioCard
                key={f.code}
                name="exportFormat"
                value={f.code}
                label={f.label}
                text={f.available ? undefined : "Not available yet"}
                disabled={!f.available}
                checked={(chosen ?? prefs.data.exportFormat) === f.code}
                onChange={() => {
                  setChosen(f.code);
                  save.mutate({ exportFormat: f.code as "CSV" | "JSON" }, { onSettled: () => setChosen(null) });
                }}
              />
            ))}
          </fieldset>
          <p className="text-sm text-subtle">Downloaded files go to your browser&apos;s download folder. Choosing a folder is possible only in the desktop and mobile apps.</p>
          <SaveStatus mutation={save} />
        </>
      )}
    </div>
  );
}
