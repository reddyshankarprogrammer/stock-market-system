"use client";

import { useRef, useState } from "react";
import { FormAlert, Spinner } from "@/components/ui/form";
import { QueryState } from "@/components/ui/QueryState";
import { statementFileProblem } from "@/lib/accounts";
import { useDeleteStatement, useStatements, useUploadStatement } from "@/lib/api/account-hooks";
import type { Statement } from "@/lib/api/account-api";
import { dateOnly, dateTime, money } from "@/lib/format";

const statusText: Record<string, string> = { SUCCESS: "Imported", PARTIAL: "Imported with warnings", FAILED: "Import failed" };

function ImportResult({ s, currency }: { s: Statement; currency: string | undefined }) {
  const i = s.latestImport;
  if (!i) return <p className="text-sm text-subtle">Not imported yet.</p>;
  return (
    <div className="grid gap-1 text-sm">
      <p className="font-semibold">{statusText[i.status ?? ""] ?? i.status}</p>
      <p className="text-subtle">
        {i.tradesImported ?? 0} trades imported, {i.duplicateTrades ?? 0} already known, {i.rowsSkipped ?? 0} rows skipped (of {i.rowsRead ?? 0} read)
        {(i.chargesImported ?? 0) > 0 || (i.taxesImported ?? 0) > 0 ? `; ${i.chargesImported ?? 0} charges and ${i.taxesImported ?? 0} taxes` : ""}.
      </p>
      {i.reportedRealizedPnl !== undefined && i.reportedRealizedPnl !== null && <p className="text-subtle">The statement itself reports realised profit of {money(i.reportedRealizedPnl, currency)}.</p>}
      {i.errorMessage && <p className="font-semibold">{i.errorMessage}</p>}
      {i.warnings && i.warnings.length > 0 && (
        <details>
          <summary className="min-h-11 cursor-pointer py-2 font-semibold text-link">{i.warnings.length} warning{i.warnings.length === 1 ? "" : "s"}</summary>
          <ul className="list-disc pl-5 text-subtle">
            {i.warnings.slice(0, 20).map((w, n) => (
              <li key={n}>{w}</li>
            ))}
            {i.warnings.length > 20 && <li>…and {i.warnings.length - 20} more</li>}
          </ul>
        </details>
      )}
    </div>
  );
}

/** Upload a broker statement and see what was imported. Only for tracking accounts of a broker with a parser. */
export function StatementsPanel({ accountId, brokerLabel, currency }: { accountId: string; brokerLabel: string; currency: string | undefined }) {
  const statements = useStatements(accountId);
  const upload = useUploadStatement(accountId);
  const remove = useDeleteStatement(accountId);
  const input = useRef<HTMLInputElement>(null);
  const [problem, setProblem] = useState<string | null>(null);
  const [confirming, setConfirming] = useState<string | null>(null);

  async function send(file: File | undefined) {
    setProblem(null);
    if (!file) return;
    const bad = statementFileProblem(file);
    if (bad) return setProblem(bad);
    try {
      await upload.mutateAsync(file);
    } catch {
      /* shown from upload.error below */
    }
    if (input.current) input.current.value = "";
  }

  return (
    <section aria-labelledby="statements-heading" className="grid gap-4">
      <h2 id="statements-heading" className="font-display text-xl font-bold">
        Broker statements
      </h2>
      <div className="grid gap-3 rounded-brand border border-line bg-panel p-5">
        <label htmlFor="statement-file" className="font-semibold">
          Upload a {brokerLabel} statement
        </label>
        <p id="statement-hint" className="text-sm text-subtle">
          CSV, XLS or XLSX, up to 20 MB. The trades are imported straight away; a file you have already uploaded is recognised and not imported twice.
        </p>
        <input
          ref={input}
          id="statement-file"
          type="file"
          accept=".csv,.xls,.xlsx"
          aria-describedby="statement-hint"
          disabled={upload.isPending}
          onChange={(e) => send(e.target.files?.[0])}
          className="block min-h-11 w-full text-sm file:mr-4 file:min-h-11 file:rounded-brand file:border-0 file:bg-electric file:px-4 file:font-semibold file:text-white"
        />
        {upload.isPending && (
          <p role="status" className="flex items-center gap-2 text-sm">
            <Spinner /> Importing…
          </p>
        )}
        {problem && (
          <FormAlert tone="error" title="That file cannot be used">
            {problem}
          </FormAlert>
        )}
        {upload.isError && (
          <FormAlert tone="error" title="The statement was not imported">
            {upload.error.message}
          </FormAlert>
        )}
        {upload.isSuccess && upload.data.latestImport?.status !== "FAILED" && (
          <FormAlert tone="success" title="Statement imported">
            {upload.data.originalFileName}: {upload.data.latestImport?.tradesImported ?? 0} trades imported.
          </FormAlert>
        )}
      </div>

      <QueryState query={statements} what="the statements" next={`/accounts/${accountId}`}>
        {!statements.data || statements.data.length === 0 ? (
          <p className="text-subtle">No statements uploaded yet.</p>
        ) : (
          <ul className="grid gap-3">
            {statements.data.map((s) => (
              <li key={s.id} className="rounded-brand border border-line p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="break-all font-semibold">{s.originalFileName}</p>
                    <p className="text-sm text-subtle">
                      Uploaded {dateTime(s.uploadedAt)} · {s.fileFormat}
                      {s.periodFrom && s.periodTo ? ` · ${dateOnly(s.periodFrom)} to ${dateOnly(s.periodTo)}` : ""}
                    </p>
                  </div>
                  {confirming === s.id ? (
                    <div className="flex flex-wrap items-center gap-2" role="group" aria-label={`Delete ${s.originalFileName}?`}>
                      <button type="button" onClick={() => remove.mutate(s.id!, { onSettled: () => setConfirming(null) })} disabled={remove.isPending} className="min-h-11 rounded-brand border border-ink bg-ink px-4 text-sm font-semibold text-canvas">
                        Yes, delete
                      </button>
                      <button type="button" onClick={() => setConfirming(null)} className="min-h-11 rounded-brand border border-line px-4 text-sm font-semibold">
                        Keep
                      </button>
                    </div>
                  ) : (
                    <button type="button" onClick={() => setConfirming(s.id ?? null)} className="min-h-11 rounded-brand border border-line px-4 text-sm font-semibold hover:bg-soft">
                      Delete
                    </button>
                  )}
                </div>
                <div className="mt-3">
                  <ImportResult s={s} currency={currency} />
                </div>
              </li>
            ))}
          </ul>
        )}
        {remove.isError && (
          <FormAlert tone="error" title="The statement was not deleted">
            {remove.error.message}
          </FormAlert>
        )}
      </QueryState>
    </section>
  );
}
