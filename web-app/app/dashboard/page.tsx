import { Suspense } from "react";
import { DashboardView } from "@/components/auth/DashboardView";

export default function Page() {
  return (
    <Suspense fallback={<p role="status">Loading…</p>}>
      <DashboardView />
    </Suspense>
  );
}
