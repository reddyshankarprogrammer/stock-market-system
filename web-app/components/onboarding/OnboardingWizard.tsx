"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ErrorText, FormAlert, SubmitButton } from "@/components/ui/form";
import { useCompleteOnboarding, useOnboarding, useSaveOnboarding } from "@/lib/auth/auth-hooks";
import { ONBOARDING_SKIPPED_KEY } from "@/lib/auth/constants";
import { AuthError } from "@/lib/auth/errors";
import { brokers, experiences, firstIncompleteStep, onboardingSchema, plans, steps, type Onboarding } from "@/lib/onboarding";

const secondary = "inline-flex min-h-11 items-center justify-center rounded-brand border border-line px-5 text-sm font-semibold text-ink hover:bg-soft";
const card =
  "flex min-h-11 cursor-pointer items-start gap-3 rounded-brand border border-line p-4 text-sm has-[:checked]:border-electric has-[:checked]:bg-soft has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-electric";

/** Loads the saved answers first, so a half-finished onboarding resumes at the right step. */
export function OnboardingWizard() {
  const query = useOnboarding();
  if (query.isPending) {
    return (
      <p role="status" aria-busy="true" className="text-subtle">
        Loading…
      </p>
    );
  }
  if (query.isError) {
    return (
      <div className="grid gap-4">
        <FormAlert tone="error" title="We could not load your setup">
          {query.error.message}
        </FormAlert>
        <button type="button" onClick={() => query.refetch()} className={`${secondary} w-fit`}>
          Try again
        </button>
      </div>
    );
  }
  return <Wizard initial={query.data} />;
}

