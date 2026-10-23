"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { PasswordStrength } from "@/components/auth/PasswordStrength";
import { TermsGate } from "@/components/auth/TermsGate";
import { ErrorText, FormAlert, PasswordField, SelectField, SubmitButton, TextField, inputClass } from "@/components/ui/form";
import { useRegister } from "@/lib/auth/auth-hooks";
import { countries, dialCodes, dialFor } from "@/lib/auth/countries";
import { AuthError } from "@/lib/auth/errors";
import { registerSchema, type RegisterParsed, type RegisterValues } from "@/lib/auth/schemas";

export function RegisterForm() {
  const router = useRouter();
  const registerUser = useRegister();
  const [showPassword, setShowPassword] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    setFocus,
    control,
    formState: { errors },
  } = useForm<RegisterValues, unknown, RegisterParsed>({
    resolver: zodResolver(registerSchema),
    defaultValues: { firstName: "", lastName: "", email: "", password: "", confirmPassword: "", country: "", dialCode: "+91", mobile: "", gender: undefined, termsAccepted: false },
  });

  async function onSubmit(values: RegisterParsed) {
    setFormError(null);
    try {
      await registerUser.mutateAsync(values);
      router.replace("/dashboard?welcome=1");
    } catch (e) {
      const err = e instanceof AuthError ? e : new AuthError("UNKNOWN", "Something went wrong. Please try again.");
      if (err.code === "EMAIL_TAKEN") {
        setError("email", { message: err.message });
        setFocus("email");
      } else {
        setFormError(err.message);
      }
    }
  }

  const busy = registerUser.isPending || registerUser.isSuccess;
  const password = useWatch({ control, name: "password" }) ?? "";
  const termsAccepted = useWatch({ control, name: "termsAccepted" }) === true;

  return (
    <div>
      <h1 className="font-display text-3xl font-extrabold tracking-tight">Create your account</h1>
      <p className="mt-2 text-subtle">All fields are required. Quantiva is an educational tool, not investment advice.</p>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-8 grid gap-6">
        {formError && <FormAlert tone="error" title="We could not create your account">{formError}</FormAlert>}
        {registerUser.isSuccess && <FormAlert tone="success" title="Account created">Taking you to your dashboard…</FormAlert>}

        <fieldset className="grid gap-5 sm:grid-cols-2">
          <legend className="sr-only">Your name</legend>
          <TextField id="firstName" label="First name" autoComplete="given-name" error={errors.firstName?.message} {...register("firstName")} />
          <TextField id="lastName" label="Last name" autoComplete="family-name" error={errors.lastName?.message} {...register("lastName")} />
        </fieldset>

        <TextField id="email" label="Email" type="email" autoComplete="email" inputMode="email" autoCapitalize="none" spellCheck={false} error={errors.email?.message} {...register("email")} />

        <fieldset className="grid gap-5 sm:grid-cols-2">
          <legend className="sr-only">Password</legend>
          <div>
            <PasswordField
              id="password"
              label="Password"
              autoComplete="new-password"
              show={showPassword}
              onToggle={() => setShowPassword((s) => !s)}
              hint="At least 10 characters, with a letter and a number."
              error={errors.password?.message}
              {...register("password")}
            />
            <PasswordStrength value={password} />
          </div>
          <PasswordField
            id="confirmPassword"
            label="Confirm password"
            autoComplete="new-password"
            show={showPassword}
            onToggle={() => setShowPassword((s) => !s)}
            error={errors.confirmPassword?.message}
            {...register("confirmPassword")}
          />
        </fieldset>

        <SelectField
          id="country"
          label="Country"
          autoComplete="country"
          error={errors.country?.message}
          {...register("country", { onChange: (e) => setValue("dialCode", dialFor(e.target.value)) })}
        >
          <option value="">Select your country</option>
          {countries.map((c) => (
            <option key={c.code} value={c.code}>
              {c.name}
            </option>
          ))}
        </SelectField>

        <div>
          <label htmlFor="mobile" className="mb-1.5 block text-sm font-semibold">
            Mobile number
          </label>
          <div className="grid grid-cols-[6.5rem_minmax(0,1fr)] gap-3">
            <select aria-label="Dial code" className={inputClass(errors.dialCode?.message)} {...register("dialCode")}>
              {dialCodes.map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
            </select>
            <input
              id="mobile"
              type="tel"
              inputMode="numeric"
              autoComplete="tel-national"
              aria-invalid={errors.mobile ? true : undefined}
              aria-describedby={errors.mobile ? "mobile-error" : "mobile-hint"}
              className={inputClass(errors.mobile?.message)}
              {...register("mobile")}
            />
          </div>
          {errors.mobile ? (
            <ErrorText id="mobile-error">{errors.mobile.message}</ErrorText>
          ) : errors.dialCode ? (
            <ErrorText id="mobile-error">{errors.dialCode.message}</ErrorText>
          ) : (
            <p id="mobile-hint" className="mt-1.5 text-xs text-subtle">
              No OTP is needed. We do not verify this number.
            </p>
          )}
        </div>

        <fieldset aria-describedby={errors.gender ? "gender-error" : undefined}>
          <legend className="mb-1.5 text-sm font-semibold">Gender</legend>
          <div className="grid grid-cols-2 gap-3">
            {(
              [
                { value: "male", label: "Male" },
                { value: "female", label: "Female" },
              ] as const
            ).map((g) => (
              <label key={g.value} className="flex min-h-11 cursor-pointer items-center gap-3 rounded-brand border border-line px-3.5 text-sm has-[:checked]:border-electric has-[:checked]:bg-soft">
                <input type="radio" value={g.value} className="h-5 w-5 accent-electric" {...register("gender")} />
                <span>{g.label}</span>
              </label>
            ))}
          </div>
          {errors.gender && <ErrorText id="gender-error">{errors.gender.message}</ErrorText>}
        </fieldset>

        <TermsGate
          accepted={termsAccepted}
          error={errors.termsAccepted?.message}
          onAccept={() => setValue("termsAccepted", true, { shouldValidate: true, shouldDirty: true })}
        />

        <SubmitButton loading={busy} loadingText={registerUser.isSuccess ? "Redirecting…" : "Creating account…"}>
          Create Account
        </SubmitButton>
      </form>

      <p className="mt-6 text-sm text-subtle">
        Already have an account?{" "}
        <Link href="/login" className="inline-flex min-h-11 items-center font-semibold text-link underline underline-offset-4 hover:decoration-2">
          Log in
        </Link>
      </p>
    </div>
  );
}
