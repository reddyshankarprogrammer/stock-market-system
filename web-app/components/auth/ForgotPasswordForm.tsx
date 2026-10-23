"use client";

import { useState } from "react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { FormAlert, SubmitButton, TextField } from "@/components/ui/form";
import { useForgotPassword } from "@/lib/auth/auth-hooks";
import { AuthError } from "@/lib/auth/errors";
import { forgotSchema, type ForgotValues } from "@/lib/auth/schemas";

const linkClass = "inline-flex min-h-11 items-center font-semibold text-link underline underline-offset-4 hover:decoration-2";

export function ForgotPasswordForm() {
  const forgot = useForgotPassword();
  const [formError, setFormError] = useState<string | null>(null);
  const [sentTo, setSentTo] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotValues>({ resolver: zodResolver(forgotSchema), defaultValues: { email: "" } });

  async function onSubmit(values: ForgotValues) {
    setFormError(null);
    try {
      await forgot.mutateAsync(values);
      setSentTo(values.email);
    } catch (e) {
      setFormError(e instanceof AuthError ? e.message : "Something went wrong. Please try again.");
    }
  }

  if (sentTo) {
    // The same screen for every email: it never says whether an account exists.
    return (
      <div>
        <h1 className="font-display text-3xl font-extrabold tracking-tight">Check your email</h1>
        <div className="mt-6">
          <FormAlert tone="success" title="If an account exists, a reset link is on its way">
            If {sentTo} is registered with Quantiva, we have sent a link to choose a new password. It works once and expires in 30 minutes. Check your spam folder if you
            do not see it.
          </FormAlert>
        </div>
        <p className="mt-6 text-sm text-subtle">
          Nothing arrived after a few minutes?{" "}
          <button type="button" onClick={() => setSentTo(null)} className={linkClass}>
            Try again
          </button>
          . You can ask for a new link once a minute.
        </p>
        <Link href="/login" className={`${linkClass} mt-2 text-sm`}>
          Back to log in
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h1 className="font-display text-3xl font-extrabold tracking-tight">Forgot your password?</h1>
      <p className="mt-2 text-subtle">Enter your email and we will send you a link to choose a new one.</p>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-8 grid gap-5">
        {formError && (
          <FormAlert tone="error" title="We could not send the link">
            {formError}
          </FormAlert>
        )}
        <TextField id="email" label="Email" type="email" autoComplete="email" inputMode="email" autoCapitalize="none" spellCheck={false} error={errors.email?.message} {...register("email")} />
        <SubmitButton loading={forgot.isPending} loadingText="Sending…">
          Send Reset Link
        </SubmitButton>
      </form>

      <p className="mt-6 text-sm text-subtle">
        Remembered it?{" "}
        <Link href="/login" className={linkClass}>
          Log in
        </Link>
      </p>
    </div>
  );
}
