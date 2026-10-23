import { z } from "zod";
import { countryCodes } from "@/lib/auth/countries";

// The same schemas run in the browser (React Hook Form) and in the route handlers, so the rules can't drift.
// Server rules to respect (user-service): email format, password at least 6 characters, names not blank.
// BCrypt only uses the first 72 bytes of a password, so 72 is the maximum we accept.

const name = (label: string) =>
  z
    .string()
    .trim()
    .min(1, `${label} is required`)
    .max(50, `${label} is too long`)
    .regex(/^[\p{L}][\p{L}\p{M}' .-]*$/u, `${label} can contain letters, spaces, apostrophes, dots and hyphens only`);

const email = z.string().trim().toLowerCase().min(1, "Email is required").max(254, "Email is too long").email("Enter a valid email address");

export const loginSchema = z.object({
  email,
  password: z.string().min(1, "Password is required").max(72, "Password is too long"),
  remember: z.boolean().default(false),
});
export type LoginInput = z.input<typeof loginSchema>;
export type LoginValues = z.output<typeof loginSchema>;

export const passwordRule = z
  .string()
  .min(10, "Use at least 10 characters")
  .max(72, "Use at most 72 characters")
  .refine((v) => /[A-Za-z]/.test(v), "Include at least one letter")
  .refine((v) => /\d/.test(v), "Include at least one number");

// National number only; the dial code is chosen separately.
const mobile = z
  .string()
  .trim()
  .min(1, "Mobile number is required")
  .transform((v) => v.replace(/[\s()-]/g, ""))
  .refine((v) => /^\d+$/.test(v), "Use digits only");

export const registerSchema = z
  .object({
    firstName: name("First name"),
    lastName: name("Last name"),
    email,
    password: passwordRule,
    confirmPassword: z.string().min(1, "Confirm your password"),
    country: z.enum(countryCodes, { errorMap: () => ({ message: "Select your country" }) }),
    dialCode: z.string().regex(/^\+\d{1,4}$/, "Select a dial code"),
    mobile,
    gender: z.enum(["male", "female"], { errorMap: () => ({ message: "Select a gender" }) }),
    // Set only by the Terms dialog after the visitor scrolls to the end and accepts (components/auth/TermsGate.tsx).
    termsAccepted: z.boolean().refine((v) => v === true, "Read the Terms and Risk Disclaimer to the end and accept them to continue"),
  })
  .superRefine((v, ctx) => {
    if (v.password !== v.confirmPassword) ctx.addIssue({ code: "custom", path: ["confirmPassword"], message: "Passwords do not match" });
    const digits = v.mobile;
    const indian = v.dialCode === "+91";
    if (indian ? !/^[6-9]\d{9}$/.test(digits) : digits.length < 6 || digits.length > 14) {
      ctx.addIssue({
        code: "custom",
        path: ["mobile"],
        message: indian ? "Enter a 10-digit Indian mobile number starting with 6, 7, 8 or 9" : "Enter a mobile number of 6 to 14 digits",
      });
    }
  });
export type RegisterValues = z.input<typeof registerSchema>;
export type RegisterParsed = z.output<typeof registerSchema>;

// 0-4 score for the strength meter. Display only; the rules above are what the form enforces.
export function passwordStrength(p: string): { score: 0 | 1 | 2 | 3 | 4; label: string } {
  let s = 0;
  if (p.length >= 10) s++;
  if (p.length >= 14) s++;
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) s++;
  if (/\d/.test(p) && /[^A-Za-z0-9]/.test(p)) s++;
  const score = (p.length === 0 ? 0 : Math.max(1, s)) as 0 | 1 | 2 | 3 | 4;
  return { score, label: ["", "Weak", "Fair", "Good", "Strong"][score] };
}
