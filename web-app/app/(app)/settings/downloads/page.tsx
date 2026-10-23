import type { Metadata } from "next";
import { DownloadSettings } from "@/components/settings/DownloadSettings";

export const metadata: Metadata = { title: "Downloads" };

export default function Page() {
  return <DownloadSettings />;
}
