import { z } from "zod";

// The landing page waitlist. The same schema runs in the browser and in the route handler.
export const waitlistSchema = z.object({
  email: z.string().trim().toLowerCase().min(1, "Enter your email").max(254, "That email is too long").email("Enter a valid email address"),
  consent: z.boolean().refine((v) => v === true, "Please agree to be contacted about the launch"),
  // Honeypot: a field people never see or fill. A bot that fills it is answered with success and not stored.
  company: z.string().max(200).optional(),
});
export type WaitlistValues = z.input<typeof waitlistSchema>;
