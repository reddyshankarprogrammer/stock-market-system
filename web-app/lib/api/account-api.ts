import createClient from "openapi-fetch";
import { API_BASE, apiFetch, unwrap } from "@/lib/api/client";
import type { components, paths } from "@/lib/api/account-service";

// Typed client for account-service. Types come from openapi/account-service.json (npm run api:spec:account, then
// npm run api:types), so a changed endpoint breaks the build here instead of in the browser.
const client = createClient<paths>({ baseUrl: API_BASE, fetch: apiFetch });

export type Account = components["schemas"]["AccountResponse"];

export const accountApi = {
  /** The caller's own accounts. account-service checks the user against X-User-Id (set by the gateway), not this id alone. */
  listForUser: (userId: string) => unwrap(client.GET("/api/v1/accounts/user/{userId}", { params: { path: { userId } } })),
};
