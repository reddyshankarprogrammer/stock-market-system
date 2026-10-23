"use client";

import { useRef, useState } from "react";
import { buttonBase, buttonPrimary } from "@/components/ui/Button";
import { ErrorText } from "@/components/ui/form";
import { termsMeta, termsSections } from "@/lib/auth/legal-content";

// Mandatory-to-read terms. The visitor cannot accept until the text has been scrolled to the end, and acceptance is
// only recorded through onAccept. The text is a draft (lib/auth/legal-content.ts) and needs legal review before launch.
export function TermsGate({ accepted, onAccept, error }: { accepted: boolean; onAccept: () => void; error?: string }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const scroller = useRef<HTMLDivElement>(null);
  const [atEnd, setAtEnd] = useState(false);

  function check() {
    const el = scroller.current;
    if (el) setAtEnd(el.scrollTop + el.clientHeight >= el.scrollHeight - 8);
  }
  function open() {
    dialog.current?.showModal();
    requestAnimationFrame(check); // short texts (or tall screens) may already show the whole text
  }
  function accept() {
    onAccept();
    dialog.current?.close();
  }

  return (
    <div>
      <div className={`rounded-brand p-4 text-sm ${error ? "border-2 border-ink" : "border border-line"} bg-soft`}>
        {accepted ? (
          <p className="flex flex-wrap items-center gap-x-3 gap-y-1">
            <span aria-hidden="true" className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-growth text-xs font-bold text-navy">
              ✓
            </span>
            <span className="font-semibold">You read and accepted the Terms and Risk Disclaimer</span>
            <span className="text-subtle">(version {termsMeta.version})</span>
          </p>
        ) : (
          <p>
            Quantiva is for <strong>education and information only</strong> and is not investment advice. Trading can lose you money. You must read the
            Terms and Risk Disclaimer to the end before you can create an account.
          </p>
        )}
        <button
          type="button"
          onClick={open}
          aria-haspopup="dialog"
          className="mt-3 flex min-h-11 items-center rounded-brand border border-link px-4 text-sm font-semibold text-link hover:bg-link hover:text-canvas"
        >
          {accepted ? "Read again" : "Read Terms & Risk Disclaimer"}
        </button>
      </div>
      {error && <ErrorText id="terms-error">{error}</ErrorText>}

      <dialog
        ref={dialog}
        aria-labelledby="terms-title"
        className="m-auto max-h-[90dvh] w-[min(48rem,calc(100vw-2rem))] rounded-brand border border-line bg-panel p-0 text-ink backdrop:bg-navy/80"
      >
        <div className="flex max-h-[90dvh] flex-col">
          <header className="border-b border-line p-5">
            <h2 id="terms-title" className="text-xl font-bold">
              {termsMeta.title}
            </h2>
            <p className="mt-1 text-xs text-subtle">
              Version {termsMeta.version} · {termsMeta.date} ·{" "}
              <span className="font-semibold text-ink">{termsMeta.draftNotice}</span>
            </p>
          </header>

          <div
            ref={scroller}
            onScroll={check}
            tabIndex={0}
            role="region"
            aria-label="Terms and risk disclaimer text. Scroll to the end to enable the accept button."
            className="min-h-0 flex-1 overflow-y-auto p-5"
          >
            {termsSections.map((s) => (
              <section key={s.id} className="mb-6">
                <h3 className="mb-2 font-semibold">{s.title}</h3>
                {s.body.map((p, i) => (
                  <p key={i} className="mb-2 text-sm leading-relaxed text-subtle">
                    {p}
                  </p>
                ))}
              </section>
            ))}
            <p className="text-sm font-semibold">End of document.</p>
          </div>

          <footer className="flex flex-col gap-3 border-t border-line p-5 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-xs text-subtle" aria-live="polite">
              {atEnd ? "You have reached the end. You can accept now." : "Scroll to the end of the document to enable Accept."}
            </p>
            <div className="flex gap-3">
              <button type="button" onClick={() => dialog.current?.close()} className={`${buttonBase} border border-line text-ink hover:bg-soft`}>
                Close
              </button>
              <button
                type="button"
                onClick={accept}
                disabled={!atEnd}
                className={`${buttonBase} ${buttonPrimary} disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0`}
              >
                I have read and accept
              </button>
            </div>
          </footer>
        </div>
      </dialog>
    </div>
  );
}
