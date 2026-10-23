"use client";

import { useQuery } from "@tanstack/react-query";
import { accountApi } from "@/lib/api/account-api";
import { AuthError } from "@/lib/auth/errors";

export const accountKeys = { list: (userId: string) => ["accounts", userId] as const };

export function useAccounts(userId: string | undefined) {
  return useQuery({
    queryKey: accountKeys.list(userId ?? ""),
    queryFn: () => accountApi.listForUser(userId!),
    enabled: Boolean(userId),
    retry: (count, error) => !(error instanceof AuthError && error.status === 401) && count < 1,
    staleTime: 30_000,
  });
}
