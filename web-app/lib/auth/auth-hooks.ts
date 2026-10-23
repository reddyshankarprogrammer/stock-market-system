"use client";

import { useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { authService, type Profile } from "@/lib/auth/auth-service";
import { useAuthStore } from "@/lib/auth/auth-store";
import { AuthError } from "@/lib/auth/errors";

export const authKeys = { me: ["auth", "me"] as const };

/** The signed-in user. Keeps the store in step with the server: a 401 marks the visitor as anonymous. */
export function useMe() {
  const setUser = useAuthStore((s) => s.setUser);
  const setAnonymous = useAuthStore((s) => s.setAnonymous);
  const query = useQuery({
    queryKey: authKeys.me,
    queryFn: authService.me,
    retry: false, // a 401 is final; do not retry it
    staleTime: 60_000,
  });
  useEffect(() => {
    if (query.data) setUser(query.data);
    else if (query.error instanceof AuthError && query.error.code === "UNAUTHENTICATED") setAnonymous();
  }, [query.data, query.error, setUser, setAnonymous]);
  return query;
}

export function useLogin() {
  const qc = useQueryClient();
  const setUser = useAuthStore((s) => s.setUser);
  return useMutation({
    mutationFn: authService.login,
    onSuccess: (user) => {
      setUser(user);
      // Do NOT seed the profile cache with the login answer: it has no onboarding status, and the dashboard would trust it.
      // Remove any old profile so the next screen fetches the full one.
      qc.removeQueries({ queryKey: authKeys.me });
    },
  });
}

export function useRegister() {
  const qc = useQueryClient();
  const setUser = useAuthStore((s) => s.setUser);
  const setNotice = useAuthStore((s) => s.setNotice);
  return useMutation({
    mutationFn: authService.register,
    onSuccess: ({ user, notSaved }) => {
      setUser(user);
      setNotice(notSaved);
      qc.removeQueries({ queryKey: authKeys.me }); // see useLogin: the dashboard must fetch the full profile
    },
  });
}

export function useForgotPassword() {
  return useMutation({ mutationFn: authService.forgotPassword });
}

export function useResetPassword() {
  return useMutation({ mutationFn: authService.resetPassword });
}

export const onboardingKey = ["onboarding"] as const;

export function useOnboarding() {
  return useQuery({ queryKey: onboardingKey, queryFn: authService.getOnboarding, retry: false, staleTime: Infinity });
}

export function useSaveOnboarding() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: authService.saveOnboarding, onSuccess: (data) => qc.setQueryData(onboardingKey, data) });
}

export function useCompleteOnboarding() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: authService.completeOnboarding,
    onSuccess: (data) => {
      qc.setQueryData(onboardingKey, data);
      // Update the cached profile at once: the dashboard acts on cached data before it refetches, and a stale
      // "not completed" would send the user straight back to the wizard.
      qc.setQueryData(authKeys.me, (old: Profile | undefined) => (old ? { ...old, onboardingCompleted: true } : old));
      qc.invalidateQueries({ queryKey: authKeys.me });
    },
  });
}

export function useLogout() {
  const qc = useQueryClient();
  const router = useRouter();
  const clear = useAuthStore((s) => s.clear);
  return useMutation({
    mutationFn: authService.logout,
    // Whether or not the call succeeds, the visitor leaves this screen signed out.
    onSettled: () => {
      clear();
      qc.clear();
      router.replace("/login");
    },
  });
}
