import type { Metadata } from "next";
import { Suspense } from "react";
import { AccountDetail } from "@/components/accounts/AccountDetail";

export const metadata: Metadata = { title: "Account" };

// The id is read inside Suspense: with cacheComponents, request data may only be awaited under a boundary.
async function Detail({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <AccountDetail id={id} />;
}

export default function Page({ params }: { params: Promise<{ id: string }> }) {
  return (
    <Suspense fallback={<p role="status">Loading…</p>}>
      <Detail params={params} />
    </Suspense>
  );
}
