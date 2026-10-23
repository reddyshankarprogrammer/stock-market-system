import type { CSSProperties, ReactNode } from "react";

// Fixed 1280x720 stage shared by every clip: title, "Sample data" badge and caption (max 6 words, stays to the end frame).
export function ClipStage({ title, caption, children }: { title: string; caption: string; children: ReactNode }) {
  return (
    <div className="clip-stage" role="img" aria-label={`${title}. ${caption}. Animated clip using sample data.`}>
      <div aria-hidden="true">
        <p className="absolute left-20 top-7 font-display text-2xl font-bold">{title}</p>
        <p className="absolute right-20 top-7 rounded-full border border-silver/40 px-3 py-1 text-sm text-silver">Sample data</p>
        {children}
        <p
          className="absolute bottom-9 left-20 font-display text-3xl font-extrabold"
          style={{ animation: "clip-fade 0.5s ease-out 0.5s both" }}
        >
          {caption}
        </p>
      </div>
    </div>
  );
}

export const at = (delay: number, name = "clip-fade", dur = 0.4, extra = "ease-out"): CSSProperties => ({
  animation: `${name} ${dur}s ${extra} ${delay}s both`,
});
