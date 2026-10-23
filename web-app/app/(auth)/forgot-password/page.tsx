import type { Metadata } from "next";
import { ForgotPasswordForm } from "@/components/auth/ForgotPasswordForm";

export const metadata: Metadata = { title: "Forgot password", robots: { index: false } };

export default function Page() {
  return <ForgotPasswordForm />;
}
