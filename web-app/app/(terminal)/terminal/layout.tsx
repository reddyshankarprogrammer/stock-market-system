import type { Metadata } from "next";
import type { ReactNode } from "react";
import { TerminalShell } from "@/components/terminal/TerminalShell";

export const metadata: Metadata = { title: "Terminal", robots: { index: false, follow: false } };

// Protected by proxy.ts. Dark only, whatever the visitor's theme: the terminal screens follow the design brief of 24 Oct 2026.
export default function TerminalLayout({ children }: { children: ReactNode }) {
  return <TerminalShell>{children}</TerminalShell>;
}
