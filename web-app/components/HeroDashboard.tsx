import { hero } from "@/lib/content";
import { Card } from "@/components/ui/Card";
import { Metric } from "@/components/ui/Metric";

// Built UI, not an image: an equity curve, performance metrics, a portfolio split and an AI coach answer.
// All figures are sample data and the card says so.
const curve = "M0,92 L28,84 L52,88 L80,70 L108,74 L136,52 L164,58 L192,36 L220,40 L248,18 L280,10";

export function HeroDashboard() {
  return (
    <Card tone="dark" className="p-4 shadow-2xl sm:p-5" role="img" aria-label="Sample Quantiva dashboard with equity curve, metrics, portfolio split and AI coach answer">
      <div aria-hidden="true">
        <div className="flex items-center justify-between text-xs text-silver">
          <span className="font-semibold text-white">Performance · last 90 days</span>
          <span>{hero.sampleNote}</span>
        </div>

        <div className="mt-3 rounded-brand bg-navy p-3">
          <svg viewBox="0 0 280 100" className="h-32 w-full sm:h-40" preserveAspectRatio="none">
            <defs>
              <linearGradient id="equity-fill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stopColor="#00C878" stopOpacity="0.35" />
                <stop offset="1" stopColor="#00C878" stopOpacity="0" />
              </linearGradient>
            </defs>
            {[20, 45, 70].map((y) => (
              <line key={y} x1="0" x2="280" y1={y} y2={y} stroke="#DCE3EA" strokeOpacity="0.15" />
            ))}
            <path d={`${curve} L280,100 L0,100 Z`} fill="url(#equity-fill)" />
            <path d={curve} fill="none" stroke="#00C878" strokeWidth="2.5" vectorEffect="non-scaling-stroke" />
          </svg>
        </div>

        <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4">
          {hero.metrics.map((m) => (
            <Metric key={m.label} {...m} />
          ))}
        </div>

        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <div className="min-w-0 rounded-brand bg-navy p-3">
            <p className="text-xs font-semibold uppercase tracking-wide text-silver">Portfolio</p>
            <ul className="mt-2 grid gap-2">
              {hero.allocation.map((a) => (
                <li key={a.name} className="grid grid-cols-[4.5rem_1fr_2.5rem] items-center gap-2 text-xs">
                  <span className="truncate">{a.name}</span>
                  <span className="h-1.5 rounded-full bg-graphite">
                    <span className="block h-full rounded-full bg-electric" style={{ width: `${a.pct}%` }} />
                  </span>
                  <span className="text-right tabular-nums text-silver">{a.pct}%</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="min-w-0 rounded-brand bg-navy p-3 text-xs">
            <p className="font-semibold uppercase tracking-wide text-silver">AI Trading Coach</p>
            <p className="mt-2 rounded-brand bg-graphite px-3 py-2 text-silver">{hero.coach.question}</p>
            <p className="mt-2 rounded-brand bg-electric px-3 py-2 text-white">{hero.coach.answer}</p>
            <p className="mt-2 text-silver">{hero.coach.label}</p>
          </div>
        </div>
      </div>
    </Card>
  );
}
