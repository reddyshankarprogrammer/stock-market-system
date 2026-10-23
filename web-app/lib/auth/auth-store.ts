import { create } from "zustand";
import type { Profile } from "@/lib/auth/auth-service";

// Client state for "who is signed in". It holds only the profile, never a token: the JWT stays in httpOnly cookies
// (see lib/auth/server.ts), so a script injected into the page cannot read it. Nothing here is persisted to storage.
type Status = "unknown" | "authenticated" | "anonymous";

type AuthState = {
  user: Profile | null;
  status: Status;
  /** Messages for the page after sign-up, such as profile fields that are not saved yet. */
  notice: string[] | null;
  setUser: (user: Profile) => void;
  setAnonymous: () => void;
  setNotice: (notice: string[] | null) => void;
  clear: () => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  status: "unknown",
  notice: null,
  setUser: (user) => set({ user, status: "authenticated" }),
  setAnonymous: () => set({ user: null, status: "anonymous" }),
  setNotice: (notice) => set({ notice }),
  clear: () => set({ user: null, status: "anonymous", notice: null }),
}));
