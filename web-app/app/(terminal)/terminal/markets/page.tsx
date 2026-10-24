import type { Metadata } from "next";
import { MarketsPage } from "@/components/terminal/pages/MarketsPage";

export const metadata: Metadata = { title: "Markets · Terminal" };

export default function Page() {
  return <MarketsPage />;
}
