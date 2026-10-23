import type { ReactNode } from "react";

/** A screen that is planned but not built yet: a heading, an honest status line and grey placeholder blocks. */
export function PageSkeleton({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="grid gap-6">
      <h1 className="font-display text-3xl font-extrabold tracking-tight">{title}</h1>
      <p className="rounded-brand border border-line bg-soft p-4 text-sm text-subtle">{children}</p>
      <div aria-hidden="true" className="grid gap-4 sm:grid-cols-2">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="h-28 animate-pulse rounded-brand bg-soft motion-reduce:animate-none" />
        ))}
      </div>
    </div>
  );
}
