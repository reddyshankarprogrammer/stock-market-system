import type { Metadata } from "next";
import { Button } from "@/components/ui/Button";

export const metadata: Metadata = {
  title: "Contact sales",
  robots: { index: false },
};

// Placeholder until a real contact channel exists.
export default function ContactPage() {
  return (
    <main className="mx-auto flex min-h-[60vh] w-full max-w-xl flex-col items-start justify-center px-4 py-16 sm:px-6">
      <h1 className="text-3xl font-extrabold">Contact sales is coming soon</h1>
      <p className="mt-4 text-graphite">A contact form for Enterprise enquiries is being prepared.</p>
      <Button href="/" className="mt-8">
        Back to home
      </Button>
    </main>
  );
}
