type Props = {
  label: string;
  value: string;
  /** Change versus the previous period; the sign sets the arrow. */
  delta?: number;
  /** True for measures where a fall is good, such as drawdown. */
  lowerIsBetter?: boolean;
};

// Improvement is green. Worsening is silver with a minus sign: the palette has no loss colour yet.
export function Metric({ label, value, delta, lowerIsBetter = false }: Props) {
  const improved = delta !== undefined && (lowerIsBetter ? delta < 0 : delta > 0);
  return (
    <div className="min-w-0">
      <p className="text-xs font-medium uppercase tracking-wide text-silver">{label}</p>
      <p className="mt-1 font-display text-2xl font-bold tabular-nums">{value}</p>
      {delta !== undefined && (
        <p className={`mt-0.5 text-xs font-semibold tabular-nums ${improved ? "text-growth" : "text-silver"}`}>
          {delta >= 0 ? "▲ +" : "▼ −"}
          {Math.abs(delta)}
          <span className="sr-only">{improved ? " improved" : " worsened"}</span>
        </p>
      )}
    </div>
  );
}
