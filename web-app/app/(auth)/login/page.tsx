import type { Metadata } from "next";
import { Suspense } from "react";
import { LoginForm } from "@/components/auth/LoginForm";

export const metadata: Metadata = { title: "Log in", robots: { index: false } };

// useSearchParams (the ?next= destination) needs a Suspense boundary.
export default function Page() {
  return (
    <Suspense fallback={<p role="status">Loading…</p>}>
      <LoginForm />
    </Suspense>
  );
}
