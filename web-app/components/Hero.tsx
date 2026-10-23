import { hero } from "@/lib/content";
import { Button } from "@/components/ui/Button";
import { HeroDashboard } from "@/components/HeroDashboard";

export function Hero() {
  return (
    <section className="bg-navy text-white" aria-labelledby="hero-title">
      <div className="mx-auto grid w-full max-w-7xl items-center gap-12 px-4 py-16 sm:px-6 lg:grid-cols-[1.05fr_1fr] lg:gap-14 lg:px-8 lg:py-24 2xl:max-w-[1600px]">
        <div className="min-w-0">
          <p className="text-sm font-semibold uppercase tracking-widest text-growth">{hero.eyebrow}</p>
          <h1 id="hero-title" className="mt-4 text-4xl font-extrabold leading-[1.08] tracking-tight sm:text-5xl xl:text-6xl">
            {hero.title}
          </h1>
          <p className="mt-6 max-w-xl text-lg text-silver">{hero.subtitle}</p>
          <div className="mt-8 flex flex-col gap-3 sm:flex-row">
            <Button href={hero.primaryCta.href}>{hero.primaryCta.label}</Button>
            <Button href={hero.secondaryCta.href} variant="secondary" tone="dark">
              {hero.secondaryCta.label}
            </Button>
          </div>
        </div>
        <div className="min-w-0">
          <HeroDashboard />
        </div>
      </div>
    </section>
  );
}
