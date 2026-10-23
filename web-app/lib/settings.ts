import { z } from "zod";
import { passwordRule } from "@/lib/auth/schemas";

// The settings a user can change. Same schema in the browser and in the route handlers; user-service validates again.
export const THEMES = [
  { code: "SYSTEM", label: "System", text: "Follow my device" },
  { code: "LIGHT", label: "Light", text: "Always light" },
  { code: "DARK", label: "Dark", text: "Always dark" },
] as const;

// Excel and PDF exports do not exist yet (CSV and JSON come first), so they are listed but cannot be chosen.
export const EXPORT_FORMATS = [
  { code: "CSV", label: "CSV", available: true },
  { code: "JSON", label: "JSON", available: true },
  { code: "EXCEL", label: "Excel", available: false },
  { code: "PDF", label: "PDF", available: false },
] as const;

export type Preferences = {
  theme: "SYSTEM" | "LIGHT" | "DARK";
  language: "en";
  notifyInApp: boolean;
  notifyPush: boolean;
  notifyEmail: boolean;
  exportFormat: "CSV" | "EXCEL" | "PDF" | "JSON";
};

/** Only the fields that change are sent; the others keep their saved value. */
export const preferencesSchema = z.object({
  theme: z.enum(["SYSTEM", "LIGHT", "DARK"]).optional(),
  language: z.literal("en").optional(),
  notifyInApp: z.boolean().optional(),
  notifyPush: z.boolean().optional(),
  notifyEmail: z.boolean().optional(),
  exportFormat: z.enum(["CSV", "JSON"]).optional(), // EXCEL and PDF are not offered yet
});
export type PreferencesInput = z.infer<typeof preferencesSchema>;

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "Enter your current password").max(200),
    newPassword: passwordRule,
    confirmPassword: z.string().min(1, "Confirm your new password"),
  })
  .superRefine((v, ctx) => {
    if (v.newPassword !== v.confirmPassword) ctx.addIssue({ code: "custom", path: ["confirmPassword"], message: "Passwords do not match" });
    if (v.newPassword && v.newPassword === v.currentPassword) ctx.addIssue({ code: "custom", path: ["newPassword"], message: "Choose a password that is different from the current one" });
  });
export type ChangePasswordValues = z.infer<typeof changePasswordSchema>;
