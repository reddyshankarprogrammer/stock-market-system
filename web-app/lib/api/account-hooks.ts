"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { accountApi, type CreateAccount, type ManualTrade } from "@/lib/api/account-api";
import { useMe } from "@/lib/auth/auth-hooks";
import { AuthError } from "@/lib/auth/errors";

export const accountKeys = {
  list: (userId: string) => ["accounts", userId] as const,
  one: (id: string) => ["accounts", "one", id] as const,
  summary: (id: string) => ["accounts", "summary", id] as const,
  performance: (id: string) => ["accounts", "performance", id] as const,
  trades: (id: string, from?: string, to?: string) => ["trades", id, from ?? "", to ?? ""] as const,
  trade: (id: string) => ["trade", id] as const,
  holdings: (id: string) => ["holdings", id] as const,
  statements: (id: string) => ["statements", id] as const,
};

// A 401 is final (the session is over); anything else is retried once.
const retry = (count: number, error: unknown) => !(error instanceof AuthError && error.status === 401) && count < 1;

/** All accounts of the signed-in user. Waits for the profile, which also renews an expired session once. */
export function useAccounts() {
  const me = useMe();
  const userId = me.data?.userId;
  const query = useQuery({
    queryKey: accountKeys.list(userId ?? ""),
    queryFn: () => accountApi.listForUser(userId!),
    enabled: Boolean(userId),
    retry,
    staleTime: 30_000,
  });
  return { ...query, me };
}

export function useAccount(id: string | undefined) {
  return useQuery({ queryKey: accountKeys.one(id ?? ""), queryFn: () => accountApi.get(id!), enabled: Boolean(id), retry, staleTime: 30_000 });
}

export function useAccountSummary(id: string | undefined) {
  return useQuery({ queryKey: accountKeys.summary(id ?? ""), queryFn: () => accountApi.summary(id!), enabled: Boolean(id), retry, staleTime: 15_000 });
}

export function usePerformance(id: string | undefined) {
  return useQuery({ queryKey: accountKeys.performance(id ?? ""), queryFn: () => accountApi.performance(id!), enabled: Boolean(id), retry, staleTime: 15_000 });
}

export function useTrades(id: string | undefined, from?: string, to?: string) {
  return useQuery({ queryKey: accountKeys.trades(id ?? "", from, to), queryFn: () => accountApi.trades(id!, { from, to }), enabled: Boolean(id), retry, staleTime: 15_000 });
}

export function useTrade(id: string | undefined) {
  return useQuery({ queryKey: accountKeys.trade(id ?? ""), queryFn: () => accountApi.trade(id!), enabled: Boolean(id), retry, staleTime: 60_000 });
}

/** Open positions with prices from market-data. `live` turns on polling so the prices move without a reload. */
export function useHoldings(id: string | undefined, live = false) {
  return useQuery({ queryKey: accountKeys.holdings(id ?? ""), queryFn: () => accountApi.holdings(id!), enabled: Boolean(id), retry, staleTime: 5_000, refetchInterval: live ? 10_000 : false });
}

export function useStatements(id: string | undefined) {
  return useQuery({ queryKey: accountKeys.statements(id ?? ""), queryFn: () => accountApi.statements(id!), enabled: Boolean(id), retry });
}

export function useCreateAccount() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (body: CreateAccount) => accountApi.create(body), onSuccess: () => qc.invalidateQueries({ queryKey: ["accounts"] }) });
}

/** Everything that depends on an account's trades changes when trades or statements do. */
function invalidateAccount(qc: ReturnType<typeof useQueryClient>, accountId: string) {
  for (const key of [["trades", accountId], ["holdings", accountId], ["accounts", "summary", accountId], ["accounts", "performance", accountId], ["statements", accountId]]) {
    qc.invalidateQueries({ queryKey: key });
  }
}

export function useRecordTrade() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (body: ManualTrade) => accountApi.recordTrade(body), onSuccess: (_t, body) => body.accountId && invalidateAccount(qc, body.accountId) });
}

export function useUploadStatement(accountId: string) {
  const qc = useQueryClient();
  // A rejected file (bad format, duplicate) still creates a statement record with its import result, so refresh either way.
  return useMutation({ mutationFn: (file: File) => accountApi.uploadStatement(accountId, file), onSettled: () => invalidateAccount(qc, accountId) });
}

export function useDeleteStatement(accountId: string) {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (statementId: string) => accountApi.deleteStatement(statementId), onSuccess: () => invalidateAccount(qc, accountId) });
}
