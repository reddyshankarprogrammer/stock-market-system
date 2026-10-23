import type { Metadata } from "next";
import { Suspense } from "react";
import { TradesView } from "@/components/trades/TradesView";

export const metadata: Metadata = { title: "Trades" };

export default function Page() {
  return (
    <Suspense fallback={<p role="status">Loading…</p>}>
      <TradesView />
    </Suspense>
  );
}
