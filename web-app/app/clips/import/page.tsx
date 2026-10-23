import type { Metadata } from "next";
import { ImportScene } from "@/components/clips/ImportScene";

// Recording scene only (scripts/record-clips.mjs). Not part of the public site.
export const metadata: Metadata = { title: "Clip: statement import", robots: { index: false, follow: false } };

export default function Page() {
  return <ImportScene />;
}
