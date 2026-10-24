import Link from "next/link";
import { BarChart3, FlaskConical, ScanSearch, ShoppingCart, Sparkles, TriangleAlert } from "lucide-react";
import type { LucideIcon } from "lucide-react";

const soon = "flex min-h-11 w-full cursor-not-allowed items-center gap-3 rounded-xl border border-term-line px-3 text-left text-sm text-term-muted";

function Action({ icon: Icon, label, href }: { icon: LucideIcon; label: string; href?: string }) {
  return href ? (
    <Link href={href} className="flex min-h-11 items-center gap-3 rounded-xl bg-term-teal px-3 text-sm font-semibold text-term-bg hover:brightness-110">
      <Icon aria-hidden="true" size={18} />
      {label}
    </Link>
  ) : (
    <button type="button" aria-disabled="true" onClick={(e) => e.preventDefault()} className={soon}>
      <Icon aria-hidden="true" size={18} />
      <span className="flex flex-1 items-center justify-between gap-2">
        {label}
        <span className="rounded-md border border-term-line px-1.5 text-[0.65rem] uppercase tracking-wide">Soon</span>
      </span>
    </button>
  );
}

// The words below are fixed sample text, not output of a model, and not advice. The panel says so.
const insights = {
  summary: "Technology and healthcare lead the sample book this month; energy lags. Volatility in the sample is moderate.",
  opportunities: ["Semiconductors: momentum strategy trades are ahead of the mean reversion trades.", "Indian large caps: three open positions are near their entry price."],
  warnings: ["Technology is the largest share of open positions.", "Several open trades are rated high risk."],
  suggestions: ["Review the oldest open trades.", "Compare the risk rating with the position size."],
};

export function RightPanel() {
  return (
    <aside aria-label="Quick actions and AI assistant" className="grid min-w-0 grid-cols-1 content-start gap-4">
      <section aria-labelledby="qa-title" className="rounded-2xl border border-term-line bg-term-panel p-4 backdrop-blur">
        <h2 id="qa-title" className="font-display text-base font-bold">
          Quick actions
        </h2>
        <div className="mt-3 grid gap-2">
          <Action icon={ShoppingCart} label="Place order" href="/trades" />
          <Action icon={ScanSearch} label="Run screener" />
          <Action icon={FlaskConical} label="Start backtest" />
          <Action icon={Sparkles} label="Generate AI signal" />
        </div>
      </section>

      <section aria-labelledby="ai-title" className="rounded-2xl border border-term-line bg-term-panel p-4 backdrop-blur">
        <div className="flex items-start justify-between gap-2">
          <h2 id="ai-title" className="flex items-center gap-2 font-display text-base font-bold">
            <Sparkles aria-hidden="true" size={18} className="text-term-teal" />
            AI assistant
          </h2>
          <span className="rounded-md border border-term-line px-1.5 text-[0.65rem] uppercase tracking-wide text-term-muted">Sample</span>
        </div>
        <p className="mt-1 text-xs text-term-muted">Example text for the design. Not generated from your data and not investment advice.</p>
        <div className="mt-3 grid gap-4 text-sm">
          <div>
            <h3 className="mb-1 flex items-center gap-2 font-semibold">
              <BarChart3 aria-hidden="true" size={16} /> Market summary
            </h3>
            <p className="text-term-muted">{insights.summary}</p>
          </div>
          <div>
            <h3 className="mb-1 font-semibold">Top opportunities</h3>
            <ul className="list-disc pl-5 text-term-muted">{insights.opportunities.map((t) => <li key={t}>{t}</li>)}</ul>
          </div>
          <div>
            <h3 className="mb-1 flex items-center gap-2 font-semibold text-term-loss">
              <TriangleAlert aria-hidden="true" size={16} /> Risk warnings
            </h3>
            <ul className="list-disc pl-5 text-term-muted">{insights.warnings.map((t) => <li key={t}>{t}</li>)}</ul>
          </div>
          <div>
            <h3 className="mb-1 font-semibold">Portfolio suggestions</h3>
            <ul className="list-disc pl-5 text-term-muted">{insights.suggestions.map((t) => <li key={t}>{t}</li>)}</ul>
          </div>
        </div>
      </section>
    </aside>
  );
}
