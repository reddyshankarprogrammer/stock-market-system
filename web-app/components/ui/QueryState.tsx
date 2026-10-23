"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { FormAlert } from "@/components/ui/form";
import { AuthError } from "@/lib/auth/errors";

type Q = { isPending: boolean; isError: boolean; error: unknown; refetch: () => unknown; fetchStatus?: string };

/** Loading, error (with Try again) and signed-out handling shared by every data screen. Renders children when ready. */
export function QueryState({ query, what, next, children }: { query: Q | Q[]; what: string; next: string; children: ReactNode }) {
  const router = useRouter();
  const queries = Array.isArray(query) ? query : [query];
  const failed = queries.find((q) => q.isError);
  const signedOut = failed?.error instanceof AuthError && failed.error.code === "UNAUTHENTICATED";
  useEffect(() => {
    if (signedOut) router.replace(`/login?next=${encodeURIComponent(next)}`);
  }, [signedOut, router, next]);

  if (signedOut || (!failed && queries.some((q) => q.isPending && q.fetchStatus !== "idle"))) {
    return (
      <p role="status" aria-busy="true" className="text-subtle">
        Loading {what}…
      </p>
    );
  }
  if (failed) {
    return (
      <div className="grid gap-4">
        <FormAlert tone="error" title={`We could not load ${what}`}>
          {failed.error instanceof Error ? failed.error.message : "Something went wrong. Please try again."}
        </FormAlert>
        <button type="button" onClick={() => queries.filter((q) => q.isError).forEach((q) => q.refetch())} className="min-h-11 w-fit rounded-brand border border-link px-4 text-sm font-semibold text-link hover:bg-link hover:text-canvas">
          Try again
        </button>
      </div>
    );
  }
  return <>{children}</>;
}
