import { z } from "zod";

// Choices for opening an account. The enums follow account-service (AccountType, BrokerName); the server validates again.
export const ACCOUNT_TYPES = ["INDIVIDUAL", "JOINT", "CORPORATE", "EQUITY", "DERIVATIVES", "CRYPTO", "FOREX"] as const;
export const BROKERS = ["PLATFORM", "PAPER", "GROWW", "ZERODHA", "BINANCE", "EXNESS", "MANUAL"] as const;
export const CURRENCIES = ["USD", "INR", "EUR", "GBP"] as const;

/** Brokers whose statements can be uploaded (account-service has a parser for each). MANUAL accounts record trades by hand. */
export const STATEMENT_BROKERS = ["GROWW", "ZERODHA", "BINANCE", "EXNESS"] as const;
export const canUploadStatement = (broker: string | undefined) => (STATEMENT_BROKERS as readonly string[]).includes(broker ?? "");

export const MAX_STATEMENT_BYTES = 20 * 1024 * 1024;
export const STATEMENT_EXTENSIONS = [".csv", ".xls", ".xlsx"];

export const openAccountSchema = z.object({
  accountName: z.string().trim().min(1, "Give the account a name").max(100, "Use at most 100 characters"),
  accountType: z.enum(ACCOUNT_TYPES, { error: "Choose an account type" }),
  brokerName: z.enum(BROKERS, { error: "Choose where the account is held" }),
  currency: z.enum(CURRENCIES, { error: "Choose a currency" }),
  remarks: z.string().trim().max(500, "Use at most 500 characters").optional(),
});
export type OpenAccountValues = z.infer<typeof openAccountSchema>;

/** A message for a file that should not even be sent, or null when it looks fine. */
export function statementFileProblem(file: { name: string; size: number }): string | null {
  const lower = file.name.toLowerCase();
  if (!STATEMENT_EXTENSIONS.some((e) => lower.endsWith(e))) return "Choose a CSV, XLS or XLSX file.";
  if (file.size === 0) return "That file is empty.";
  if (file.size > MAX_STATEMENT_BYTES) return "That file is larger than 20 MB.";
  return null;
}
