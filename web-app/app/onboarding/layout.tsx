import type { Metadata } from "next";
import type { ReactNode } from "react";
import { Brand } from "@/components/auth/Brand";
import { LogoutButton } from "@/components/auth/LogoutButton";
import { ThemeToggle } from "@/components/auth/ThemeToggle";
import { Providers } from "@/components/Providers";
import { THEME_KEY } from "@/lib/auth/constants";

export const metadata: Metadata = { title: "Set up your account", robots: { index: false, follow: false } };

const themeScript = `try{var t=localStorage.getItem(${JSON.stringify(THEME_KEY)});if(t==="dark"||t==="light")document.documentElement.dataset.theme=t}catch(e){}`;

// Protected by proxy.ts: visitors without a session cookie are redirected to /login before this renders.
export default function OnboardingLayout({ children }: { children: ReactNode }) {
  return (
    <Providers>
      <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      <div className="theme-app min-h-dvh">
        <header className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2 border-b border-line p-4 sm:px-6">
          <Brand />
          <div className="flex flex-wrap items-center gap-3">
            <ThemeToggle />
            <LogoutButton />
          </div>
        </header>
        <main className="mx-auto w-full max-w-3xl px-4 py-10 sm:px-6">{children}</main>
      </div>
    </Providers>
  );
}
