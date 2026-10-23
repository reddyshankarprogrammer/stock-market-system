"use client";

import { useState } from "react";
import { pricing } from "@/lib/content";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Section } from "@/components/ui/Section";

// Prices are TBC. The toggle changes the billing period label so the layout is ready for real prices.
export function Pricing() {
  const [annual, setAnnual] = useState(false);
  const period = annual ? "per year" : "per month";

  return (
    <Section id="pricing" eyebrow={pricing.eyebrow} title={pricing.title} intro={pricing.intro}>
      <div role="group" aria-label="Billing period" className="mb-8 inline-flex rounded-brand border border-graphite p-1">
        {[
          { label: "Monthly", value: false },
          { label: "Annual", value: true },
        ].map((o) => (
          <button
            key={o.label}
            type="button"
            aria-pressed={annual === o.value}
            onClick={() => setAnnual(o.value)}
            className={`min-h-11 rounded-brand px-5 text-sm font-semibold transition-colors ${
              annual === o.value ? "bg-navy text-white" : "text-navy hover:bg-silver"
            }`}
          >
            {o.label}
          </button>
        ))}
      </div>

      <ul className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {pricing.plans.map((p) => (
          <li key={p.name} className="min-w-0">
            <Card className="flex h-full flex-col gap-4">
              <div>
                <h3 className="text-xl font-bold">{p.name}</h3>
                <p className="text-sm text-graphite">{p.audience}</p>
              </div>
              <p className="font-display text-3xl font-extrabold">
                {pricing.priceText}
                <span className="ml-1 text-sm font-medium text-graphite">{period}</span>
              </p>
              <ul className="grid gap-2 text-sm text-graphite">
                {p.points.map((pt) => (
                  <li key={pt} className="flex gap-2">
                    <span aria-hidden="true" className="text-electric">
                      ✓
                    </span>
                    {pt}
                  </li>
                ))}
              </ul>
              <Button href={p.cta.href} variant={p.name === "Enterprise" ? "secondary" : "primary"} className="mt-auto">
                {p.cta.label}
                <span className="sr-only"> for {p.name}</span>
              </Button>
            </Card>
          </li>
        ))}
      </ul>

      <h3 className="mt-14 text-xl font-bold">Compare plans</h3>
      <div
        role="region"
        aria-label="Plan comparison table, scrolls sideways on small screens"
        tabIndex={0}
        className="mt-4 overflow-x-auto rounded-brand border border-silver"
      >
        <table className="w-full min-w-[44rem] border-collapse text-left text-sm">
          <caption className="sr-only">Proposed contents of each plan. Prices and limits are not final.</caption>
          <thead className="bg-silver/50">
            <tr>
              <th scope="col" className="px-4 py-3 font-bold">
                Feature
              </th>
              {pricing.plans.map((p) => (
                <th key={p.name} scope="col" className="px-4 py-3 font-bold">
                  {p.name}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {pricing.rows.map((r) => (
              <tr key={r.feature} className="border-t border-silver">
                <th scope="row" className="px-4 py-3 font-medium">
                  {r.feature}
                </th>
                {r.values.map((v, i) => (
                  <td key={i} className="px-4 py-3">
                    {v === true ? (
                      <>
                        <span aria-hidden="true" className="font-bold text-electric">
                          ✓
                        </span>
                        <span className="sr-only">Included</span>
                      </>
                    ) : v === false ? (
                      <>
                        <span aria-hidden="true">–</span>
                        <span className="sr-only">Not included</span>
                      </>
                    ) : (
                      v
                    )}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="mt-3 text-sm text-graphite">Proposed contents; prices and limits are not final.</p>
    </Section>
  );
}
