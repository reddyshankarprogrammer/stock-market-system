import type { Metadata } from "next";
import { PendingPage } from "@/components/auth/PendingPage";

export const metadata: Metadata = { title: "Verify email", robots: { index: false } };

export default function Page() {
  return <PendingPage title="Verify your email" why="Email verification with a 6-digit code (verify and resend) is pending. It needs an email service and verification endpoints that do not exist yet." />;
}
