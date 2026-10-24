"use client";

import { useState } from "react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Section } from "@/components/ui/Section";
import { waitlist } from "@/lib/content";
import { waitlistSchema, type WaitlistValues } from "@/lib/waitlist";

async function join(values: WaitlistValues): Promise<void> {
  let res: Response;
  try {
    res = await fetch("/api/waitlist", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(values), signal: AbortSignal.timeout(10_000) });
  } catch {
    throw new Error("We could not reach Quantiva. Check your connection and try again.");
  }
  if (res.ok) return;
  const body = (await res.json().catch(() => ({}))) as { message?: string };
  throw new Error(body.message ?? "Something went wrong. Please try again.");
}

// Public: no account, no session. Light surface in the landing page's fixed look.
export function WaitlistForm() {
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<WaitlistValues>({ resolver: zodResolver(waitlistSchema), defaultValues: { email: "", consent: false, company: "" } });

  async function onSubmit(values: WaitlistValues) {
    setError(null);
    try {
      await join(values);
      setDone(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Something went wrong. Please try again.");
    }
  }

  return (
    <Section id="waitlist" tone="silver" eyebrow={waitlist.eyebrow} title={waitlist.title} intro={waitlist.intro}>
      {done ? (
        <p role="status" className="max-w-xl rounded-brand border border-navy bg-white p-5 font-semibold text-navy">
          {waitlist.success}
        </p>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} noValidate aria-label="Join the launch list" className="grid max-w-xl gap-5">
          {error && (
            <p role="alert" className="rounded-brand border-2 border-navy bg-white p-4 font-semibold text-navy">
              {error}
            </p>
          )}
          <div className="grid gap-1.5">
            <label htmlFor="waitlist-email" className="font-semibold">
              Email
            </label>
            <input
              id="waitlist-email"
              type="email"
              autoComplete="email"
              inputMode="email"
              aria-invalid={errors.email ? true : undefined}
              aria-describedby={errors.email ? "waitlist-email-error" : undefined}
              className={`block min-h-11 w-full rounded-brand bg-white px-3.5 py-2.5 text-base text-navy ${errors.email ? "border-2 border-navy" : "border border-graphite"}`}
              {...register("email")}
            />
            {errors.email && (
              <p id="waitlist-email-error" role="alert" className="text-sm font-semibold">
                {errors.email.message}
              </p>
            )}
          </div>
          {/* Honeypot: off-screen and out of the tab order; people never see it */}
          <div aria-hidden="true" className="absolute -left-[9999px] h-0 w-0 overflow-hidden">
            <label htmlFor="waitlist-company">Company</label>
            <input id="waitlist-company" type="text" tabIndex={-1} autoComplete="off" {...register("company")} />
          </div>
          <div className="grid gap-1.5">
            <label className="flex min-h-11 items-start gap-3">
              <input type="checkbox" className="mt-1 size-5 accent-electric" aria-describedby={errors.consent ? "waitlist-consent-error" : undefined} aria-invalid={errors.consent ? true : undefined} {...register("consent")} />
              <span>
                {waitlist.consent}{" "}
                <Link href="/legal/privacy" className="font-semibold text-electric underline underline-offset-4">
                  Privacy policy
                </Link>
              </span>
            </label>
            {errors.consent && (
              <p id="waitlist-consent-error" role="alert" className="text-sm font-semibold">
                {errors.consent.message}
              </p>
            )}
          </div>
          <button type="submit" disabled={isSubmitting} className="inline-flex min-h-11 items-center justify-center rounded-brand bg-electric px-5 py-2.5 text-sm font-semibold text-white hover:brightness-110 disabled:opacity-60 sm:w-fit">
            {isSubmitting ? "Joining…" : "Join the list"}
          </button>
        </form>
      )}
    </Section>
  );
}
