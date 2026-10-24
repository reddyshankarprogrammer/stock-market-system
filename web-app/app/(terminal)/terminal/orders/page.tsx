import type { Metadata } from "next";
import { OrdersPage } from "@/components/terminal/pages/OrdersPage";

export const metadata: Metadata = { title: "Orders · Terminal" };

export default function Page() {
  return <OrdersPage />;
}
