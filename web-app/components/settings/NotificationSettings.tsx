"use client";

import { SaveStatus, SettingsHeading, Switch } from "@/components/settings/controls";
import { FormAlert } from "@/components/ui/form";
import { usePreferences, useSavePreferences } from "@/lib/settings-hooks";

export function NotificationSettings() {
  const prefs = usePreferences();
  const save = useSavePreferences();

  return (
    <div className="grid gap-6">
      <SettingsHeading title="Notifications">Choose how Quantiva may reach you. Nothing is sent to a channel you have not switched on.</SettingsHeading>
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
          <div className="divide-y divide-line rounded-brand border border-line px-4">
            <Switch id="notify-in-app" label="In-app notifications" description="Messages inside Quantiva, such as imports and trade results." checked={prefs.data.notifyInApp} onChange={(v) => save.mutate({ notifyInApp: v })} />
            <Switch id="notify-push" label="Push notifications" description="Alerts on your phone or browser. Delivery arrives with the mobile app; your choice is saved now." checked={prefs.data.notifyPush} onChange={(v) => save.mutate({ notifyPush: v })} />
            <Switch id="notify-email" label="Email alerts" description="Alerts by email. No alert emails are sent yet; your choice is saved for when they are." checked={prefs.data.notifyEmail} onChange={(v) => save.mutate({ notifyEmail: v })} />
          </div>
          <SaveStatus mutation={save} />
        </>
      )}
    </div>
  );
}
