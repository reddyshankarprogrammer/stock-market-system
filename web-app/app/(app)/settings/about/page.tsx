import type { Metadata } from "next";
import { AboutSettings } from "@/components/settings/AboutSettings";

export const metadata: Metadata = { title: "About" };

export default function Page() {
  return <AboutSettings />;
}
