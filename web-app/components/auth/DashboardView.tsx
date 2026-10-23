"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { FormAlert } from "@/components/ui/form";
import { useMe } from "@/lib/auth/auth-hooks";
import { useAuthStore } from "@/lib/auth/auth-store";
import { AuthError } from "@/lib/auth/errors";

const fieldNames: Record<string, string> = { mobile: "mobile number", gender: "gender", country: "country", termsAcceptance: "your acceptance of the Terms" };

// Placeholder for the real dashboard (daily plan, phase 3). It proves the session works end to end.
export function DashboardView() {
  const router = useRouter();
  const welcome = useSearchParams().get("welcome") === "1";
  const notice = useAuthStore((s) => s.notice);
  const me = useMe();

  const unauthenticated = me.error instanceof AuthError && me.error.code === "UNAUTHENTICATED";
  useEffect(() => {
    if (unauthenticated) router.replace("/login?next=/dashboard");
  }, [unauthenticated, router]);

  if (me.isPending || unauthenticated) {
    return (
      <p role="status" aria-busy="true" className="text-subtle">
        Loading your account…
      </p>
    );
  }
  if (me.isError) {
    return (
      <div className="grid gap-4">
        <FormAlert tone="error" title="We could not load your account">
          {me.error.message}
        </FormAlert>
        <button type="button" onClick={() => me.refetch()} className="min-h-11 w-fit rounded-brand border border-link px-4 text-sm font-semibold text-link hover:bg-link hover:text-canvas">
          Try again
        </button>
      </div>
    );
  }

  const user = me.data;
  return (
    <div className="grid gap-6">
      <div>
        <h1 className="font-display text-3xl font-extrabold tracking-tight">{welcome ? `Welcome to Quantiva, ${user.firstName}` : `Hello, ${user.firstName}`}</h1>
        <p className="mt-2 text-subtle">
          Signed in as {user.email} ({user.role === "ADMIN" ? "administrator" : "trader"}). This is a placeholder dashboard.
        </p>
      </div>
      {welcome && notice && notice.length > 0 && (
        <FormAlert tone="info" title="Some details were not saved yet">
          Your {notice.map((n) => fieldNames[n] ?? n).join(", ")} passed validation but our profile service cannot store them yet. We will ask for them again when saving is available.
        </FormAlert>
      )}
      <FormAlert tone="success" title="Your session is active">
        The session lives in a secure cookie that scripts on the page cannot read. It lasts 15 minutes, because the refresh endpoint does not exist yet; you will then be asked to log in again.
      </FormAlert>
    </div>
  );
}
