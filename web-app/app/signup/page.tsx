import { redirect } from "next/navigation";

// Old path used by earlier links; registration now lives at /register.
export default function Page() {
  redirect("/register");
}
