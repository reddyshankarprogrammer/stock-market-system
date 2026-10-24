import type { Metadata } from "next";
import { JournalPage } from "@/components/terminal/pages/JournalPage";

export const metadata: Metadata = { title: "Journal · Terminal" };

export default function Page() {
  return <JournalPage />;
}
