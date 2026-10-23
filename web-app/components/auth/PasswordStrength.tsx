import { passwordStrength } from "@/lib/auth/schemas";

// Four segments plus a word, so strength is never shown by colour alone. Display only: the form rules are in lib/auth/schemas.ts.
export function PasswordStrength({ value }: { value: string }) {
  const { score, label } = passwordStrength(value);
  if (!value) return null;
  return (
    <div className="mt-2" aria-live="polite">
      <div className="flex gap-1.5" aria-hidden="true">
        {[1, 2, 3, 4].map((i) => (
          <span key={i} className={`h-1.5 flex-1 rounded-full ${i <= score ? "bg-growth" : "bg-line/50"}`} />
        ))}
      </div>
      <p className="mt-1.5 text-xs text-subtle">
        Password strength: <span className="font-semibold text-ink">{label}</span>
      </p>
    </div>
  );
}
