import type { ComponentProps, ReactNode } from "react";
import { buttonBase, buttonPrimary } from "@/components/ui/Button";

// Form building blocks for the app screens. Colours come from the brand palette via theme roles; the palette has no
// error red, so errors use a bold "!" badge, a heavier border and text (never colour alone).

export function ErrorText({ id, children }: { id: string; children: ReactNode }) {
  return (
    <p id={id} className="mt-1.5 flex items-start gap-2 text-sm font-medium text-ink">
      <span aria-hidden="true" className="mt-0.5 inline-flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-ink text-[10px] font-bold text-canvas">
        !
      </span>
      <span>
        <span className="sr-only">Error: </span>
        {children}
      </span>
    </p>
  );
}

type FieldProps = { id: string; label: string; error?: string; hint?: ReactNode; children: ReactNode };
export function Field({ id, label, error, hint, children }: FieldProps) {
  return (
    <div className="min-w-0">
      <label htmlFor={id} className="mb-1.5 block text-sm font-semibold">
        {label}
      </label>
      {children}
      {hint && !error && (
        <p id={`${id}-hint`} className="mt-1.5 text-xs text-subtle">
          {hint}
        </p>
      )}
      {error && <ErrorText id={`${id}-error`}>{error}</ErrorText>}
    </div>
  );
}

export const inputClass = (error?: string) =>
  `block min-h-11 w-full rounded-brand bg-panel px-3.5 py-2.5 text-base text-ink placeholder:text-subtle/70 ${
    error ? "border-2 border-ink" : "border border-line"
  } focus-visible:outline-2 focus-visible:outline-electric`;

type InputProps = ComponentProps<"input"> & { label: string; error?: string; hint?: ReactNode };

function describedBy(id: string, error?: string, hint?: ReactNode) {
  return error ? `${id}-error` : hint ? `${id}-hint` : undefined;
}

export function TextField({ label, error, hint, id, className = "", ...rest }: InputProps & { id: string }) {
  return (
    <Field id={id} label={label} error={error} hint={hint}>
      <input id={id} aria-invalid={error ? true : undefined} aria-describedby={describedBy(id, error, hint)} className={`${inputClass(error)} ${className}`} {...rest} />
    </Field>
  );
}

export function PasswordField({ label, error, hint, id, show, onToggle, ...rest }: InputProps & { id: string; show: boolean; onToggle: () => void }) {
  return (
    <Field id={id} label={label} error={error} hint={hint}>
      <div className="relative">
        <input
          id={id}
          type={show ? "text" : "password"}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(id, error, hint)}
          className={`${inputClass(error)} pr-20`}
          {...rest}
        />
        <button
          type="button"
          onClick={onToggle}
          aria-pressed={show}
          className="absolute right-1 top-1/2 flex min-h-11 min-w-11 -translate-y-1/2 items-center justify-center rounded-brand px-3 text-sm font-semibold text-link underline underline-offset-4 hover:decoration-2"
        >
          {show ? "Hide" : "Show"}
          <span className="sr-only"> password</span>
        </button>
      </div>
    </Field>
  );
}

export function SelectField({ label, error, id, children, ...rest }: ComponentProps<"select"> & { id: string; label: string; error?: string }) {
  return (
    <Field id={id} label={label} error={error}>
      <select id={id} aria-invalid={error ? true : undefined} aria-describedby={describedBy(id, error)} className={inputClass(error)} {...rest}>
        {children}
      </select>
    </Field>
  );
}

export function Spinner() {
  return <span aria-hidden="true" className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white motion-reduce:animate-none" />;
}

export function SubmitButton({ loading, children, loadingText }: { loading: boolean; children: ReactNode; loadingText: string }) {
  return (
    <button
      type="submit"
      disabled={loading}
      aria-busy={loading}
      className={`${buttonBase} ${buttonPrimary} w-full gap-2 disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0`}
    >
      {loading && <Spinner />}
      {loading ? loadingText : children}
    </button>
  );
}

/** Messages: "error" interrupts (role=alert); "success" and "info" are polite. */
export function FormAlert({ tone, title, children }: { tone: "error" | "success" | "info"; title?: string; children?: ReactNode }) {
  const border = tone === "success" ? "border-growth" : tone === "error" ? "border-ink" : "border-electric";
  const mark = tone === "success" ? "✓" : tone === "error" ? "!" : "i";
  const markClass = tone === "success" ? "bg-growth text-navy" : tone === "error" ? "bg-ink text-canvas" : "bg-electric text-white";
  return (
    <div role={tone === "error" ? "alert" : "status"} className={`flex gap-3 rounded-brand border-l-4 bg-soft p-4 text-sm ${border}`}>
      <span aria-hidden="true" className={`mt-0.5 inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-xs font-bold ${markClass}`}>
        {mark}
      </span>
      <div className="min-w-0">
        {title && <p className="font-semibold">{title}</p>}
        {children && <div className={title ? "mt-1 text-subtle" : ""}>{children}</div>}
      </div>
    </div>
  );
}
