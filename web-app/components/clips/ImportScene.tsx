import { at, ClipStage } from "@/components/clips/ClipStage";

// Clip 2, "Statement import" (research-doc/07-feature-video-scripts.md). Times in seconds match the script.
const rows = [
  ["01 Oct", "RELIANCE", "Buy", "40", "+₹1,240", true],
  ["01 Oct", "TCS", "Sell", "15", "−₹820", false],
  ["05 Oct", "INFY", "Buy", "120", "+₹2,310", true],
  ["06 Oct", "HDFCBANK", "Buy", "60", "−₹450", false],
  ["07 Oct", "BTCUSDT", "Buy", "0.02", "+₹980", true],
] as const;
const brokers = ["Groww", "Zerodha", "Binance", "Exness"]; // plain text names only, no logos

export function ImportScene() {
  return (
    <ClipStage title="Import statement" caption="Drop a statement. Trades appear.">
      <div
        className="absolute rounded-brand border-2 border-dashed"
        style={{ left: 190, top: 100, width: 900, height: 140, borderColor: "rgba(220,227,234,0.4)" }}
      >
        <span className="absolute inset-0 flex items-center justify-center text-silver" style={{ animation: "clip-out 0.4s ease 1.5s both" }}>
          Drop a statement here
        </span>
        <span className="absolute inset-[-2px] rounded-brand border-2" style={{ animation: "clip-border 0.3s ease 1.9s forwards", borderColor: "transparent" }} />
      </div>
      <div
        className="absolute flex items-center gap-3 rounded-brand bg-graphite px-5 text-base"
        style={{ left: 460, top: 142, width: 360, height: 56, animation: "clip-file-in 1.2s cubic-bezier(.22,1,.36,1) 0.8s both" }}
      >
        <span aria-hidden="true">▤</span>
        <span className="font-mono">tradebook_oct2026.xlsx</span>
      </div>

      <div className="absolute rounded-full bg-graphite" style={{ left: 190, top: 270, width: 900, height: 8 }}>
        <div className="h-full origin-left rounded-full bg-electric" style={{ animation: "clip-fill 1s linear 2s both" }} />
      </div>
      <p className="absolute text-sm text-silver" style={{ left: 190, top: 286, ...at(2.0) }}>
        Reading 212 trades
      </p>
      <p className="absolute rounded-full border border-growth px-3 py-0.5 text-xs font-semibold text-growth" style={{ left: 960, top: 286, ...at(5.0) }}>
        ✓ Charges matched
      </p>

      <div className="absolute font-mono text-sm" style={{ left: 190, top: 322, width: 900 }}>
        <div className="grid grid-cols-5 gap-3 border-b border-silver/30 px-2 pb-2 text-silver" style={at(3.0)}>
          <span>Date</span>
          <span>Symbol</span>
          <span>Side</span>
          <span>Qty</span>
          <span>P&amp;L</span>
        </div>
        {rows.map(([date, sym, side, qty, p, gain], i) => (
          <div key={sym} className="grid grid-cols-5 gap-3 border-b border-silver/15 px-2 py-2.5" style={at(3.0 + i * 0.44, "clip-fade", 0.35)}>
            <span>{date}</span>
            <span className="font-semibold">{sym}</span>
            <span>{side}</span>
            <span>{qty}</span>
            <span className={`font-semibold tabular-nums ${gain ? "text-growth" : "text-silver"}`}>{p}</span>
          </div>
        ))}
      </div>

      <div className="absolute flex items-center gap-2 text-sm" style={{ left: 190, top: 594, ...at(5.2, "clip-fade", 0.4) }}>
        <span className="mr-1 text-silver">Statement formats:</span>
        {brokers.map((b, i) => (
          <span key={b} className="rounded-full bg-graphite px-3 py-1 font-semibold" style={{ animation: `clip-lit 0.3s ease ${5.2 + i * 0.35}s both` }}>
            {b}
          </span>
        ))}
      </div>
      <div
        className="absolute flex items-center justify-center rounded-brand bg-electric px-6 text-base font-semibold"
        style={{ left: 870, top: 586, height: 48, animation: "clip-press 0.4s ease 6.6s both" }}
      >
        Import 212 trades
      </div>
    </ClipStage>
  );
}
