import { at, ClipStage } from "@/components/clips/ClipStage";

// Clip 1, "P&L calendar" (research-doc/07-feature-video-scripts.md). Times in seconds match the script.
const pnl = [1240, -820, 2310, -450, 980, 0, 1760, -1320, 640, 2890, -210, 1100, 450, -2040, 1530, 780, -610, 1980, 920, -380, 1450, 2210];
const weeks: (number | null)[][] = [
  [null, null, null, 1, 2],
  [5, 6, 7, 8, 9],
  [12, 13, 14, 15, 16],
  [19, 20, 21, 22, 23],
  [26, 27, 28, 29, 30],
];
const matching = new Set([2, 6, 9, 12, 17, 19]); // days carrying the "Opening range breakout" tag
const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Week"];

const money = (n: number) => {
  if (n === 0) return "₹0";
  const a = Math.abs(n);
  const s = a >= 1000 ? `${(a / 1000).toFixed(1)}k` : String(a);
  return `${n > 0 ? "+" : "−"}₹${s}`;
};
const x = (c: number) => 80 + c * 138;
const y = (r: number) => 186 + r * 82;

const cursor = `
@keyframes cal-cursor {
  0%, 29% { opacity: 0; transform: translate(220px, 640px); }
  30% { opacity: 1; transform: translate(220px, 640px); animation-timing-function: cubic-bezier(.22,1,.36,1); }
  37.5% { transform: translate(430px, 520px); animation-timing-function: cubic-bezier(.22,1,.36,1); }
  47.5% { transform: translate(697px, 305px); }
  70% { transform: translate(697px, 305px); animation-timing-function: cubic-bezier(.22,1,.36,1); }
  75%, 100% { opacity: 1; transform: translate(360px, 120px); }
}`;

export function CalendarScene() {
  let k = 0;
  const cells = weeks.flatMap((week, r) => {
    let total = 0;
    const row = week.map((day, c) => {
      if (day === null) return null;
      const idx = k++;
      const v = pnl[idx];
      total += v;
      const alpha = Math.min(0.5, 0.14 + Math.abs(v) / 6000).toFixed(2);
      const bg = v > 0 ? `rgba(0,200,120,${alpha})` : v < 0 ? `rgba(220,227,234,${(Number(alpha) * 0.55).toFixed(2)})` : "#1E293B";
      const dim = !matching.has(idx);
      const target = r === 1 && c === 4;
      return (
        <div
          key={`d${r}-${c}`}
          className="absolute rounded-brand"
          style={{ left: x(c), top: y(r), width: 130, height: 74, animation: `clip-pop 0.35s ease-out ${0.6 + (r * 5 + c) * 0.045}s both` }}
        >
          <div
            className="flex h-full flex-col justify-between rounded-brand border-2 border-transparent p-2"
            style={{
              background: bg,
              ...(dim ? { animation: "clip-dim 0.5s ease 6.3s both" } : {}),
              ...(target ? { animation: "clip-border 0.2s ease 3.9s forwards" } : {}),
            }}
          >
            <span className="text-xs text-silver">{day}</span>
            <span className={`font-mono text-base font-semibold tabular-nums ${v > 0 ? "text-growth" : "text-silver"}`}>{money(v)}</span>
          </div>
        </div>
      );
    });
    const week_total = (
      <div
        key={`w${r}`}
        className="absolute flex flex-col justify-between rounded-brand bg-graphite p-2"
        style={{ left: x(5), top: y(r), width: 130, height: 74, ...at(2.4 + r * 0.2, "clip-fade", 0.5) }}
      >
        <span className="text-xs text-silver">Week {r + 1}</span>
        <span className={`font-mono text-base font-semibold tabular-nums ${total >= 0 ? "text-growth" : "text-silver"}`}>{money(total)}</span>
      </div>
    );
    return [...row, week_total];
  });

  return (
    <ClipStage title="Dashboard · October 2026" caption="Every trading day at a glance">
      <style>{cursor}</style>

      <div className="absolute left-20 flex h-10 items-center rounded-brand border border-silver/40 px-4 text-sm" style={{ top: 100, width: 320 }}>
        <span className="absolute" style={{ animation: "clip-out 0.3s ease 6.2s both" }}>
          Filter: tag
        </span>
        <span className="absolute" style={at(6.2, "clip-fade", 0.4)}>
          Tag: Opening range breakout
        </span>
      </div>

      {days.map((d, c) => (
        <span key={d} className="absolute font-mono text-xs text-silver" style={{ left: x(c) + 4, top: 160 }}>
          {d}
        </span>
      ))}
      {cells}

      <div
        className="absolute rounded-brand border border-silver/30 bg-graphite p-5"
        style={{ left: 940, top: 100, width: 300, height: 470, animation: "clip-slide-in 0.7s cubic-bezier(.22,1,.36,1) 4.2s both" }}
      >
        <p className="font-display text-lg font-bold">9 Oct · 3 trades</p>
        {[
          ["INFY", "Buy", "+₹2,310", true],
          ["TCS", "Sell", "−₹820", false],
          ["HDFCBANK", "Buy", "+₹270", true],
        ].map(([sym, side, p, gain]) => (
          <div key={String(sym)} className="mt-4 flex items-center justify-between border-b border-silver/20 pb-3 text-sm">
            <span>
              <span className="font-semibold">{sym}</span> <span className="text-silver">{side}</span>
            </span>
            <span className={`font-mono font-semibold ${gain ? "text-growth" : "text-silver"}`}>{p}</span>
          </div>
        ))}
        <p className="mt-5 flex justify-between text-sm">
          <span className="text-silver">Net for the day</span>
          <span className="font-mono font-semibold text-growth">+₹1,760</span>
        </p>
        <p className="mt-6 text-xs text-silver">Tags: Opening range breakout</p>
      </div>

      {/* Rings stay invisible until their click (no backwards fill), then fade out */}
      <span className="absolute rounded-full border-2 border-white opacity-0" style={{ left: 697, top: 305, width: 44, height: 44, animation: "clip-ring 0.55s ease-out 3.85s forwards" }} />
      <span className="absolute rounded-full border-2 border-white opacity-0" style={{ left: 360, top: 120, width: 44, height: 44, animation: "clip-ring 0.55s ease-out 6.0s forwards" }} />
      <span
        className="absolute left-0 top-0 block h-[18px] w-[18px] rounded-full border-[3px] border-navy bg-white shadow-lg"
        style={{ marginLeft: -9, marginTop: -9, animation: "cal-cursor 8s linear both" }}
      />
    </ClipStage>
  );
}
