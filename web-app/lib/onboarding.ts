import { z } from "zod";
import { pricing } from "@/lib/content";

// The four onboarding steps and their choices. The same schema runs in the browser and in the route handlers.
export const experiences = [
  { code: "BEGINNER", label: "Beginner", text: "I am just starting out" },
  { code: "INTERMEDIATE", label: "Intermediate", text: "I trade regularly" },
  { code: "ADVANCED", label: "Advanced", text: "I have several years of experience" },
  { code: "PROFESSIONAL", label: "Professional", text: "I trade for a living or manage money for others" },
] as const;

export const brokers = [
  { code: "GROWW", label: "Groww" },
  { code: "ZERODHA", label: "Zerodha" },
  { code: "BINANCE", label: "Binance" },
  { code: "EXNESS", label: "Exness" },
  { code: "OTHER", label: "Another broker" },
  { code: "NONE", label: "I do not use a broker yet" },
] as const;

// Plan codes follow the plan names in lib/content.ts (the single source of the five plans).
const planCodes: Record<string, string> = { Free: "FREE", Pro: "PRO", "AI Pro": "AI_PRO", "Quant Pro": "QUANT_PRO", Enterprise: "ENTERPRISE" };
export const plans = pricing.plans.map((p) => ({ code: planCodes[p.name], name: p.name, audience: p.audience, points: p.points, paid: p.name !== "Free" }));

export const steps = ["Trading experience", "Broker preference", "Subscription plan", "Complete setup"] as const;

const experienceCodes = experiences.map((e) => e.code) as [string, ...string[]];
const brokerCodes = brokers.map((b) => b.code) as [string, ...string[]];
const planCodeList = plans.map((p) => p.code) as [string, ...string[]];

export const onboardingSchema = z
  .object({
    tradingExperience: z.enum(experienceCodes, { errorMap: () => ({ message: "Choose your trading experience" }) }).optional(),
    brokerPreferences: z.array(z.enum(brokerCodes)).min(1, "Choose at least one broker, or say you do not use one yet").max(6).optional(),
    plan: z.enum(planCodeList, { errorMap: () => ({ message: "Choose a plan" }) }).optional(),
  })
  .refine((v) => !(v.brokerPreferences?.includes("NONE") && v.brokerPreferences.length > 1), {
    path: ["brokerPreferences"],
    message: "\"I do not use a broker yet\" cannot be combined with a broker",
  });
export type OnboardingInput = z.infer<typeof onboardingSchema>;

export type Onboarding = {
  tradingExperience: string | null;
  brokerPreferences: string[];
  plan: string | null;
  completed: boolean;
  completedAt: string | null;
};

/** The first step that still needs an answer (0-2), or 3 (the summary) when all three are answered. */
export function firstIncompleteStep(o: Onboarding): number {
  if (!o.tradingExperience) return 0;
  if (o.brokerPreferences.length === 0) return 1;
  if (!o.plan) return 2;
  return 3;
}
