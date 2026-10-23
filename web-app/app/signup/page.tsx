import type { Metadata } from "next";
import { Button } from "@/components/ui/Button";

export const metadata: Metadata = {
  title: "Get Started",
  robots: { index: false },
};

// Placeholder until the registration form exists (daily plan unit 27).
export default function SignupPage() {
  return (
    <main className="mx-auto flex min-h-[60vh] w-full max-w-xl flex-col items-start justify-center px-4 py-16 sm:px-6">
      <h1 className="text-3xl font-extrabold">Free trial sign-up is coming soon</h1>
      <p className="mt-4 text-graphite">Registration is being built. Check back shortly.</p>
      <Button href="/" className="mt-8">
        Back to home
      </Button>
    </main>
  );
}
