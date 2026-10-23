"use client";

import { useEffect, useRef, useState, useSyncExternalStore } from "react";

// Muted looping clip with a poster. It loads and plays only while on screen, never autoplays for people who
// prefer reduced motion, and always has a visible pause/play button (WCAG 2.2.2). Files: /video/<name>.webm and .jpg.
const query = "(prefers-reduced-motion: reduce)";
const subscribe = (cb: () => void) => {
  const mq = window.matchMedia(query);
  mq.addEventListener("change", cb);
  return () => mq.removeEventListener("change", cb);
};
const getSnapshot = () => window.matchMedia(query).matches;
const getServerSnapshot = () => false;

export function FeatureVideo({ name, label }: { name: string; label: string }) {
  const ref = useRef<HTMLVideoElement>(null);
  const userPaused = useRef(false);
  const [playing, setPlaying] = useState(false);
  const reduced = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  useEffect(() => {
    const v = ref.current;
    if (!v || reduced) return;
    const io = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !userPaused.current) void v.play().catch(() => {});
        else v.pause();
      },
      { threshold: 0.5 },
    );
    io.observe(v);
    return () => io.disconnect();
  }, [reduced]);

  function toggle() {
    const v = ref.current;
    if (!v) return;
    if (v.paused) {
      userPaused.current = false;
      void v.play().catch(() => {});
    } else {
      userPaused.current = true;
      v.pause();
    }
  }

  return (
    <div className="relative aspect-video w-full overflow-hidden rounded-brand border border-white/15 bg-navy">
      <video
        ref={ref}
        className="h-full w-full object-cover"
        muted
        loop
        playsInline
        preload="none"
        poster={`/video/${name}.jpg`}
        aria-label={label}
        disablePictureInPicture
        onPlay={() => setPlaying(true)}
        onPause={() => setPlaying(false)}
      >
        <source src={`/video/${name}.webm`} type="video/webm" />
      </video>
      <button
        type="button"
        onClick={toggle}
        className="absolute bottom-3 right-3 flex min-h-11 min-w-11 items-center justify-center rounded-brand border border-silver bg-navy px-3 text-sm font-semibold text-white transition-colors hover:bg-white hover:text-navy"
      >
        <span aria-hidden="true">{playing ? "❚❚" : "▶"}</span>
        <span className="sr-only">{playing ? "Pause clip" : "Play clip"}</span>
      </button>
    </div>
  );
}
