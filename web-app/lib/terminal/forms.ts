import { z } from "zod";
import { EMOTIONS } from "./sample-extra.ts";
import { INSTRUMENTS, STRATEGIES } from "./sample-data.ts";

const symbols = INSTRUMENTS.map((i) => i.symbol) as [string, ...string[]];

/** The order ticket of the terminal preview. A limit or stop order needs a price; a market order has none. */
export const ticketSchema = z
  .object({
    side: z.enum(["Buy", "Sell"], { error: "Choose Buy or Sell" }),
    type: z.enum(["Market", "Limit", "Stop"], { error: "Choose an order type" }),
    symbol: z.enum(symbols, { error: "Choose an instrument" }),
    quantity: z.coerce.number({ error: "Enter a number" }).int("Use a whole number of shares").min(1, "Enter at least 1 share").max(1_000_000, "That is too many shares"),
    limitPrice: z.union([z.literal(""), z.coerce.number({ error: "Enter a number" }).positive("Enter a price above zero").max(1_000_000_000, "That price is too large")]).optional(),
  })
  .superRefine((v, ctx) => {
    if (v.type !== "Market" && (v.limitPrice === "" || v.limitPrice === undefined)) ctx.addIssue({ code: "custom", path: ["limitPrice"], message: v.type === "Limit" ? "A limit order needs a limit price" : "A stop order needs a stop price" });
  });
export type TicketValues = z.input<typeof ticketSchema>;

export const journalSchema = z.object({
  symbol: z.enum(symbols, { error: "Choose an instrument" }),
  strategy: z.enum(STRATEGIES, { error: "Choose a strategy" }),
  emotion: z.enum(EMOTIONS, { error: "Choose how you felt" }),
  rating: z.coerce.number({ error: "Choose a rating" }).int().min(1, "Choose a rating from 1 to 5").max(5, "Choose a rating from 1 to 5"),
  notes: z.string().trim().min(1, "Write a few words about the trade").max(500, "Use at most 500 characters"),
  lesson: z.string().trim().max(300, "Use at most 300 characters").optional(),
});
export type JournalValues = z.input<typeof journalSchema>;
