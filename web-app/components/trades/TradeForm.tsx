"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { FormAlert, SelectField, SubmitButton, TextField } from "@/components/ui/form";
import { useRecordTrade, usePlatformTrade } from "@/lib/api/account-hooks";
import type { Account } from "@/lib/api/account-api";
import { isTracking } from "@/lib/format";
import { manualTradeSchema, platformTradeSchema, type ManualTradeValues, type PlatformTradeValues } from "@/lib/trades";

const today = () => new Date().toISOString().slice(0, 10);

function PlatformForm({ account, onDone }: { account: Account; onDone: () => void }) {
  const trade = usePlatformTrade();
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<PlatformTradeValues>({ resolver: zodResolver(platformTradeSchema), defaultValues: { side: "BUY", symbol: "", quantity: 1 } });

  async function onSubmit(raw: PlatformTradeValues) {
    setError(null);
    const v = platformTradeSchema.parse(raw);
    try {
      await trade.mutateAsync({ side: v.side, accountId: account.id!, symbol: v.symbol.toUpperCase(), quantity: v.quantity });
      onDone();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Something went wrong. Please try again.");
    }
  }
  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="grid gap-5 rounded-brand border border-line bg-panel p-5" aria-label="Record a trade">
      <h2 className="font-display text-xl font-bold">Trade at the market price</h2>
      <p className="text-sm text-subtle">The trade is executed at the current price from market data; cash moves in this account.</p>
      {error && (
        <FormAlert tone="error" title="The trade was not placed">
          {error}
        </FormAlert>
      )}
      <div className="grid gap-5 sm:grid-cols-3">
        <SelectField id="side" label="Side" error={errors.side?.message} {...register("side")}>
          <option value="BUY">Buy</option>
          <option value="SELL">Sell</option>
        </SelectField>
        <TextField id="symbol" label="Symbol" autoComplete="off" error={errors.symbol?.message} {...register("symbol")} />
        <TextField id="quantity" label="Shares" type="number" inputMode="numeric" min={1} step={1} error={errors.quantity?.message} {...register("quantity")} />
      </div>
      <div className="flex flex-wrap gap-3">
        <SubmitButton loading={trade.isPending} loadingText="Placing…">
          Place trade
        </SubmitButton>
        <button type="button" onClick={onDone} className="min-h-11 rounded-brand border border-line px-5 text-sm font-semibold hover:bg-soft">
          Cancel
        </button>
      </div>
    </form>
  );
}

function ManualForm({ account, onDone }: { account: Account; onDone: () => void }) {
  const record = useRecordTrade();
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ManualTradeValues>({ resolver: zodResolver(manualTradeSchema), defaultValues: { side: "BUY", symbol: "", quantity: 1, price: undefined, tradeDate: today(), notes: "" } });

  async function onSubmit(raw: ManualTradeValues) {
    setError(null);
    const v = manualTradeSchema.parse(raw);
    try {
      await record.mutateAsync({
        accountId: account.id!,
        side: v.side,
        symbol: v.symbol.toUpperCase(),
        quantity: v.quantity,
        ...(v.side === "BUY" ? { buyPrice: v.price } : { sellPrice: v.price }),
        tradeDate: `${v.tradeDate}T00:00:00`,
        notes: v.notes || undefined,
      });
      onDone();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Something went wrong. Please try again.");
    }
  }
  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="grid gap-5 rounded-brand border border-line bg-panel p-5" aria-label="Record a trade">
      <h2 className="font-display text-xl font-bold">Record a trade</h2>
      <p className="text-sm text-subtle">For a trade that already happened at your broker. It is only recorded: no cash moves here.</p>
      {error && (
        <FormAlert tone="error" title="The trade was not recorded">
          {error}
        </FormAlert>
      )}
      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        <SelectField id="side" label="Side" error={errors.side?.message} {...register("side")}>
          <option value="BUY">Buy</option>
          <option value="SELL">Sell</option>
        </SelectField>
        <TextField id="symbol" label="Symbol" autoComplete="off" error={errors.symbol?.message} {...register("symbol")} />
        <TextField id="quantity" label="Quantity" type="number" inputMode="decimal" step="any" error={errors.quantity?.message} {...register("quantity")} />
        <TextField id="price" label="Price" type="number" inputMode="decimal" step="any" error={errors.price?.message} {...register("price")} />
      </div>
      <div className="grid gap-5 sm:grid-cols-2">
        <TextField id="tradeDate" label="Trade date" type="date" max={today()} error={errors.tradeDate?.message} {...register("tradeDate")} />
        <TextField id="notes" label="Notes (optional)" autoComplete="off" error={errors.notes?.message} {...register("notes")} />
      </div>
      <div className="flex flex-wrap gap-3">
        <SubmitButton loading={record.isPending} loadingText="Recording…">
          Record trade
        </SubmitButton>
        <button type="button" onClick={onDone} className="min-h-11 rounded-brand border border-line px-5 text-sm font-semibold hover:bg-soft">
          Cancel
        </button>
      </div>
    </form>
  );
}

export function TradeForm({ account, onDone }: { account: Account; onDone: () => void }) {
  return isTracking(account.brokerName) ? <ManualForm account={account} onDone={onDone} /> : <PlatformForm account={account} onDone={onDone} />;
}
