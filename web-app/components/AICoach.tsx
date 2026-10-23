"use client";

import { useState } from "react";
import { aiDemo } from "@/lib/content";
import { Section } from "@/components/ui/Section";

// A scripted demo, clearly labelled: no backend and no AI call. Every bar prints its value, so colour is never the only cue.
export function AICoach() {
  const [i, setI] = useState(0);
  const item = aiDemo.questions[i];
  const max = Math.max(...item.bars.map((b) => b.value));

  return (
    <Section id="ai" tone="navy" eyebrow={aiDemo.eyebrow} title={aiDemo.title} intro={aiDemo.intro}>
      <div className="grid items-start gap-10 lg:grid-cols-[1fr_1.15fr]">
        <ul className="grid gap-3 text-silver">
          {aiDemo.points.map((p) => (
            <li key={p} className="flex gap-3">
              <span aria-hidden="true" className="mt-0.5 text-growth">
                ✓
              </span>
              {p}
            </li>
          ))}
        </ul>

        <div className="min-w-0 rounded-brand border border-white/15 bg-graphite p-5">
          <p className="text-xs text-silver">{aiDemo.label}</p>

          <div role="group" aria-label="Choose an example question" className="mt-4 flex flex-wrap gap-2">
            {aiDemo.questions.map((qq, idx) => (
              <button
                key={qq.q}
                type="button"
                aria-pressed={idx === i}
                onClick={() => setI(idx)}
                className={`min-h-11 rounded-brand border px-4 py-2 text-left text-sm font-semibold transition-colors ${
                  idx === i ? "border-electric bg-electric text-white" : "border-silver text-white hover:bg-white hover:text-navy"
                }`}
              >
                {qq.q}
              </button>
            ))}
          </div>

          <div aria-live="polite" className="mt-5 grid gap-4">
            <p className="ml-auto max-w-[85%] rounded-brand bg-navy px-4 py-2.5 text-sm">{item.q}</p>
            <div className="rounded-brand border border-white/15 bg-navy p-4 text-sm">
              <p>{item.a}</p>
              <ul className="mt-4 grid gap-2" aria-label={`Chart: ${item.unit}`}>
                {item.bars.map((b) => {
                  const good = b.value >= item.goodAt;
                  return (
                    <li key={b.label} className="grid grid-cols-[5.5rem_1fr_3rem] items-center gap-2 text-xs">
                      <span className="truncate text-silver">{b.label}</span>
                      <span className="h-2.5 rounded-full bg-graphite">
                        <span
                          className={`block h-full rounded-full ${good ? "bg-growth" : "bg-silver"}`}
                          style={{ width: `${Math.round((b.value / max) * 100)}%` }}
                        />
                      </span>
                      <span className="text-right font-semibold tabular-nums">{b.text}</span>
                    </li>
                  );
                })}
              </ul>
              <p className="mt-4 inline-block rounded-full border border-white/20 px-3 py-1 text-xs text-silver">{aiDemo.cite}</p>
            </div>
          </div>
          <p className="mt-4 text-xs text-silver">{aiDemo.disclaimer}</p>
        </div>
      </div>
    </Section>
  );
}
