import { faq } from "@/lib/content";
import { Section } from "@/components/ui/Section";

// The same list feeds the FAQPage JSON-LD in app/page.tsx, so the structured data always matches what is visible.
export function Faq() {
  return (
    <Section id="faq" title={faq.title}>
      <div className="max-w-3xl border-t border-silver">
        {faq.items.map((f) => (
          <details key={f.q} className="group border-b border-silver">
            <summary className="flex min-h-11 cursor-pointer list-none items-center justify-between gap-4 py-4 text-left font-semibold [&::-webkit-details-marker]:hidden">
              {f.q}
              <span aria-hidden="true" className="text-xl text-graphite transition-transform group-open:rotate-45">
                +
              </span>
            </summary>
            <p className="pb-5 text-graphite">{f.a}</p>
          </details>
        ))}
      </div>
    </Section>
  );
}
