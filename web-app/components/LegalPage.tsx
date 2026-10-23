import { Button } from "@/components/ui/Button";

// Placeholder legal pages so footer links work. Real documents need review by counsel before launch.
export function LegalPage({ title, body }: { title: string; body: string }) {
  return (
    <main className="mx-auto flex min-h-[60vh] w-full max-w-2xl flex-col items-start justify-center px-4 py-16 sm:px-6">
      <h1 className="text-3xl font-extrabold">{title}</h1>
      <p className="mt-4 text-graphite">{body}</p>
      <Button href="/" className="mt-8">
        Back to home
      </Button>
    </main>
  );
}
