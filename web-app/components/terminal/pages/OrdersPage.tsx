"use client";

import { useMemo, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Search } from "lucide-react";
import { DataTable, type Column } from "@/components/terminal/DataTable";
import { PageFrame, Stat, fieldClass, panel, usd } from "@/components/terminal/PageFrame";
import { ticketSchema, type TicketValues } from "@/lib/terminal/forms";
import { INSTRUMENTS } from "@/lib/terminal/sample-data";
import { ORDER_STATUSES, type Order, type OrderStatus } from "@/lib/terminal/sample-extra";
import { useTerminalStore } from "@/lib/terminal/store";

const label = "text-xs font-semibold text-term-muted";
const err = "text-xs font-semibold text-term-loss";

function Ticket() {
  const place = useTerminalStore((s) => s.placeOrder);
  const [placed, setPlaced] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<TicketValues>({ resolver: zodResolver(ticketSchema), defaultValues: { side: "Buy", type: "Market", symbol: "AAPL", quantity: 10, limitPrice: "" } });
  const type = useWatch({ control, name: "type" });

  function onSubmit(raw: TicketValues) {
    const v = ticketSchema.parse(raw);
    const o = place({ symbol: v.symbol, side: v.side, type: v.type, quantity: v.quantity, limitPrice: v.limitPrice === "" || v.limitPrice === undefined ? null : v.limitPrice });
    setPlaced(`${o.side} ${o.quantity} ${o.symbol} (${o.type.toLowerCase()}) added as ${o.id}. It is only in this sample list: nothing was sent to a broker.`);
    reset({ ...raw, quantity: 10, limitPrice: "" });
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate aria-label="Order ticket" className={`${panel} grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-6`}>
      <h2 className="font-display text-base font-bold sm:col-span-2 lg:col-span-6">Order ticket (sample)</h2>
      <div className="grid gap-1">
        <label htmlFor="t-side" className={label}>Side</label>
        <select id="t-side" className={fieldClass} {...register("side")}><option>Buy</option><option>Sell</option></select>
      </div>
      <div className="grid gap-1">
        <label htmlFor="t-type" className={label}>Type</label>
        <select id="t-type" className={fieldClass} {...register("type")}><option>Market</option><option>Limit</option><option>Stop</option></select>
      </div>
      <div className="grid gap-1">
        <label htmlFor="t-symbol" className={label}>Instrument</label>
        <select id="t-symbol" className={fieldClass} {...register("symbol")}>{INSTRUMENTS.map((i) => <option key={i.symbol} value={i.symbol}>{i.symbol}</option>)}</select>
      </div>
      <div className="grid gap-1">
        <label htmlFor="t-qty" className={label}>Quantity</label>
        <input id="t-qty" type="number" inputMode="numeric" min={1} step={1} aria-invalid={errors.quantity ? true : undefined} aria-describedby={errors.quantity ? "t-qty-e" : undefined} className={fieldClass} {...register("quantity")} />
        {errors.quantity && <p id="t-qty-e" role="alert" className={err}>{errors.quantity.message}</p>}
      </div>
      <div className="grid gap-1">
        <label htmlFor="t-price" className={label}>{type === "Stop" ? "Stop price" : "Limit price"}</label>
        <input id="t-price" type="number" inputMode="decimal" step="any" disabled={type === "Market"} placeholder={type === "Market" ? "Market price" : ""} aria-invalid={errors.limitPrice ? true : undefined} aria-describedby={errors.limitPrice ? "t-price-e" : undefined} className={`${fieldClass} disabled:opacity-60`} {...register("limitPrice")} />
        {errors.limitPrice && <p id="t-price-e" role="alert" className={err}>{errors.limitPrice.message}</p>}
      </div>
      <div className="flex items-end">
        <button type="submit" className="min-h-11 w-full rounded-xl bg-term-teal px-4 text-sm font-semibold text-term-bg hover:brightness-110">Add order</button>
      </div>
      {placed && <p role="status" className="text-sm text-term-teal sm:col-span-2 lg:col-span-6">{placed}</p>}
    </form>
  );
}

