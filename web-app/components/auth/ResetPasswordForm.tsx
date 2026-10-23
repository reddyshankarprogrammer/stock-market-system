"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { PasswordStrength } from "@/components/auth/PasswordStrength";
import { FormAlert, PasswordField, SubmitButton } from "@/components/ui/form";
import { useResetPassword } from "@/lib/auth/auth-hooks";
import { AuthError } from "@/lib/auth/errors";
import { resetSchema, type ResetValues } from "@/lib/auth/schemas";

const linkClass = "inline-flex min-h-11 items-center font-semibold text-link underline underline-offset-4 hover:decoration-2";

export function ResetPasswordForm() {
  const params = useSearchParams();
  const pathname = usePathname();
  const reset = useResetPassword();
  const [showPassword, setShowPassword] = useState(false);
  const [formError, setFormError] = useState<{ message: string; expired: boolean } | null>(null);
  // Read the token once, then take it out of the address bar so it is not left in history or a copied URL.
  const [token] = useState(() => params.get("token") ?? "");
  useEffect(() => {
    if (params.has("token")) window.history.replaceState(null, "", pathname);
  }, [params, pathname]);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<ResetValues>({ resolver: zodResolver(resetSchema), defaultValues: { token, password: "", confirmPassword: "" } });
  const password = useWatch({ control, name: "password" }) ?? "";

  async function onSubmit(values: ResetValues) {
    setFormError(null);
    try {
      await reset.mutateAsync(values);
    } catch (e) {
      const err = e instanceof AuthError ? e : new AuthError("UNKNOWN", "Something went wrong. Please try again.");
      setFormError({ message: err.message, expired: err.code === "INVALID_RESET_TOKEN" });
    }
  }

  if (reset.isSuccess) {
    return (
      <div>
        <h1 className="font-display text-3xl font-extrabold tracking-tight">Password changed</h1>
        <div className="mt-6">
          <FormAlert tone="success" title="Your password has been changed">
            Log in with your new password. For your security, other devices will be asked to log in again.
          </FormAlert>
        </div>
        <Link href="/login" className="mt-8 inline-flex min-h-11 items-center justify-center rounded-brand bg-electric px-5 text-sm font-semibold text-white hover:brightness-110">
          Log in
        </Link>
      </div>
    );
  }

  if (!token) {
    return (
      <div>
        <h1 className="font-display text-3xl font-extrabold tracking-tight">Reset your password</h1>
        <div className="mt-6">
          <FormAlert tone="error" title="This reset link is incomplete">
            Open the link from your email again, or request a new one.
          </FormAlert>
        </div>
        <Link href="/forgot-password" className={`${linkClass} mt-6`}>
          Request a new link
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h1 className="font-display text-3xl font-extrabold tracking-tight">Choose a new password</h1>
      <p className="mt-2 text-subtle">Use at least 10 characters, with a letter and a number.</p>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-8 grid gap-5">
        {formError && (
          <FormAlert tone="error" title={formError.expired ? "This link cannot be used" : "We could not change your password"}>
            {formError.message}{" "}
            {formError.expired && (
              <Link href="/forgot-password" className={linkClass}>
                Request a new link
              </Link>
            )}
          </FormAlert>
        )}
        <input type="hidden" {...register("token")} />
        <div>
          <PasswordField
            id="password"
            label="New password"
            autoComplete="new-password"
            show={showPassword}
            onToggle={() => setShowPassword((s) => !s)}
            error={errors.password?.message}
            {...register("password")}
          />
          <PasswordStrength value={password} />
        </div>
        <PasswordField
          id="confirmPassword"
          label="Confirm new password"
          autoComplete="new-password"
          show={showPassword}
          onToggle={() => setShowPassword((s) => !s)}
          error={errors.confirmPassword?.message}
          {...register("confirmPassword")}
        />
        <SubmitButton loading={reset.isPending} loadingText="Changing password…">
          Change Password
        </SubmitButton>
      </form>
    </div>
  );
}
