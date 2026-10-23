// Display formatting shared by the account screens. Amounts are shown, never calculated, here: every figure comes from
// account-service, so the screen and the API cannot disagree.

/** An amount in its own currency; an unknown currency code must not break the page. */
export function money(value: number | undefined | null, currency: string | undefined): string {
  if (value === undefined || value === null) return "-";
  try {
    return new Intl.NumberFormat("en", { style: "currency", currency: currency ?? "USD" }).format(value);
  } catch {
    return `${value} ${currency ?? ""}`.trim();
  }
}

/** Quantities and prices: up to 4 decimals, no trailing zeros. */
export function quantity(value: number | undefined | null): string {
  if (value === undefined || value === null) return "-";
  return new Intl.NumberFormat("en", { maximumFractionDigits: 4 }).format(value);
}

export function percent(value: number | undefined | null): string {
  if (value === undefined || value === null) return "-";
  return `${new Intl.NumberFormat("en", { maximumFractionDigits: 2, signDisplay: "exceptZero" }).format(value)}%`;
}

/** 2026-10-24T09:20:11 -> 24 Oct 2026. Dates come without a zone and are shown as they are, never shifted. */
export function dateOnly(value: string | undefined | null): string {
  if (!value) return "-";
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
  if (!m) return value;
  return `${Number(m[3])} ${["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"][Number(m[2]) - 1]} ${m[1]}`;
}

export function dateTime(value: string | undefined | null): string {
  if (!value) return "-";
  const m = /T(\d{2}:\d{2})/.exec(value);
  return m ? `${dateOnly(value)} ${m[1]}` : dateOnly(value);
}

export const brokerLabel: Record<string, string> = { PLATFORM: "Quantiva platform", PAPER: "Paper trading", GROWW: "Groww", ZERODHA: "Zerodha", BINANCE: "Binance", EXNESS: "Exness", MANUAL: "Manual" };
export const accountTypeLabel: Record<string, string> = { INDIVIDUAL: "Individual", JOINT: "Joint", CORPORATE: "Corporate", EQUITY: "Equity", DERIVATIVES: "Derivatives", CRYPTO: "Crypto", FOREX: "Forex" };

/** Platform and paper accounts hold cash; every other broker is a tracking account that never moves cash. */
export const isTracking = (broker: string | undefined) => broker !== undefined && broker !== "PLATFORM" && broker !== "PAPER";

/** A profit or loss as a text a screen reader can use as well as a colour: "+1,000.00" or "-250.00". */
export function signed(value: number | undefined | null, currency: string | undefined): string {
  if (value === undefined || value === null) return "-";
  const text = money(Math.abs(value), currency);
  return value > 0 ? `+${text}` : value < 0 ? `-${text}` : text;
}
