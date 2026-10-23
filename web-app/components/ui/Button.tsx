import Link from "next/link";
import type { ComponentProps } from "react";

// One button for the whole site: same radius, height, type and hover motion everywhere.
// "tone" only adapts the colours to a light or navy surface.
type Props = ComponentProps<typeof Link> & {
  variant?: "primary" | "secondary";
  tone?: "light" | "dark";
};

export const buttonBase =
  "inline-flex min-h-11 items-center justify-center rounded-brand px-5 py-2.5 text-sm font-semibold " +
  "transition duration-200 ease-out hover:-translate-y-0.5 focus-visible:-translate-y-0.5 motion-reduce:transform-none";
export const buttonPrimary = "bg-electric text-white shadow-sm hover:shadow-lg hover:brightness-110";
const base = buttonBase;

const styles = {
  primary: buttonPrimary,
  "secondary-light": "border border-electric text-electric hover:bg-electric hover:text-white",
  "secondary-dark": "border border-silver text-white hover:bg-white hover:text-navy",
};

export function Button({ variant = "primary", tone = "light", className = "", ...rest }: Props) {
  const style = variant === "primary" ? styles.primary : styles[`secondary-${tone}`];
  return <Link className={`${base} ${style} ${className}`} {...rest} />;
}
