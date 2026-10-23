import type { Metadata } from "next";
import { Button } from "@/components/ui/Button";

export const metadata: Metadata = {
  title: "Login",
  robots: { index: false },
};

// Placeholder until Keycloak sign-in exists (daily plan units 26-28).
export default function LoginPage() {
  return (
    <main className="mx-auto flex min-h-[60vh] w-full max-w-xl flex-col items-start justify-center px-4 py-16 sm:px-6">
      <h1 className="text-3xl font-extrabold">Login is coming soon</h1>
      <p className="mt-4 text-graphite">Sign-in with email, password and social accounts is being built.</p>
      <Button href="/" className="mt-8">
        Back to home
      </Button>
    </main>
  );
}
