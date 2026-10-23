import type { Metadata } from "next";
import { Suspense } from "react";
import { AccountsView } from "@/components/accounts/AccountsView";

export const metadata: Metadata = { title: "Accounts" };

export default function Page() {
  return (
    <Suspense fallback={<p role="status">Loading…</p>}>
      <AccountsView />
    </Suspense>
  );
}
