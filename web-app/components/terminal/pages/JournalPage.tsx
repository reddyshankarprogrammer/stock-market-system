"use client";

import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { CategoryBars } from "@/components/terminal/Charts";
import { PageFrame, Stat, fieldClass, panel, signedUsd, tone } from "@/components/terminal/PageFrame";
import { journalSchema, type JournalValues } from "@/lib/terminal/forms";
import { AS_OF, INSTRUMENTS, STRATEGIES } from "@/lib/terminal/sample-data";
import { EMOTIONS, journalByEmotion, type Emotion } from "@/lib/terminal/sample-extra";
import { useTerminalStore } from "@/lib/terminal/store";

const label = "text-xs font-semibold text-term-muted";
const err = "text-xs font-semibold text-term-loss";
const PAGE = 8;

function EntryForm() {
  const add = useTerminalStore((s) => s.addJournal);
  const [saved, setSaved] = useState(false);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<JournalValues>({ resolver: zodResolver(journalSchema), defaultValues: { symbol: "AAPL", strategy: "Momentum", emotion: "Calm", rating: 3, notes: "", lesson: "" } });
  function onSubmit(raw: JournalValues) {
    const v = journalSchema.parse(raw);
    add({ date: AS_OF, symbol: v.symbol, strategy: v.strategy, emotion: v.emotion, rating: v.rating, notes: v.notes, lesson: v.lesson ?? "" });
    setSaved(true);
    reset({ ...raw, notes: "", lesson: "" });
  }
  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate aria-label="New journal entry" className={`${panel} grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4`}>
      <h2 className="font-display text-base font-bold sm:col-span-2 lg:col-span-4">New entry (sample: kept until you reload the page)</h2>
      <div className="grid gap-1"><label htmlFor="j-symbol" className={label}>Instrument</label><select id="j-symbol" className={fieldClass} {...register("symbol")}>{INSTRUMENTS.map((i) => <option key={i.symbol}>{i.symbol}</option>)}</select></div>
      <div className="grid gap-1"><label htmlFor="j-strategy" className={label}>Strategy</label><select id="j-strategy" className={fieldClass} {...register("strategy")}>{STRATEGIES.map((s) => <option key={s}>{s}</option>)}</select></div>
      <div className="grid gap-1"><label htmlFor="j-emotion" className={label}>How did you feel?</label><select id="j-emotion" className={fieldClass} {...register("emotion")}>{EMOTIONS.map((e) => <option key={e}>{e}</option>)}</select></div>
      <div className="grid gap-1">
        <label htmlFor="j-rating" className={label}>Rating (1 poor, 5 excellent)</label>
        <select id="j-rating" className={fieldClass} {...register("rating")}>{[1, 2, 3, 4, 5].map((n) => <option key={n} value={n}>{n}</option>)}</select>
      </div>
      <div className="grid gap-1 sm:col-span-2">
        <label htmlFor="j-notes" className={label}>What happened</label>
        <textarea id="j-notes" rows={3} aria-invalid={errors.notes ? true : undefined} aria-describedby={errors.notes ? "j-notes-e" : undefined} className={`${fieldClass} py-2`} {...register("notes")} />
        {errors.notes && <p id="j-notes-e" role="alert" className={err}>{errors.notes.message}</p>}
      </div>
      <div className="grid gap-1 sm:col-span-2">
        <label htmlFor="j-lesson" className={label}>Lesson (optional)</label>
        <textarea id="j-lesson" rows={3} aria-invalid={errors.lesson ? true : undefined} aria-describedby={errors.lesson ? "j-lesson-e" : undefined} className={`${fieldClass} py-2`} {...register("lesson")} />
        {errors.lesson && <p id="j-lesson-e" role="alert" className={err}>{errors.lesson.message}</p>}
      </div>
      <div className="sm:col-span-2 lg:col-span-4">
        <button type="submit" className="min-h-11 rounded-xl bg-term-teal px-5 text-sm font-semibold text-term-bg hover:brightness-110">Save entry</button>
        {saved && <span role="status" className="ml-3 text-sm text-term-teal">Entry saved to this page (sample).</span>}
      </div>
    </form>
  );
}

