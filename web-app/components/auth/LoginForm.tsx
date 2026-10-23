"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { FormAlert, PasswordField, SubmitButton, TextField } from "@/components/ui/form";
import { useLogin } from "@/lib/auth/auth-hooks";
import { REMEMBER_EMAIL_KEY, safeNext } from "@/lib/auth/constants";
import { AuthError } from "@/lib/auth/errors";
import { loginSchema, type LoginInput, type LoginValues } from "@/lib/auth/schemas";

export function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const login = useLogin();
  const [showPassword, setShowPassword] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    setFocus,
    formState: { errors },
  } = useForm<LoginInput, unknown, LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "", remember: false },
  });

  // "Remember me" remembers the email on this device (never the password).
  useEffect(() => {
    try {
      const saved = localStorage.getItem(REMEMBER_EMAIL_KEY);
      if (saved) {
        setValue("email", saved);
        setValue("remember", true);
      }
    } catch {
      /* storage blocked: skip */
    }
  }, [setValue]);

  async function onSubmit(values: LoginValues) {
    setFormError(null);
    try {
      await login.mutateAsync(values);
      try {
        if (values.remember) localStorage.setItem(REMEMBER_EMAIL_KEY, values.email);
        else localStorage.removeItem(REMEMBER_EMAIL_KEY);
      } catch {
        /* storage blocked: skip */
      }
      router.replace(safeNext(params.get("next")));
    } catch (e) {
      const err = e instanceof AuthError ? e : new AuthError("UNKNOWN", "Something went wrong. Please try again.");
      setFormError(err.message);
      if (err.code === "INVALID_CREDENTIALS") setFocus("password");
    }
  }

  const busy = login.isPending || login.isSuccess;

  return (
    <div>
      <h1 className="font-display text-3xl font-extrabold tracking-tight">Log in to Quantiva</h1>
      <p className="mt-2 text-subtle">Welcome back. Enter your details to continue.</p>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-8 grid gap-5">
        {formError && <FormAlert tone="error" title="We could not log you in">{formError}</FormAlert>}
        {login.isSuccess && <FormAlert tone="success" title="Signed in">Taking you to your dashboard…</FormAlert>}

        <TextField id="email" label="Email" type="email" autoComplete="username" inputMode="email" autoCapitalize="none" spellCheck={false} error={errors.email?.message} {...register("email")} />

        <PasswordField
          id="password"
          label="Password"
          autoComplete="current-password"
          show={showPassword}
          onToggle={() => setShowPassword((s) => !s)}
          error={errors.password?.message}
          {...register("password")}
        />

        <div className="flex flex-wrap items-center justify-between gap-x-4">
          <label className="flex min-h-11 cursor-pointer items-center gap-3 text-sm">
            <input type="checkbox" className="h-5 w-5 accent-electric" {...register("remember")} />
            Remember me
          </label>
          <Link href="/forgot-password" className="inline-flex min-h-11 items-center text-sm font-semibold text-link underline underline-offset-4 hover:decoration-2">
            Forgot password?
          </Link>
        </div>

        <SubmitButton loading={busy} loadingText={login.isSuccess ? "Redirecting…" : "Logging in…"}>
          Login
        </SubmitButton>
      </form>

      <p className="mt-6 text-sm text-subtle">
        New to Quantiva?{" "}
        <Link href="/register" className="inline-flex min-h-11 items-center font-semibold text-link underline underline-offset-4 hover:decoration-2">
          Create account
        </Link>
      </p>
    </div>
  );
}
