"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { OpenAccountForm } from "@/components/accounts/OpenAccountForm";
import { QueryState } from "@/components/ui/QueryState";
import { FormAlert } from "@/components/ui/form";
import { useAccounts } from "@/lib/api/account-hooks";
import { brokerLabel, accountTypeLabel, isTracking, money } from "@/lib/format";

const statusLabel: Record<string, string> = { ACTIVE: "Active", SUSPENDED: "Suspended", CLOSED: "Closed" };

export function AccountsView() {
  const router = useRouter();
  const accounts = useAccounts();
  const [opening, setOpening] = useState(false);

  return (
    <div className="grid gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-extrabold tracking-tight">Accounts</h1>
          <p className="mt-2 text-subtle">Your platform and tracking accounts.</p>
        </div>
        {!opening && (
          <button type="button" onClick={() => setOpening(true)} className="min-h-11 rounded-brand bg-electric px-5 text-sm font-semibold text-white hover:brightness-110">
            Open an account
          </button>
        )}
      </div>

      {opening && (
        <OpenAccountForm
          onDone={(id) => {
            setOpening(false);
            if (id) router.push(`/accounts/${id}`);
          }}
        />
      )}

      <QueryState query={[accounts.me, accounts]} what="your accounts" next="/accounts">
        {!accounts.data || accounts.data.length === 0 ? (
          <FormAlert tone="info" title="No accounts yet">
            Open an account to get started. A tracking account for your broker (Groww, Zerodha, Binance or Exness) lets you import a statement.
          </FormAlert>
        ) : (
          <ul className="grid gap-4 sm:grid-cols-2">
            {accounts.data.map((a) => (
              <li key={a.id} className="rounded-brand border border-line bg-panel p-5">
                <div className="flex items-start justify-between gap-3">
                  <h2 className="font-display text-lg font-bold">
                    <Link href={`/accounts/${a.id}`} className="underline-offset-4 hover:underline">
                      {a.accountName}
                    </Link>
                  </h2>
                  <span className="rounded-brand border border-line px-2 py-0.5 text-xs font-semibold">{statusLabel[a.status ?? ""] ?? a.status}</span>
                </div>
                <p className="mt-1 text-sm text-subtle">
                  {brokerLabel[a.brokerName ?? ""] ?? a.brokerName} · {accountTypeLabel[a.accountType ?? ""] ?? a.accountType}
                </p>
                <p className="mt-4 font-display text-2xl font-extrabold">{money(a.balance, a.currency)}</p>
                <p className="text-xs text-subtle">{isTracking(a.brokerName) ? "Tracking only: no cash moves here" : "Cash balance"}</p>
              </li>
            ))}
          </ul>
        )}
      </QueryState>
    </div>
  );
}
