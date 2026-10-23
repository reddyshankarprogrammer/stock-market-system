import { finalCta } from "@/lib/content";
import { Button } from "@/components/ui/Button";

export function FinalCta() {
  return (
    <section className="bg-navy py-section text-white" aria-labelledby="final-cta-title">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-8 px-4 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8 2xl:max-w-[1600px]">
        <h2 id="final-cta-title" className="max-w-2xl text-3xl font-extrabold tracking-tight sm:text-4xl">
          {finalCta.title}
        </h2>
        <div className="flex flex-col gap-3 sm:flex-row">
          <Button href={finalCta.primary.href}>{finalCta.primary.label}</Button>
          <Button href={finalCta.secondary.href} variant="secondary" tone="dark">
            {finalCta.secondary.label}
          </Button>
        </div>
      </div>
    </section>
  );
}
