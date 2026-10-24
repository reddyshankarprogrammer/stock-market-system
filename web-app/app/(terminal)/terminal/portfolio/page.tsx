import type { Metadata } from "next";
import { PortfolioPage } from "@/components/terminal/pages/PortfolioPage";

export const metadata: Metadata = { title: "Portfolio · Terminal" };

export default function Page() {
  return <PortfolioPage />;
}
