"use client";

import { useSyncExternalStore } from "react";

// A remembered on/off choice (a collapsed sidebar). It lives in localStorage, so it survives a reload and follows every
// component that uses the same key; where storage is blocked it still works until the page is closed.
const listeners = new Map<string, Set<() => void>>();

export function useCollapsed(key: string): [boolean, (next: boolean) => void] {
  const read = () => {
    try {
      return localStorage.getItem(key) === "1";
    } catch {
      return false;
    }
  };
  const subscribe = (cb: () => void) => {
    const set = listeners.get(key) ?? new Set<() => void>();
    set.add(cb);
    listeners.set(key, set);
    window.addEventListener("storage", cb);
    return () => {
      set.delete(cb);
      window.removeEventListener("storage", cb);
    };
  };
  const value = useSyncExternalStore(subscribe, read, () => false);
  const write = (next: boolean) => {
    try {
      localStorage.setItem(key, next ? "1" : "0");
    } catch {
      /* storage blocked */
    }
    listeners.get(key)?.forEach((l) => l());
  };
  return [value, write];
}
