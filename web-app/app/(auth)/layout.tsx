import type { ReactNode } from "react";
import Link from "next/link";
import { Brand } from "@/components/auth/Brand";
import { ThemeToggle } from "@/components/auth/ThemeToggle";
import { Providers } from "@/components/Providers";
import { THEME_KEY } from "@/lib/auth/constants";

// Applies a stored light/dark pick before first paint. "System" needs no script (CSS follows the OS).
const themeScript = `try{var t=localStorage.getItem(${JSON.stringify(THEME_KEY)});if(t==="dark"||t==="light")document.documentElement.dataset.theme=t}catch(e){}`;

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <Providers>
      <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      <div className="theme-app min-h-dvh lg:grid lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)]">
        <aside className="hidden flex-col justify-between bg-navy p-12 text-white lg:flex">
          <Brand onDark />
          <div>
            <p className="font-display text-4xl font-extrabold leading-tight tracking-tight">Trade With Intelligence.</p>
            <ul className="mt-8 grid gap-4 text-silver">
              <li className="flex gap-3"><span aria-hidden="true" className="text-growth">✓</span>Your journal, analytics and AI coach in one place</li>
              <li className="flex gap-3"><span aria-hidden="true" className="text-growth">✓</span>Statements from Groww, Zerodha, Binance and Exness (in development)</li>
              <li className="flex gap-3"><span aria-hidden="true" className="text-growth">✓</span>Educational tools. Not investment advice.</li>
            </ul>
          </div>
          <p className="text-xs text-silver">Quantiva is pre-launch. Features are marked in development or planned.</p>
        </aside>

        <div className="flex min-h-dvh min-w-0 flex-col">
          <header className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2 p-4 sm:p-6">
            <span className="lg:hidden">
              <Brand />
            </span>
            <span className="hidden lg:block" />
            <ThemeToggle />
          </header>
          <main className="mx-auto flex w-full max-w-xl flex-1 flex-col justify-center px-4 pb-10 sm:px-6">{children}</main>
          <footer className="px-4 pb-6 text-xs text-subtle sm:px-6">
            <p>Trading involves risk, including the loss of capital. For education and information only; not investment advice.</p>
            <nav aria-label="Legal" className="mt-1 flex flex-wrap gap-x-4">
              <Link href="/legal/terms" className="inline-flex min-h-11 items-center underline underline-offset-4 hover:decoration-2">Terms</Link>
              <Link href="/legal/privacy" className="inline-flex min-h-11 items-center underline underline-offset-4 hover:decoration-2">Privacy</Link>
              <Link href="/legal/risk" className="inline-flex min-h-11 items-center underline underline-offset-4 hover:decoration-2">Risk disclosure</Link>
            </nav>
          </footer>
        </div>
      </div>
    </Providers>
  );
}
