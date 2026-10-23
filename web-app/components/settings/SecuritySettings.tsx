"use client";

import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { PasswordStrength } from "@/components/auth/PasswordStrength";
import { SettingsHeading } from "@/components/settings/controls";
import { FormAlert, PasswordField, SubmitButton } from "@/components/ui/form";
import { AuthError } from "@/lib/auth/errors";
import { changePasswordSchema, type ChangePasswordValues } from "@/lib/settings";
import { useChangePassword } from "@/lib/settings-hooks";

// Features that need Keycloak or a device registry. Listed so nobody wonders where they are, and clearly not offered.
const planned = [
  ["Two-factor authentication", "An extra code when you log in."],
  ["Passkeys, Face login and fingerprint login", "Unlock Quantiva with this device."],
  ["Active sessions and devices", "See where you are signed in and sign other devices out."],
  ["Login history", "Review recent sign-ins to your account."],
];

function ChangePasswordForm() {
  const change = useChangePassword();
  const [show, setShow] = useState(false);
  const [formError, setFormError] = useState<{ message: string; field: "currentPassword" | null } | null>(null);
  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<ChangePasswordValues>({ resolver: zodResolver(changePasswordSchema), defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" } });
  const next = useWatch({ control, name: "newPassword" }) ?? "";

  async function onSubmit(values: ChangePasswordValues) {
    setFormError(null);
    try {
      await change.mutateAsync(values);
      reset();
    } catch (e) {
      const err = e instanceof AuthError ? e : new AuthError("UNKNOWN", "Something went wrong. Please try again.");
      setFormError({ message: err.message, field: err.code === "INVALID_CURRENT_PASSWORD" ? "currentPassword" : null });
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="grid gap-5">
      {change.isSuccess && !formError && (
        <FormAlert tone="success" title="Your password has been changed">
          You stay signed in on this device. Other devices will be asked to log in again.
        </FormAlert>
      )}
      {formError && (
        <FormAlert tone="error" title="We could not change your password">
          {formError.message}
        </FormAlert>
      )}
      <PasswordField id="currentPassword" label="Current password" autoComplete="current-password" show={show} onToggle={() => setShow((s) => !s)} error={errors.currentPassword?.message} {...register("currentPassword")} />
      <div>
        <PasswordField id="newPassword" label="New password" autoComplete="new-password" show={show} onToggle={() => setShow((s) => !s)} error={errors.newPassword?.message} hint="At least 10 characters, with a letter and a number." {...register("newPassword")} />
        <PasswordStrength value={next} />
      </div>
      <PasswordField id="confirmPassword" label="Confirm new password" autoComplete="new-password" show={show} onToggle={() => setShow((s) => !s)} error={errors.confirmPassword?.message} {...register("confirmPassword")} />
      <SubmitButton loading={change.isPending} loadingText="Changing password…">
        Change Password
      </SubmitButton>
    </form>
  );
}

export function SecuritySettings() {
  return (
    <div className="grid gap-8">
      <SettingsHeading title="Security">Keep your account safe.</SettingsHeading>
      <section aria-labelledby="change-password-heading" className="grid gap-4">
        <h2 id="change-password-heading" className="font-display text-lg font-bold">
          Change password
        </h2>
        <ChangePasswordForm />
      </section>
      <section aria-labelledby="planned-heading" className="grid gap-3">
        <h2 id="planned-heading" className="font-display text-lg font-bold">
          Coming later
        </h2>
        <ul className="divide-y divide-line rounded-brand border border-line px-4">
          {planned.map(([name, text]) => (
            <li key={name} className="flex items-start justify-between gap-4 py-3">
              <div>
                <p className="font-semibold">{name}</p>
                <p className="text-sm text-subtle">{text}</p>
              </div>
              <span className="shrink-0 rounded-brand border border-line px-2 py-0.5 text-xs font-semibold">Not available yet</span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
