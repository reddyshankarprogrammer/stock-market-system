import type { Metadata } from "next";
import { Suspense } from "react";
import { ResetPasswordForm } from "@/components/auth/ResetPasswordForm";

export const metadata: Metadata = { title: "Reset password", robots: { index: false, follow: false } };

// useSearchParams (the ?token= from the emailed link) needs a Suspense boundary.
export default function Page() {
  return (
    <Suspense fallback={<p role="status">Loading…</p>}>
      <ResetPasswordForm />
    </Suspense>
  );
}
