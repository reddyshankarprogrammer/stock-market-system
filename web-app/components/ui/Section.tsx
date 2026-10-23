import type { ReactNode } from "react";

type Props = {
  id?: string;
  tone?: "light" | "silver" | "navy";
  eyebrow?: string;
  title?: string;
  intro?: string;
  children: ReactNode;
};

const tones = {
  light: "bg-white text-navy",
  silver: "bg-silver/40 text-navy",
  navy: "bg-navy text-white",
};

// Every section shares the same vertical rhythm, container width and heading scale.
export function Section({ id, tone = "light", eyebrow, title, intro, children }: Props) {
  return (
    <section id={id} className={`${tones[tone]} py-section`} aria-labelledby={id ? `${id}-title` : undefined}>
      <div className="mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8 2xl:max-w-[1600px]">
        {(eyebrow || title || intro) && (
          <header className="mb-10 max-w-3xl">
            {eyebrow && (
              <p className={`text-sm font-bold uppercase tracking-widest ${tone === "navy" ? "text-growth" : "text-navy"}`}>
                {eyebrow}
              </p>
            )}
            {title && (
              <h2 id={id ? `${id}-title` : undefined} className="mt-2 text-3xl font-extrabold tracking-tight sm:text-4xl">
                {title}
              </h2>
            )}
            {intro && <p className={`mt-4 text-lg ${tone === "navy" ? "text-silver" : "text-graphite"}`}>{intro}</p>}
          </header>
        )}
        {children}
      </div>
    </section>
  );
}