export function OrdersPage() {
  const orders = useTerminalStore((s) => s.orders);
  const cancel = useTerminalStore((s) => s.cancelOrder);
  const [status, setStatus] = useState<"All" | OrderStatus>("All");
  const [search, setSearch] = useState("");
  const rows = useMemo(() => orders.filter((o) => (status === "All" || o.status === status) && (!search || o.symbol.toLowerCase().includes(search.toLowerCase()))), [orders, status, search]);
  const filled = orders.filter((o) => o.status === "Filled").length;
  const closedOut = orders.filter((o) => o.status !== "Open" && o.status !== "Partially filled").length;

  const columns: Column<Order>[] = [
    { id: "id", header: "Order", cell: (o) => o.id, sort: (o) => o.id },
    { id: "time", header: "Time", cell: (o) => o.time.replace("T", " ").slice(0, 16), sort: (o) => o.time },
    { id: "symbol", header: "Symbol", cell: (o) => <strong>{o.symbol}</strong>, sort: (o) => o.symbol },
    { id: "side", header: "Side", cell: (o) => o.side, sort: (o) => o.side },
    { id: "type", header: "Type", cell: (o) => o.type, sort: (o) => o.type },
    { id: "qty", header: "Quantity", cell: (o) => String(o.quantity), sort: (o) => o.quantity, numeric: true },
    { id: "limit", header: "Limit / stop", cell: (o) => (o.limitPrice === null ? "Market" : usd.format(o.limitPrice)), sort: (o) => o.limitPrice ?? -1, numeric: true },
    { id: "filled", header: "Filled", cell: (o) => `${o.filledQuantity} / ${o.quantity}`, sort: (o) => o.filledQuantity, numeric: true },
    { id: "avg", header: "Average fill", cell: (o) => (o.avgFill === null ? "-" : usd.format(o.avgFill)), sort: (o) => o.avgFill ?? -1, numeric: true },
    { id: "status", header: "Status", cell: (o) => <span title={o.note || undefined}>{o.status}{o.note ? " ⓘ" : ""}</span>, sort: (o) => o.status },
    {
      id: "action", header: "Action",
      cell: (o) => (o.status === "Open" ? <button type="button" onClick={() => cancel(o.id)} aria-label={`Cancel order ${o.id}`} className="min-h-11 rounded-xl border border-term-field px-3 text-sm font-semibold hover:bg-term-panel-strong">Cancel</button> : <span className="text-term-muted">-</span>),
    },
  ];

  return (
    <PageFrame title="Orders" description="Order ticket and the order book. Orders added here stay in this page only.">
      <dl aria-label="Order figures (sample data)" className="grid grid-cols-2 gap-3 xl:grid-cols-4">
        <Stat label="Open orders" value={String(orders.filter((o) => o.status === "Open").length)} />
        <Stat label="Partially filled" value={String(orders.filter((o) => o.status === "Partially filled").length)} />
        <Stat label="Filled" value={String(filled)} />
        <Stat label="Fill rate" value={`${closedOut ? ((filled / closedOut) * 100).toFixed(1) : "0.0"}%`} hint="filled of all finished orders" />
      </dl>
      <Ticket />
      <section aria-labelledby="book-title" className={`${panel} min-w-0`}>
        <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
          <h2 id="book-title" className="font-display text-lg font-bold">Order book</h2>
          <div className="flex flex-wrap items-center gap-2">
            <div role="group" aria-label="Status" className="flex flex-wrap rounded-xl border border-term-field p-0.5">
              {(["All", ...ORDER_STATUSES] as const).map((s) => (
                <button key={s} type="button" aria-pressed={status === s} onClick={() => setStatus(s)} className={`min-h-11 rounded-[10px] px-3 text-sm font-semibold ${status === s ? "bg-term-teal text-term-bg" : "text-term-text hover:bg-term-panel-strong"}`}>{s}</button>
              ))}
            </div>
            <div className="relative">
              <label htmlFor="o-search" className="sr-only">Search orders by symbol</label>
              <Search aria-hidden="true" size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-term-muted" />
              <input id="o-search" type="search" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Symbol" autoComplete="off" className={`${fieldClass} w-36 pl-9`} />
            </div>
          </div>
        </div>
        <p role="status" aria-live="polite" className="mb-2 text-sm text-term-muted">{rows.length} of {orders.length} orders</p>
        <DataTable rows={rows} columns={columns} rowKey={(o) => o.id} label="Order book table" pageSize={10} initialSort={{ id: "time", dir: "desc" }} empty="No orders match." />
      </section>
    </PageFrame>
  );
}
