import type { Metadata } from "next";
import { PageSkeleton } from "@/components/shell/PageSkeleton";

export const metadata: Metadata = { title: "Portfolio" };

export default function Page() {
  return <PageSkeleton title="Portfolio">Holdings, profit and loss, and the AI coach summary are planned for the MVP.</PageSkeleton>;
}
