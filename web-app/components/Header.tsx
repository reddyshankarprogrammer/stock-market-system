import Image from "next/image";
import Link from "next/link";
import { nav, site } from "@/lib/content";
import { Button } from "@/components/ui/Button";

// Server component: the mobile menu is a native <details>, so it needs no client JavaScript.
export function Header() {
  return (
    <header className="sticky top-0 z-50 border-b border-silver bg-white/95 text-navy backdrop-blur">
      <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8 2xl:max-w-[1600px]">
        <Link href="/" className="flex shrink-0 items-center gap-2" aria-label={`${site.name} home`}>
          <Image src="/brand/quantiva-icon-192.png" alt="" width={36} height={36} priority />
          <span className="font-display text-xl font-extrabold tracking-tight">
            {site.name}
            <sup className="text-[0.55em] font-semibold">™</sup>
          </span>
        </Link>

        <nav aria-label="Primary" className="hidden lg:block">
          <ul className="flex items-center gap-7 text-sm font-medium">
            {nav.links.map((l) => (
              <li key={l.label}>
                <a href={l.href} className="py-2 text-graphite transition-colors hover:text-electric">
                  {l.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="hidden items-center gap-3 lg:flex">
          <Button href={nav.login.href} variant="secondary">
            {nav.login.label}
          </Button>
          <Button href={nav.trial.href}>{nav.trial.label}</Button>
        </div>

        <details className="group relative lg:hidden">
          <summary
            className="flex min-h-11 min-w-11 cursor-pointer list-none items-center justify-center rounded-brand border border-silver text-sm font-semibold marker:hidden [&::-webkit-details-marker]:hidden"
            aria-label="Menu"
          >
            <span className="group-open:hidden">Menu</span>
            <span className="hidden group-open:inline">Close</span>
          </summary>
          <div className="absolute right-0 top-14 w-[min(20rem,calc(100vw-2rem))] rounded-brand border border-silver bg-white p-4 shadow-xl">
            <ul className="grid gap-1 text-base font-medium">
              {nav.links.map((l) => (
                <li key={l.label}>
                  <a href={l.href} className="block rounded-brand px-3 py-2.5 hover:bg-silver/50">
                    {l.label}
                  </a>
                </li>
              ))}
            </ul>
            <div className="mt-4 grid gap-2">
              <Button href={nav.login.href} variant="secondary">
                {nav.login.label}
              </Button>
              <Button href={nav.trial.href}>{nav.trial.label}</Button>
            </div>
          </div>
        </details>
      </div>
    </header>
  );
}
