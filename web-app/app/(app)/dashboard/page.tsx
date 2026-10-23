import type { Metadata } from "next";
import { Suspense } from "react";
import { DashboardView } from "@/components/auth/DashboardView";

export const metadata: Metadata = { title: "Dashboard" };

export default function Page() {
  return (
    <Suspense fallback={<p role="status">Loading…</p>}>
      <DashboardView />
    </Suspense>
  );
}
