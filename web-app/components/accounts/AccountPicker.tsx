"use client";

import { useRouter, usePathname, useSearchParams } from "next/navigation";
import { useAccounts } from "@/lib/api/account-hooks";
import { brokerLabel } from "@/lib/format";

/** URL state for a screen: `?account=<id>&from=...`. Reload-safe and shareable; changing it does not scroll or add history. */
export function useUrlState() {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();
  const get = (key: string) => params.get(key) ?? "";
  const set = (patch: Record<string, string | undefined>) => {
    const next = new URLSearchParams(params.toString());
    for (const [k, v] of Object.entries(patch)) {
      if (v) next.set(k, v);
      else next.delete(k);
    }
    const q = next.toString();
    router.replace(q ? `${pathname}?${q}` : pathname, { scroll: false });
  };
  return { get, set };
}

/** The account a screen shows: the `account` in the URL when it is one of the user's, otherwise the first account. */
export function useSelectedAccount() {
  const accounts = useAccounts();
  const url = useUrlState();
  const wanted = url.get("account");
  const list = accounts.data ?? [];
  const selected = list.find((a) => a.id === wanted) ?? list[0];
  return { accounts, list, selected, select: (id: string) => url.set({ account: id, page: undefined }), url };
}

export function AccountPicker({ id = "account-picker" }: { id?: string }) {
  const { list, selected, select } = useSelectedAccount();
  if (list.length < 2) return null; // one account needs no choice
  return (
    <div className="grid gap-1.5">
      <label htmlFor={id} className="text-sm font-semibold">
        Account
      </label>
      <select id={id} value={selected?.id ?? ""} onChange={(e) => select(e.target.value)} className="block min-h-11 w-full rounded-brand border border-line bg-panel px-3.5 py-2.5 text-base text-ink sm:w-auto">
        {list.map((a) => (
          <option key={a.id} value={a.id}>
            {a.accountName} ({brokerLabel[a.brokerName ?? ""] ?? a.brokerName}, {a.currency})
          </option>
        ))}
      </select>
    </div>
  );
}