function Wizard({ initial }: { initial: Onboarding }) {
  const router = useRouter();
  const save = useSaveOnboarding();
  const complete = useCompleteOnboarding();
  const heading = useRef<HTMLHeadingElement>(null);

  const [step, setStep] = useState(() => firstIncompleteStep(initial));
  const [experience, setExperience] = useState(initial.tradingExperience ?? "");
  const [picked, setPicked] = useState<string[]>(initial.brokerPreferences);
  const [plan, setPlan] = useState(initial.plan ?? "");
  const [problem, setProblem] = useState<string | null>(null); // a choice is missing
  const [failure, setFailure] = useState<string | null>(null); // the service said no

  // Move keyboard and screen-reader focus to the new step's heading.
  useEffect(() => {
    heading.current?.focus();
  }, [step]);

  if (initial.completed) {
    return (
      <div>
        <h1 className="font-display text-3xl font-extrabold tracking-tight">Your setup is complete</h1>
        <Link href="/dashboard" className={`${secondary} mt-6`}>
          Go to your dashboard
        </Link>
      </div>
    );
  }

  function toggleBroker(code: string) {
    setProblem(null);
    setPicked((cur) => {
      if (code === "NONE") return cur.includes("NONE") ? [] : ["NONE"]; // "no broker yet" excludes the others
      const without = cur.filter((c) => c !== "NONE" && c !== code);
      return cur.includes(code) ? without : [...without, code];
    });
  }

  async function next(e: FormEvent) {
    e.preventDefault();
    setFailure(null);
    const values = step === 0 ? { tradingExperience: experience || undefined } : step === 1 ? { brokerPreferences: picked.length ? picked : undefined } : { plan: plan || undefined };
    const key = step === 0 ? "tradingExperience" : step === 1 ? "brokerPreferences" : "plan";
    if (!(values as Record<string, unknown>)[key]) {
      setProblem(step === 0 ? "Choose your trading experience" : step === 1 ? "Choose at least one broker, or say you do not use one yet" : "Choose a plan");
      return;
    }
    const parsed = onboardingSchema.safeParse(values);
    if (!parsed.success) {
      setProblem(parsed.error.issues[0]?.message ?? "Please check your choice");
      return;
    }
    setProblem(null);
    try {
      await save.mutateAsync(parsed.data);
      setStep(step + 1);
    } catch (err) {
      setFailure(err instanceof AuthError ? err.message : "Something went wrong. Please try again.");
    }
  }

  async function finish() {
    setFailure(null);
    try {
      await complete.mutateAsync();
      router.replace("/dashboard?welcome=1"); // the dashboard greets a user who has just finished setting up
    } catch (err) {
      setFailure(err instanceof AuthError ? err.message : "Something went wrong. Please try again.");
    }
  }

  function skip() {
    try {
      sessionStorage.setItem(ONBOARDING_SKIPPED_KEY, "1"); // only for this browser session: we ask again next time
    } catch {
      /* storage blocked: the dashboard will simply ask again */
    }
    router.replace("/dashboard");
  }

  const label = (list: readonly { code: string; label: string }[], code: string) => list.find((x) => x.code === code)?.label ?? code;
  const busy = save.isPending;

  return (
    <div>
      <nav aria-label="Setup progress">
        <p className="text-sm font-semibold text-subtle">
          Step {step + 1} of {steps.length}: {steps[step]}
        </p>
        <ol className="mt-3 flex gap-2">
          {steps.map((s, i) => (
            <li key={s} aria-current={i === step ? "step" : undefined} className="flex-1">
              <span className={`block h-1.5 rounded-full ${i <= step ? "bg-electric" : "bg-line/50"}`} aria-hidden="true" />
              <span className="sr-only">
                {s}
                {i < step ? " (done)" : i === step ? " (current step)" : ""}
              </span>
            </li>
          ))}
        </ol>
      </nav>

      <h1 ref={heading} tabIndex={-1} className="mt-8 font-display text-3xl font-extrabold tracking-tight outline-none">
        {step === 0 && "What is your trading experience?"}
        {step === 1 && "Which brokers do you use?"}
        {step === 2 && "Which plan interests you?"}
        {step === 3 && "Check your answers"}
      </h1>

      {failure && (
        <div className="mt-6">
          <FormAlert tone="error" title="We could not save that">
            {failure}
          </FormAlert>
        </div>
      )}

      {step < 3 ? (
        <form onSubmit={next} noValidate className="mt-6 grid gap-5">
          {step === 0 && (
            <fieldset aria-describedby={problem ? "choice-error" : undefined}>
              <legend className="sr-only">Trading experience</legend>
              <div className="grid gap-3 sm:grid-cols-2">
                {experiences.map((x) => (
                  <label key={x.code} className={card}>
                    <input type="radio" name="experience" value={x.code} checked={experience === x.code} onChange={() => { setExperience(x.code); setProblem(null); }} className="mt-0.5 h-5 w-5 accent-electric" />
                    <span>
                      <span className="block font-semibold">{x.label}</span>
                      <span className="text-subtle">{x.text}</span>
                    </span>
                  </label>
                ))}
              </div>
            </fieldset>
          )}

          {step === 1 && (
            <fieldset aria-describedby={problem ? "choice-error" : "broker-note"}>
              <legend className="sr-only">Brokers</legend>
              <div className="grid gap-3 sm:grid-cols-2">
                {brokers.map((b) => (
                  <label key={b.code} className={card}>
                    <input type="checkbox" checked={picked.includes(b.code)} onChange={() => toggleBroker(b.code)} className="mt-0.5 h-5 w-5 accent-electric" />
                    <span className="font-semibold">{b.label}</span>
                  </label>
                ))}
              </div>
              <p id="broker-note" className="mt-3 text-xs text-subtle">
                Choose all that apply. Statement import for Groww, Zerodha, Binance and Exness is in development. Broker names are trademarks of their owners; no affiliation is implied.
              </p>
            </fieldset>
          )}

          {step === 2 && (
            <fieldset aria-describedby={problem ? "choice-error" : "plan-note"}>
              <legend className="sr-only">Plan</legend>
              <p id="plan-note" className="mb-3 text-sm text-subtle">
                Billing is not set up yet and prices are to be confirmed. Pick the plan you are interested in: we save your choice and tell you when paid plans open. Everyone starts on Free.
              </p>
              <div className="grid gap-3">
                {plans.map((p) => (
                  <label key={p.code} className={card}>
                    <input type="radio" name="plan" value={p.code} checked={plan === p.code} onChange={() => { setPlan(p.code); setProblem(null); }} className="mt-0.5 h-5 w-5 accent-electric" />
                    <span className="min-w-0">
                      <span className="block font-semibold">
                        {p.name} <span className="font-normal text-subtle">· {p.audience} · price TBC</span>
                      </span>
                      <span className="block text-subtle">{p.points.join(" · ")}</span>
                      {p.paid && <span className="mt-1 block text-xs font-semibold">Not available yet. Your interest is saved and you start on Free.</span>}
                    </span>
                  </label>
                ))}
              </div>
            </fieldset>
          )}

          {problem && <ErrorText id="choice-error">{problem}</ErrorText>}

          <div className="flex flex-wrap items-center gap-3">
            {step > 0 && (
              <button type="button" onClick={() => { setProblem(null); setFailure(null); setStep(step - 1); }} className={secondary}>
                Back
              </button>
            )}
            <div className="min-w-40 flex-1 sm:max-w-56">
              <SubmitButton loading={busy} loadingText="Saving…">
                Continue
              </SubmitButton>
            </div>
          </div>
        </form>
      ) : (
        <div className="mt-6 grid gap-5">
          <dl className="grid gap-3 text-sm">
            {[
              [0, "Trading experience", label(experiences, experience)],
              [1, "Brokers", picked.map((c) => label(brokers, c)).join(", ")],
              [2, "Plan of interest", plans.find((p) => p.code === plan)?.name ?? plan],
            ].map(([i, term, value]) => (
              <div key={String(term)} className="flex items-start justify-between gap-4 rounded-brand border border-line p-4">
                <div className="min-w-0">
                  <dt className="text-subtle">{term}</dt>
                  <dd className="font-semibold">{value}</dd>
                </div>
                <button type="button" onClick={() => setStep(Number(i))} className="min-h-11 shrink-0 font-semibold text-link underline underline-offset-4 hover:decoration-2">
                  Change<span className="sr-only"> {String(term).toLowerCase()}</span>
                </button>
              </div>
            ))}
          </dl>
          <p className="text-sm text-subtle">Quantiva is for education and information only and is not investment advice.</p>
          <div className="flex flex-wrap items-center gap-3">
            <button type="button" onClick={() => setStep(2)} className={secondary}>
              Back
            </button>
            <div className="min-w-40 flex-1 sm:max-w-56">
              <button
                type="button"
                onClick={finish}
                disabled={complete.isPending || complete.isSuccess}
                aria-busy={complete.isPending}
                className="inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-brand bg-electric px-5 py-2.5 text-sm font-semibold text-white transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-70"
              >
                {complete.isPending ? "Finishing…" : complete.isSuccess ? "Done" : "Complete setup"}
              </button>
            </div>
          </div>
          {complete.isSuccess && <FormAlert tone="success" title="You are all set">Taking you to your dashboard…</FormAlert>}
        </div>
      )}

      <p className="mt-8 text-sm text-subtle">
        Not now?{" "}
        <button type="button" onClick={skip} className="inline-flex min-h-11 items-center font-semibold text-link underline underline-offset-4 hover:decoration-2">
          Skip for now
        </button>
        . We will ask again next time you log in.
      </p>
    </div>
  );
}
