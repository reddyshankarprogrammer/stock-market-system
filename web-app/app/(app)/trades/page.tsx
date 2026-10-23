import type { Metadata } from "next";
import { PageSkeleton } from "@/components/shell/PageSkeleton";

export const metadata: Metadata = { title: "Trades" };

export default function Page() {
  return <PageSkeleton title="Trades">The trade list and trade entry are planned for the MVP. Platform and tracking accounts will share one screen.</PageSkeleton>;
}
