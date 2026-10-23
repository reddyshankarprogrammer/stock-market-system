"use client";

import type { ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

// Only the app screens (login, register, onboarding, dashboard) use this, so the landing page does not load TanStack Query.
//
// In the browser ONE QueryClient is shared by every layout. Each layout renders <Providers>, and Next keeps visited
// pages alive in memory: with a client per layout, an update made in one layout (for example "onboarding completed")
// would never reach the cached data the dashboard still holds. On the server a fresh client is made per render.
let browserClient: QueryClient | undefined;

function makeClient() {
  return new QueryClient({ defaultOptions: { queries: { refetchOnWindowFocus: false } } });
}

function getClient() {
  if (typeof window === "undefined") return makeClient();
  return (browserClient ??= makeClient());
}

export function Providers({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={getClient()}>{children}</QueryClientProvider>;
}
