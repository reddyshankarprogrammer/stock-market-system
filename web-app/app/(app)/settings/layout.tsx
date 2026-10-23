import type { Metadata } from "next";
import type { ReactNode } from "react";
import { SettingsNav } from "@/components/settings/SettingsNav";

export const metadata: Metadata = { title: { template: "%s · Settings", default: "Settings" } };

// Left-hand category list with the options on the right (a row of tabs above on phones).
export default function SettingsLayout({ children }: { children: ReactNode }) {
  return (
    <div className="grid grid-cols-[minmax(0,1fr)] gap-6 lg:grid-cols-[11rem_minmax(0,1fr)] lg:gap-10">
      <SettingsNav />
      <div className="min-w-0 max-w-2xl">{children}</div>
    </div>
  );
}
