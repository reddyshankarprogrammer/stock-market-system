import type { ComponentPropsWithoutRef } from "react";

type Props = ComponentPropsWithoutRef<"div"> & { tone?: "light" | "dark" };

export function Card({ tone = "light", className = "", ...rest }: Props) {
  const style =
    tone === "dark" ? "border-white/15 bg-graphite text-white" : "border-silver bg-white text-navy";
  return <div className={`min-w-0 rounded-brand border p-5 ${style} ${className}`} {...rest} />;
}
