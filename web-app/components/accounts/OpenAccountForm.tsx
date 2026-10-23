"use client";

import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { FormAlert, SelectField, SubmitButton, TextField } from "@/components/ui/form";
import { ACCOUNT_TYPES, BROKERS, CURRENCIES, openAccountSchema, type OpenAccountValues } from "@/lib/accounts";
import { useCreateAccount } from "@/lib/api/account-hooks";
import { accountTypeLabel, brokerLabel, isTracking } from "@/lib/format";

export function OpenAccountForm({ onDone }: { onDone: (id: string | undefined) => void }) {
  const create = useCreateAccount();
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<OpenAccountValues>({ resolver: zodResolver(openAccountSchema), defaultValues: { accountName: "", accountType: "INDIVIDUAL", brokerName: "PAPER", currency: "USD", remarks: "" } });
  const broker = useWatch({ control, name: "brokerName" });

  async function onSubmit(v: OpenAccountValues) {
    setError(null);
    try {
      const account = await create.mutateAsync({ ...v, remarks: v.remarks || undefined });
      onDone(account.id);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Something went wrong. Please try again.");
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="grid gap-5 rounded-brand border border-line bg-panel p-5" aria-label="Open an account">
      <h2 className="font-display text-xl font-bold">Open an account</h2>
      {error && (
        <FormAlert tone="error" title="We could not open the account">
          {error}
        </FormAlert>
      )}
      <TextField id="accountName" label="Account name" autoComplete="off" error={errors.accountName?.message} {...register("accountName")} />
      <div className="grid gap-5 sm:grid-cols-2">
        <SelectField id="brokerName" label="Held at" error={errors.brokerName?.message} {...register("brokerName")}>
          {BROKERS.map((b) => (
            <option key={b} value={b}>
              {brokerLabel[b]}
            </option>
          ))}
        </SelectField>
        <SelectField id="accountType" label="Account type" error={errors.accountType?.message} {...register("accountType")}>
          {ACCOUNT_TYPES.map((t) => (
            <option key={t} value={t}>
              {accountTypeLabel[t]}
            </option>
          ))}
        </SelectField>
        <SelectField id="currency" label="Currency" error={errors.currency?.message} {...register("currency")}>
          {CURRENCIES.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </SelectField>
      </div>
      <p className="text-sm text-subtle">{isTracking(broker) ? "This account only tracks trades from your broker: no cash moves here. Import a statement or record trades by hand." : "A Quantiva account holds cash and trades at market prices."}</p>
      <TextField id="remarks" label="Notes (optional)" autoComplete="off" error={errors.remarks?.message} {...register("remarks")} />
      <SubmitButton loading={create.isPending} loadingText="Opening…">
        Open account
      </SubmitButton>
    </form>
  );
}
