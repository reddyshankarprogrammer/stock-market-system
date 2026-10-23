import type { Metadata } from "next";
import { LegalPage } from "@/components/LegalPage";
import { legal } from "@/lib/content";

// The risk disclosure is real text, so this page can be indexed.
export const metadata: Metadata = { title: legal.risk.title };

export default function Page() {
  return <LegalPage {...legal.risk} />;
}
