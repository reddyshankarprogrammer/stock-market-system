"use client";

import Link from "next/link";
import { QueryState } from "@/components/ui/QueryState";
import { useAccount, useTrade } from "@/lib/api/account-hooks";
import { dateTime, money, quantity, signed } from "@/lib/format";

const sourceLabel: Record<string, string> = { PLATFORM: "Quantiva platform", STATEMENT: "Imported from a statement", MANUAL: "Entered by hand" };

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 py-3">
      <dt className="text-subtle">{label}</dt>
      <dd className="text-right font-semibold">{children}</dd>
    </div>
  );
}

export function TradeDetail({ id }: { id: string }) {
  const trade = useTrade(id);
  const t = trade.data;
  const account = useAccount(t?.accountId);
  const cur = account.data?.currency;

  return (
    <div className="grid gap-6">
      <p>
        <Link href={t?.accountId ? `/trades?account=${t.accountId}` : "/trades"} className="inline-flex min-h-11 items-center text-sm font-semibold text-link underline underline-offset-4">
          ← All trades
        </Link>
      </p>
      <QueryState query={trade} what="the trade" next={`/trades/${id}`}>
        {t && (
          <div className="grid gap-4">
            <h1 className="font-display text-3xl font-extrabold tracking-tight">
              {t.side === "BUY" ? "Buy" : "Sell"} {t.symbol}
            </h1>
            <dl className="divide-y divide-line rounded-brand border border-line px-4">
              <Row label="Account">{account.data?.accountName ?? "-"}</Row>
              <Row label="Date">{dateTime(t.tradeDate)}</Row>
              <Row label="Quantity">{quantity(t.quantity)}</Row>
              <Row label="Price">{money(t.price, cur)}</Row>
              <Row label="Gross amount">{money(t.grossAmount, cur)}</Row>
              <Row label="Brokerage and fees">{money((t.feeAmount ?? 0) + (t.brokerage ?? 0) + (t.otherFee ?? 0), cur)}</Row>
              <Row label="Taxes">{money(t.taxAmount, cur)}</Row>
              <Row label="Net amount">{signed(t.netAmount, cur)}</Row>
              {t.side === "SELL" && <Row label="Realised profit">{signed(t.realizedProfit, cur)}</Row>}
              {t.side === "SELL" && t.averageCostAtTrade !== undefined && <Row label="Average cost at the time">{money(t.averageCostAtTrade, cur)}</Row>}
              <Row label="Exchange">{t.exchange ?? "-"}</Row>
              <Row label="Source">{sourceLabel[t.source ?? ""] ?? t.source ?? "-"}</Row>
              {t.notes && <Row label="Notes">{t.notes}</Row>}
            </dl>
          </div>
        )}
      </QueryState>
    </div>
  );
}
