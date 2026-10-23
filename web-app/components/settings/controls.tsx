"use client";

import type { ReactNode } from "react";
import { FormAlert } from "@/components/ui/form";

/** An on/off setting. A real switch for assistive technology (role=switch, aria-checked), 44 px high. */
export function Switch({ id, label, description, checked, onChange, disabled }: { id: string; label: string; description?: ReactNode; checked: boolean; onChange: (next: boolean) => void; disabled?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-4 py-3">
      <div className="min-w-0">
        <p id={`${id}-label`} className="font-semibold">
          {label}
        </p>
        {description && (
          <p id={`${id}-desc`} className="mt-0.5 text-sm text-subtle">
            {description}
          </p>
        )}
      </div>
      <button
        type="button"
        role="switch"
        id={id}
        aria-checked={checked}
        aria-labelledby={`${id}-label`}
        aria-describedby={description ? `${id}-desc` : undefined}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={`relative inline-flex h-11 w-16 shrink-0 items-center rounded-full border border-line px-1 transition-colors disabled:opacity-60 ${checked ? "bg-electric" : "bg-soft"}`}
      >
        <span aria-hidden="true" className={`size-8 rounded-full bg-white shadow-sm transition-transform motion-reduce:transition-none ${checked ? "translate-x-6" : ""}`} />
        <span className="sr-only">{checked ? "On" : "Off"}</span>
      </button>
    </div>
  );
}

/** One choice out of a few, as native radios (keyboard and screen-reader friendly). */
export function RadioCard({ name, value, checked, onChange, label, text, disabled }: { name: string; value: string; checked: boolean; onChange: () => void; label: string; text?: string; disabled?: boolean }) {
  return (
    <label className={`flex min-h-11 items-center gap-3 rounded-brand border px-4 py-2 has-[:focus-visible]:outline has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-link ${checked ? "border-link bg-soft" : "border-line"} ${disabled ? "opacity-60" : "cursor-pointer hover:bg-soft"}`}>
      <input type="radio" name={name} value={value} checked={checked} onChange={onChange} disabled={disabled} className="size-5 accent-electric" />
      <span>
        <span className="block font-semibold">{label}</span>
        {text && <span className="block text-sm text-subtle">{text}</span>}
      </span>
    </label>
  );
}

type Status = { isPending: boolean; isSuccess: boolean; isError: boolean; error: Error | null };

/** "Saving…" / "Saved" / the error, for the last save of a settings screen. Takes a TanStack mutation as it is. */
export function SaveStatus({ mutation }: { mutation: Status }) {
  if (mutation.isError) {
    return (
      <FormAlert tone="error" title="We could not save that">
        {mutation.error?.message} Your previous choice is back.
      </FormAlert>
    );
  }
  return (
    <p role="status" aria-live="polite" className="min-h-6 text-sm text-subtle">
      {mutation.isPending ? "Saving…" : mutation.isSuccess ? "Saved" : ""}
    </p>
  );
}

export function SettingsHeading({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div>
      <h1 className="font-display text-3xl font-extrabold tracking-tight">{title}</h1>
      {children && <p className="mt-2 text-subtle">{children}</p>}
    </div>
  );
}
