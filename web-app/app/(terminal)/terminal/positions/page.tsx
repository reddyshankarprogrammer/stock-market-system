import type { Metadata } from "next";
import { PositionsPage } from "@/components/terminal/pages/PositionsPage";

export const metadata: Metadata = { title: "Positions · Terminal" };

export default function Page() {
  return <PositionsPage />;
}
