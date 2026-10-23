import type { Metadata } from "next";
import { Suspense } from "react";
import { PortfolioView } from "@/components/portfolio/PortfolioView";

export const metadata: Metadata = { title: "Portfolio" };

export default function Page() {
  return (
    <Suspense fallback={<p role="status">Loading…</p>}>
      <PortfolioView />
    </Suspense>
  );
}
