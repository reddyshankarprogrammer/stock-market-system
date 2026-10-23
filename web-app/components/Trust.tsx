import { trust } from "@/lib/content";
import { Card } from "@/components/ui/Card";
import { Section } from "@/components/ui/Section";

const statusStyle = {
  Live: "bg-growth text-navy",
  "In progress": "bg-electric text-white",
  Planned: "bg-silver text-navy",
};

export function Trust() {
  return (
    <Section id="trust" tone="silver" eyebrow="Built for" title={trust.heading}>
      <ul className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
        {trust.audiences.map((a) => (
          <li key={a} className="rounded-brand border border-silver bg-white px-4 py-4 text-center text-sm font-semibold">
            {a}
          </li>
        ))}
      </ul>

      <h3 className="mt-14 text-xl font-bold">{trust.indicatorsHeading}</h3>
      <ul className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {trust.indicators.map((i) => (
          <li key={i.label}>
            <Card className="flex h-full items-start justify-between gap-3">
              <span className="text-sm font-medium">{i.label}</span>
              <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-bold ${statusStyle[i.status]}`}>{i.status}</span>
            </Card>
          </li>
        ))}
      </ul>
      <p className="mt-4 text-sm text-graphite">{trust.note}</p>
    </Section>
  );
}
