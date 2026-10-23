import Image from "next/image";
import { footer, site } from "@/lib/content";

export function Footer() {
  return (
    <footer className="bg-navy text-silver">
      <div className="mx-auto w-full max-w-7xl px-4 py-14 sm:px-6 lg:px-8 2xl:max-w-[1600px]">
        <div className="grid gap-10 md:grid-cols-[1.4fr_repeat(3,1fr)]">
          <div>
            <div className="flex items-center gap-2">
              <span className="rounded-brand bg-white p-1">
                <Image src="/brand/quantiva-icon-192.png" alt="" width={32} height={32} loading="lazy" />
              </span>
              <span className="font-display text-xl font-extrabold text-white">{site.name}™</span>
            </div>
            <p className="mt-3 max-w-xs text-sm">{site.tagline}</p>
          </div>
          {footer.columns.map((c) => (
            <nav key={c.title} aria-label={c.title}>
              <h2 className="text-sm font-bold uppercase tracking-wide text-white">{c.title}</h2>
              <ul className="mt-3 grid gap-2 text-sm">
                {c.links.map((l) => (
                  <li key={l.label}>
                    <a href={l.href} className="transition-colors hover:text-white">
                      {l.label}
                    </a>
                  </li>
                ))}
              </ul>
            </nav>
          ))}
        </div>
        <p className="mt-10 border-t border-white/15 pt-6 text-xs leading-relaxed">{footer.risk}</p>
        <p className="mt-4 text-xs">{footer.copyright}</p>
      </div>
    </footer>
  );
}
