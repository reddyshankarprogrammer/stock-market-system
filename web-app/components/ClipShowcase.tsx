import { showcase } from "@/lib/content";
import { FeatureVideo } from "@/components/FeatureVideo";
import { Section } from "@/components/ui/Section";

export function ClipShowcase() {
  return (
    <Section id="see-it-working" tone="navy" eyebrow={showcase.eyebrow} title={showcase.title} intro={showcase.intro}>
      <ul className="grid gap-10 lg:grid-cols-2">
        {showcase.items.map((it) => (
          <li key={it.name} className="min-w-0">
            <FeatureVideo name={it.name} label={it.label} />
            <h3 className="mt-5 text-xl font-bold">{it.title}</h3>
            <p className="mt-2 text-silver">{it.body}</p>
            <ul className="mt-4 grid gap-2 text-sm text-silver">
              {it.points.map((p) => (
                <li key={p} className="flex gap-2">
                  <span aria-hidden="true" className="text-growth">
                    ✓
                  </span>
                  {p}
                </li>
              ))}
            </ul>
          </li>
        ))}
      </ul>
      <p className="mt-8 text-xs text-silver">{showcase.note}</p>
    </Section>
  );
}
