import type { Metadata } from "next";
import { PendingPage } from "@/components/auth/PendingPage";

export const metadata: Metadata = { title: "Reset password", robots: { index: false } };

export default function Page() {
  return <PendingPage title="Reset password" why="Choosing a new password needs a reset link, which depends on the pending forgot-password step. This screen is planned for the next stage." />;
}
