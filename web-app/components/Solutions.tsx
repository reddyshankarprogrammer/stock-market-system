"use client";

import { useId, useRef, useState, type KeyboardEvent } from "react";
import { solutions } from "@/lib/content";
import { Section } from "@/components/ui/Section";

// Full ARIA tabs pattern: arrow keys, Home and End move between tabs; only the active tab is in the tab order.
export function Solutions() {
  const [active, setActive] = useState(0);
  const uid = useId();
  const refs = useRef<(HTMLButtonElement | null)[]>([]);
  const last = solutions.tabs.length - 1;

  function onKeyDown(e: KeyboardEvent<HTMLDivElement>) {
    const keys: Record<string, number> = {
      ArrowRight: active === last ? 0 : active + 1,
      ArrowLeft: active === 0 ? last : active - 1,
      Home: 0,
      End: last,
    };
    if (!(e.key in keys)) return;
    e.preventDefault();
    const next = keys[e.key];
    setActive(next);
    refs.current[next]?.focus();
  }

  return (
    <Section id="solutions" tone="silver" eyebrow={solutions.eyebrow} title={solutions.title} intro={solutions.intro}>
      <div role="tablist" aria-label="Solutions by audience" onKeyDown={onKeyDown} className="flex flex-wrap gap-2">
        {solutions.tabs.map((t, idx) => (
          <button
            key={t.id}
            ref={(el) => {
              refs.current[idx] = el;
            }}
            id={`${uid}-tab-${t.id}`}
            role="tab"
            type="button"
            aria-selected={idx === active}
            aria-controls={`${uid}-panel-${t.id}`}
            tabIndex={idx === active ? 0 : -1}
            onClick={() => setActive(idx)}
            className={`min-h-11 rounded-brand border px-5 py-2 text-sm font-semibold transition-colors ${
              idx === active ? "border-navy bg-navy text-white" : "border-graphite bg-white text-navy hover:bg-navy hover:text-white"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {solutions.tabs.map((t, idx) => (
        <div
          key={t.id}
          id={`${uid}-panel-${t.id}`}
          role="tabpanel"
          aria-labelledby={`${uid}-tab-${t.id}`}
          hidden={idx !== active}
          tabIndex={0}
          className="mt-6 rounded-brand border border-silver bg-white p-6 sm:p-8"
        >
          <h3 className="text-2xl font-bold">{t.heading}</h3>
          <p className="mt-3 max-w-2xl text-graphite">{t.body}</p>
          <ul className="mt-5 grid gap-2.5">
            {t.points.map((p) => (
              <li key={p} className="flex gap-3 text-sm">
                <span aria-hidden="true" className="text-electric">
                  ✓
                </span>
                {p}
              </li>
            ))}
          </ul>
        </div>
      ))}
    </Section>
  );
}
