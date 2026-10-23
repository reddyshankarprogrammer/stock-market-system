"use client";

import { useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { authService } from "@/lib/auth/auth-service";
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
      qc.setQueryData(authKeys.me, user);
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
      qc.setQueryData(authKeys.me, user);
    },
  });
}

export function useForgotPassword() {
  return useMutation({ mutationFn: authService.forgotPassword });
}

export function useResetPassword() {
  return useMutation({ mutationFn: authService.resetPassword });
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
