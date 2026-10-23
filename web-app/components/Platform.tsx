import { platform } from "@/lib/content";
import { Illustration } from "@/components/Illustration";
import { Card } from "@/components/ui/Card";
import { Section } from "@/components/ui/Section";

const statusStyle = {
  "In development": "bg-electric text-white",
  Planned: "bg-silver text-navy",
};

export function Platform() {
  return (
    <Section id="platform" eyebrow={platform.eyebrow} title={platform.title} intro={platform.intro}>
      <ul className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
        {platform.blocks.map((b) => (
          <li key={b.slug} className="min-w-0">
            <Card className="flex h-full flex-col gap-3">
              <Illustration kind={b.art} />
              <div className="flex items-start justify-between gap-2">
                <h3 className="text-lg font-bold">{b.title}</h3>
                <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-bold ${statusStyle[b.status]}`}>{b.status}</span>
              </div>
              <p className="text-sm text-graphite">{b.benefit}</p>
              <details className="mt-auto border-t border-silver pt-1">
                <summary className="flex min-h-11 cursor-pointer list-none items-center text-sm font-semibold text-electric underline-offset-4 hover:underline [&::-webkit-details-marker]:hidden">
                  Learn more
                  <span className="sr-only"> about {b.title}</span>
                </summary>
                <p className="mt-1 text-xs font-bold uppercase tracking-wide text-graphite">{platform.moreHeading}</p>
                <ul className="mt-2 grid gap-1.5 pb-2 text-sm text-graphite">
                  {b.more.map((m) => (
                    <li key={m} className="flex gap-2">
                      <span aria-hidden="true" className="text-electric">
                        •
                      </span>
                      {m}
                    </li>
                  ))}
                </ul>
              </details>
            </Card>
          </li>
        ))}
      </ul>
    </Section>
  );
}
