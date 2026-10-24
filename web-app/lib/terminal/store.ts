"use client";

import { create } from "zustand";
import { AS_OF } from "./sample-data.ts";
import { JOURNAL, type JournalEntry } from "./sample-extra.ts";
import { ORDERS, newOrder, type Order, type TicketInput } from "./sample-extra.ts";

// In-memory state of the terminal preview: orders from the ticket and journal entries live here until the page is
// reloaded. Nothing is sent to a backend; that is said on the pages.
type TerminalState = {
  orders: Order[];
  journal: JournalEntry[];
  seq: number;
  placeOrder: (input: TicketInput) => Order;
  cancelOrder: (id: string) => void;
  addJournal: (entry: Omit<JournalEntry, "id" | "pnl">) => void;
};

export const useTerminalStore = create<TerminalState>((set, get) => ({
  orders: ORDERS,
  journal: JOURNAL,
  seq: 1,
  placeOrder: (input) => {
    // The preview lives on the sample day: a new order gets today's time of day, but not earlier than 09:00, so it sorts above the sample orders of that day.
    const clock = new Date().toTimeString().slice(0, 8);
    const order = newOrder(input, `${AS_OF}T${clock < "09:00:00" ? "09:00:00" : clock}`, get().seq);
    set((s) => ({ orders: [order, ...s.orders], seq: s.seq + 1 }));
    return order;
  },
  // Only an open order can be cancelled; a filled one stays as it is.
  cancelOrder: (id) => set((s) => ({ orders: s.orders.map((o) => (o.id === id && o.status === "Open" ? { ...o, status: "Cancelled" as const, note: "Cancelled by you (sample)" } : o)) })),
  addJournal: (entry) => set((s) => ({ journal: [{ ...entry, id: `JN${String(s.seq).padStart(4, "0")}`, pnl: null }, ...s.journal], seq: s.seq + 1 })),
}));
