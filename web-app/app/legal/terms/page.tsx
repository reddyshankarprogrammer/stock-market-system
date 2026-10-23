import type { Metadata } from "next";
import { LegalPage } from "@/components/LegalPage";
import { legal } from "@/lib/content";

export const metadata: Metadata = { title: legal.terms.title, robots: { index: false } };

export default function Page() {
  return <LegalPage {...legal.terms} />;
}
