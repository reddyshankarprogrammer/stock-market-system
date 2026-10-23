import type { Metadata } from "next";
import { CalendarScene } from "@/components/clips/CalendarScene";

// Recording scene only (scripts/record-clips.mjs). Not part of the public site.
export const metadata: Metadata = { title: "Clip: P&L calendar", robots: { index: false, follow: false } };

export default function Page() {
  return <CalendarScene />;
}
