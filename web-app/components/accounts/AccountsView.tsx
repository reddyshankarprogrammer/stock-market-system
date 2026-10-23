"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { FormAlert } from "@/components/ui/form";
import { useAccounts } from "@/lib/api/account-hooks";
import type { Account } from "@/lib/api/account-api";
import { useMe } from "@/lib/auth/auth-hooks";
import { AuthError } from "@/lib/auth/errors";

const brokerLabel: Record<string, string> = { PLATFORM: "Quantiva platform", PAPER: "Paper trading", GROWW: "Groww", ZERODHA: "Zerodha", BINANCE: "Binance", EXNESS: "Exness", MANUAL: "Manual" };
const statusLabel: Record<string, string> = { ACTIVE: "Active", SUSPENDED: "Suspended", CLOSED: "Closed" };

function money(value: number | undefined, currency: string | undefined) {
  if (value === undefined) return "-";
  try {
    return new Intl.NumberFormat("en", { style: "currency", currency: currency ?? "USD" }).format(value);
  } catch {
    return `${value} ${currency ?? ""}`.trim(); // an unknown currency code must not break the page
  }
}

const isUnauthenticated = (e: unknown) => e instanceof AuthError && e.code === "UNAUTHENTICATED";

export function AccountsView() {
  const router = useRouter();
  const me = useMe();
  const accounts = useAccounts(me.data?.userId);

  const signedOut = isUnauthenticated(me.error) || isUnauthenticated(accounts.error);
  useEffect(() => {
    if (signedOut) router.replace("/login?next=/accounts");
  }, [signedOut, router]);

  const pending = me.isPending || (accounts.isPending && accounts.fetchStatus !== "idle");
  const error = me.error ?? accounts.error;
  const retry = () => (me.isError ? me.refetch() : accounts.refetch());

  return (
    <div className="grid gap-6">
      <div>
        <h1 className="font-display text-3xl font-extrabold tracking-tight">Accounts</h1>
        <p className="mt-2 text-subtle">Your platform and tracking accounts.</p>
      </div>
      <Body signedOut={signedOut} pending={pending} error={error} data={accounts.data} retry={retry} />
    </div>
  );
}

function Body({ signedOut, pending, error, data, retry }: { signedOut: boolean; pending: boolean; error: Error | null; data: Account[] | undefined; retry: () => void }) {
  if (signedOut || pending) {
    return (
      <p role="status" aria-busy="true" className="text-subtle">
        Loading your accounts…
      </p>
    );
  }
  if (error) {
    return (
      <div className="grid gap-4">
        <FormAlert tone="error" title="We could not load your accounts">
          {error.message}
        </FormAlert>
        <button type="button" onClick={retry} className="min-h-11 w-fit rounded-brand border border-link px-4 text-sm font-semibold text-link hover:bg-link hover:text-canvas">
          Try again
        </button>
      </div>
    );
  }
  if (!data || data.length === 0) {
    return (
      <FormAlert tone="info" title="No accounts yet">
        Opening an account and importing a broker statement are the next screens to be built. Accounts you open later will appear here.
      </FormAlert>
    );
  }
  return (
    <ul className="grid gap-4 sm:grid-cols-2">
      {data.map((a) => (
        <li key={a.id} className="rounded-brand border border-line bg-panel p-5">
          <div className="flex items-start justify-between gap-3">
            <h2 className="font-display text-lg font-bold">{a.accountName}</h2>
            <span className="rounded-brand border border-line px-2 py-0.5 text-xs font-semibold">{statusLabel[a.status ?? ""] ?? a.status}</span>
          </div>
          <p className="mt-1 text-sm text-subtle">
            {brokerLabel[a.brokerName ?? ""] ?? a.brokerName} · {a.accountType?.toLowerCase()}
          </p>
          <p className="mt-4 font-display text-2xl font-extrabold">{money(a.balance, a.currency)}</p>
          <p className="text-xs text-subtle">{a.brokerName === "PLATFORM" || a.brokerName === "PAPER" ? "Cash balance" : "Tracking only: no cash moves here"}</p>
        </li>
      ))}
    </ul>
  );
}
