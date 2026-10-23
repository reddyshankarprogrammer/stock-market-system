import type { Metadata } from "next";
import type { ReactNode } from "react";
import { Brand } from "@/components/auth/Brand";
import { LogoutButton } from "@/components/auth/LogoutButton";
import { Providers } from "@/components/Providers";
import { BottomNav, SidebarNav } from "@/components/shell/AppNav";
import { PreferencesSync } from "@/components/shell/PreferencesSync";
import { ShellThemeToggle } from "@/components/shell/ShellThemeToggle";
import { THEME_KEY } from "@/lib/auth/constants";

export const metadata: Metadata = { robots: { index: false, follow: false } };

const themeScript = `try{var t=localStorage.getItem(${JSON.stringify(THEME_KEY)});if(t==="dark"||t==="light")document.documentElement.dataset.theme=t}catch(e){}`;

// The signed-in app shell: sidebar from 1024 px up, top bar plus bottom navigation below.
// Protected by proxy.ts: visitors without a session cookie are redirected to /login before this renders.
export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <Providers>
      <PreferencesSync />
      <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      <div className="theme-app min-h-dvh lg:grid lg:grid-cols-[15rem_minmax(0,1fr)]">
        <aside className="hidden border-r border-line lg:block">
          <div className="sticky top-0 flex h-dvh flex-col gap-6 overflow-y-auto p-4">
            <Brand />
            <SidebarNav />
            <div className="mt-auto flex flex-wrap items-center gap-3">
              <ShellThemeToggle />
              <LogoutButton />
            </div>
          </div>
        </aside>
        <div className="min-w-0">
          <header className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2 border-b border-line p-4 sm:px-6 lg:hidden">
            <Brand />
            <div className="flex flex-wrap items-center gap-3">
              <ShellThemeToggle />
              <LogoutButton />
            </div>
          </header>
          <main className="mx-auto w-full max-w-5xl px-4 py-8 pb-24 sm:px-6 lg:py-10 lg:pb-10">{children}</main>
        </div>
      </div>
      <BottomNav />
    </Providers>
  );
}
