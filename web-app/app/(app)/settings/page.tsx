import type { Metadata } from "next";
import { PageSkeleton } from "@/components/shell/PageSkeleton";

export const metadata: Metadata = { title: "Settings" };

export default function Page() {
  return <PageSkeleton title="Settings">Profile, security, notifications and preferences are planned next (plan item 8).</PageSkeleton>;
}
