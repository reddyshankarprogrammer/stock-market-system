import { security } from "@/lib/content";
import { Card } from "@/components/ui/Card";
import { Section } from "@/components/ui/Section";

const statusStyle = {
  Live: "bg-growth text-navy",
  "In progress": "bg-electric text-white",
  Planned: "bg-silver text-navy",
};

export function Security() {
  return (
    <Section id="security" tone="silver" eyebrow={security.eyebrow} title={security.title} intro={security.intro}>
      <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {security.controls.map((c) => (
          <li key={c.title} className="min-w-0">
            <Card className="flex h-full flex-col gap-2">
              <div className="flex items-start justify-between gap-2">
                <h3 className="font-bold">{c.title}</h3>
                <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-bold ${statusStyle[c.status]}`}>{c.status}</span>
              </div>
              <p className="text-sm text-graphite">{c.text}</p>
            </Card>
          </li>
        ))}
      </ul>
    </Section>
  );
}
