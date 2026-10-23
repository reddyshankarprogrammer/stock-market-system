import Link from "next/link";
import { SettingsHeading } from "@/components/settings/controls";

const linkClass = "inline-flex min-h-11 items-center font-semibold text-link underline underline-offset-4 hover:decoration-2";

// Static: no data to load. The version comes from package.json at build time (next.config.ts).
export function AboutSettings() {
  const version = process.env.NEXT_PUBLIC_APP_VERSION ?? "dev";
  return (
    <div className="grid gap-6">
      <SettingsHeading title="About">Quantiva web app.</SettingsHeading>
      <dl className="divide-y divide-line rounded-brand border border-line px-4">
        <div className="flex items-center justify-between gap-4 py-3">
          <dt className="font-semibold">Version</dt>
          <dd>{version}</dd>
        </div>
        <div className="flex items-center justify-between gap-4 py-3">
          <dt className="font-semibold">Release notes</dt>
          <dd className="text-subtle">Not published yet</dd>
        </div>
        <div className="flex items-center justify-between gap-4 py-3">
          <dt className="font-semibold">Licence</dt>
          <dd className="text-subtle">Not published yet</dd>
        </div>
      </dl>
      <nav aria-label="Legal and support" className="flex flex-wrap gap-x-6">
        <Link href="/legal/privacy" className={linkClass}>
          Privacy policy
        </Link>
        <Link href="/legal/terms" className={linkClass}>
          Terms &amp; conditions
        </Link>
        <Link href="/legal/risk" className={linkClass}>
          Risk disclaimer
        </Link>
        <Link href="/contact" className={linkClass}>
          Contact support
        </Link>
      </nav>
    </div>
  );
}
