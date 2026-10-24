import Image from "next/image";
import Link from "next/link";

// Logo for the app screens: the icon sits on a white tile so its navy parts stay visible in dark mode.
export function Brand({ onDark = false, compact = false }: { onDark?: boolean; compact?: boolean }) {
  return (
    <Link href="/" className="inline-flex items-center gap-2.5" aria-label="Quantiva home">
      <span className="rounded-brand bg-white p-1">
        <Image src="/brand/quantiva-icon-192.png" alt="" width={32} height={32} priority />
      </span>
      <span className={compact ? "sr-only" : `font-display text-xl font-extrabold tracking-tight ${onDark ? "text-white" : "text-ink"}`}>
        Quantiva<sup className="text-[0.55em] font-semibold">™</sup>
      </span>
    </Link>
  );
}