export function JournalPage() {
  const entries = useTerminalStore((s) => s.journal);
  const [emotion, setEmotion] = useState<"" | Emotion>("");
  const [search, setSearch] = useState("");
  const [shown, setShown] = useState(PAGE);
  const rows = useMemo(() => entries.filter((e) => (!emotion || e.emotion === emotion) && (!search || `${e.symbol} ${e.notes} ${e.lesson} ${e.strategy}`.toLowerCase().includes(search.toLowerCase()))), [entries, emotion, search]);
  const byEmotion = useMemo(() => journalByEmotion(entries), [entries]);
  const withResult = entries.filter((e) => e.pnl !== null);
  const wins = withResult.filter((e) => (e.pnl as number) > 0).length;
  const avg = entries.length ? entries.reduce((s, e) => s + e.rating, 0) / entries.length : 0;
  const best = [...byEmotion].filter((x) => x.count > 0).sort((a, b) => b.pnl - a.pnl)[0];

  return (
    <PageFrame title="Journal" description="Notes on each trade: what happened, how it felt, what to change.">
      <dl aria-label="Journal figures (sample data)" className="grid grid-cols-2 gap-3 xl:grid-cols-4">
        <Stat label="Entries" value={String(entries.length)} />
        <Stat label="Average rating" value={avg.toFixed(1)} hint="out of 5" />
        <Stat label="Win rate of journaled trades" value={`${withResult.length ? ((wins / withResult.length) * 100).toFixed(1) : "0.0"}%`} hint={`${wins} of ${withResult.length}`} />
        <Stat label="Best state of mind" value={best ? best.emotion : "-"} hint={best ? signedUsd(best.pnl, 0) : undefined} tone={best ? tone(best.pnl) : undefined} />
      </dl>
      <EntryForm />
      <CategoryBars id="emotion" title="Profit and loss by state of mind" note="Journaled trades that have a result" data={byEmotion.map((x) => ({ name: x.emotion, value: x.pnl }))} format={(v) => signedUsd(v, 0)} />
      <section aria-labelledby="entries-title" className={`${panel} min-w-0`}>
        <div className="mb-3 flex flex-wrap items-end justify-between gap-3">
          <h2 id="entries-title" className="font-display text-lg font-bold">Entries</h2>
          <div className="flex flex-wrap gap-3">
            <div className="grid gap-1"><label htmlFor="je-emotion" className={label}>State of mind</label><select id="je-emotion" value={emotion} onChange={(e) => { setEmotion(e.target.value as "" | Emotion); setShown(PAGE); }} className={fieldClass}><option value="">All</option>{EMOTIONS.map((e) => <option key={e}>{e}</option>)}</select></div>
            <div className="grid gap-1"><label htmlFor="je-search" className={label}>Search</label><input id="je-search" type="search" value={search} onChange={(e) => { setSearch(e.target.value); setShown(PAGE); }} placeholder="Words, symbol" autoComplete="off" className={fieldClass} /></div>
          </div>
        </div>
        <p role="status" aria-live="polite" className="mb-2 text-sm text-term-muted">{rows.length} of {entries.length} entries</p>
        {rows.length === 0 ? (
          <p className="rounded-xl border border-term-line p-4 text-term-muted">No entries match.</p>
        ) : (
          <ul className="grid grid-cols-1 gap-3">
            {rows.slice(0, shown).map((e) => (
              <li key={e.id} data-entry={e.id} className="rounded-xl border border-term-line p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <h3 className="font-semibold">{e.symbol} · {e.strategy} <span className="font-normal text-term-muted">· {e.date}</span></h3>
                  <p className="text-sm">
                    <span className="rounded-md border border-term-line px-1.5 py-0.5 text-xs">{e.emotion}</span>{" "}
                    <span aria-label={`Rating ${e.rating} out of 5`}>{"★".repeat(e.rating)}{"☆".repeat(5 - e.rating)}</span>{" "}
                    {e.pnl !== null && <span className={`tabular-nums font-semibold ${e.pnl >= 0 ? "text-term-teal" : "text-term-loss"}`}>{signedUsd(e.pnl)}</span>}
                  </p>
                </div>
                <p className="mt-2 text-sm">{e.notes}</p>
                {e.lesson && <p className="mt-1 text-sm text-term-muted">Lesson: {e.lesson}</p>}
              </li>
            ))}
          </ul>
        )}
        {shown < rows.length && (
          <button type="button" onClick={() => setShown((n) => n + PAGE)} className="mt-3 min-h-11 rounded-xl border border-term-field px-4 text-sm font-semibold hover:bg-term-panel-strong">Show more ({rows.length - shown} left)</button>
        )}
      </section>
    </PageFrame>
  );
}
