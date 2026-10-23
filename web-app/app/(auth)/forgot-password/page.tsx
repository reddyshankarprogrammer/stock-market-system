import type { Metadata } from "next";
import { PendingPage } from "@/components/auth/PendingPage";

export const metadata: Metadata = { title: "Forgot password", robots: { index: false } };

export default function Page() {
  return <PendingPage title="Forgot password" why="Password reset by email is not available yet. It needs a reset endpoint and an email service, which are planned. Sending a reset link is pending." />;
}
