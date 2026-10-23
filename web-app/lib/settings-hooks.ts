"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { authService } from "@/lib/auth/auth-service";
import { useMe } from "@/lib/auth/auth-hooks";
import { AuthError } from "@/lib/auth/errors";
import type { Preferences, PreferencesInput } from "@/lib/settings";

export const preferencesKey = ["preferences"] as const;

/** The account's settings. A 401 is final (the session is over), anything else is retried once. */
export function usePreferences() {
  // Wait for the profile: when the access cookie has expired, the profile call renews the session first, and a second
  // call in parallel would renew it again.
  const signedIn = useMe().isSuccess;
  return useQuery({
    enabled: signedIn,
    queryKey: preferencesKey,
    queryFn: authService.getPreferences,
    retry: (count, error) => !(error instanceof AuthError && error.code === "UNAUTHENTICATED") && count < 1,
    staleTime: 5 * 60_000,
  });
}

/**
 * Save one setting or several. The screen changes at once (optimistic) and goes back to the saved value, with the
 * error shown by the caller, if the save fails.
 */
export function useSavePreferences() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: authService.savePreferences,
    onMutate: async (change: PreferencesInput) => {
      await qc.cancelQueries({ queryKey: preferencesKey });
      const before = qc.getQueryData<Preferences>(preferencesKey);
      if (before) qc.setQueryData<Preferences>(preferencesKey, { ...before, ...change });
      return { before };
    },
    onError: (_e, _change, ctx) => {
      if (ctx?.before) qc.setQueryData(preferencesKey, ctx.before);
    },
    onSuccess: (saved) => qc.setQueryData(preferencesKey, saved),
  });
}

export function useChangePassword() {
  return useMutation({ mutationFn: authService.changePassword });
}
