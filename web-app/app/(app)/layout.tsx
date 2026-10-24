import type { Metadata } from "next";
import { Suspense, type ReactNode } from "react";
import { Brand } from "@/components/auth/Brand";
import { LogoutButton } from "@/components/auth/LogoutButton";
import { Providers } from "@/components/Providers";
import { BottomNav } from "@/components/shell/AppNav";
import { AppSidebar } from "@/components/shell/AppSidebar";
import { ModeSwitch } from "@/components/shell/ModeSwitch";
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
      <div className="theme-app min-h-dvh lg:flex">
        <AppSidebar />
        <div className="min-w-0 flex-1">
          <header className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2 border-b border-line p-4 sm:px-6 lg:hidden">
            <Brand />
            <ModeSwitch mode="app" />
            <div className="flex flex-wrap items-center gap-3">
              <ShellThemeToggle />
              <LogoutButton />
            </div>
          </header>
          <main className="mx-auto w-full max-w-5xl px-4 py-8 pb-24 sm:px-6 lg:py-10 lg:pb-10">{children}</main>
        </div>
      </div>
      <Suspense fallback={null}>
        <BottomNav />
      </Suspense>
    </Providers>
  );
}
