import Link from "next/link";
import { FormAlert } from "@/components/ui/form";

// Honest placeholder for screens whose backend does not exist yet (user-service has no such endpoint).
export function PendingPage({ title, why }: { title: string; why: string }) {
  return (
    <div>
      <h1 className="font-display text-3xl font-extrabold tracking-tight">{title}</h1>
      <div className="mt-6">
        <FormAlert tone="info" title="Not available yet">
          {why}
        </FormAlert>
      </div>
      <Link href="/login" className="mt-8 inline-flex min-h-11 items-center rounded-brand border border-link px-5 text-sm font-semibold text-link hover:bg-link hover:text-canvas">
        Back to log in
      </Link>
    </div>
  );
}
