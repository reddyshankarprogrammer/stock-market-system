import type { Art } from "@/lib/content";

// Small animated product illustrations in brand colours. Each plays once and stays on its final state.
const d = (i: number) => ({ animationDelay: `${i * 90}ms` });

export function Illustration({ kind }: { kind: Art }) {
  return (
    <svg viewBox="0 0 240 110" className="h-28 w-full rounded-brand bg-navy" role="img" aria-label={`${kind} illustration of the feature`}>
      <title>Illustration</title>
      {kind === "line" && (
        <>
          <path d="M0 80 H240 M0 50 H240 M0 20 H240" stroke="#DCE3EA" strokeOpacity="0.15" fill="none" />
          <path
            pathLength={400}
            className="qv-draw"
            d="M10 90 L45 74 L75 82 L105 56 L135 62 L165 38 L195 44 L230 16"
            fill="none"
            stroke="#00C878"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </>
      )}
      {kind === "bars" &&
        [34, 52, 40, 68, 58, 80].map((h, i) => (
          <rect key={i} className="qv-grow" style={d(i)} x={22 + i * 34} y={96 - h} width="20" height={h} rx="3" fill={i % 2 ? "#146EF5" : "#00C878"} />
        ))}
      {kind === "chat" && (
        <>
          <rect className="qv-rise" x="20" y="16" width="120" height="26" rx="8" fill="#1E293B" />
          <rect className="qv-rise" style={d(3)} x="76" y="52" width="148" height="44" rx="8" fill="#146EF5" />
          <path d="M92 68 H206 M92 80 H178" stroke="#FFFFFF" strokeWidth="3" strokeLinecap="round" />
        </>
      )}
      {kind === "table" &&
        [0, 1, 2, 3].map((i) => (
          <g key={i} className="qv-rise" style={d(i * 2)}>
            <rect x="20" y={16 + i * 22} width="200" height="16" rx="4" fill="#1E293B" />
            <rect x="26" y={20 + i * 22} width="50" height="8" rx="3" fill="#DCE3EA" />
            <rect x="170" y={20 + i * 22} width="40" height="8" rx="3" fill={i % 2 ? "#DCE3EA" : "#00C878"} />
          </g>
        ))}
      {kind === "nodes" && (
        <>
          <path d="M50 55 L120 25 M50 55 L120 85 M120 25 L190 55 M120 85 L190 55" stroke="#DCE3EA" strokeOpacity="0.5" strokeWidth="2" fill="none" />
          {[
            [50, 55],
            [120, 25],
            [120, 85],
            [190, 55],
          ].map(([cx, cy], i) => (
            <circle key={i} className="qv-rise" style={d(i * 2)} cx={cx} cy={cy} r="11" fill={i === 3 ? "#00C878" : "#146EF5"} />
          ))}
        </>
      )}
    </svg>
  );
}
