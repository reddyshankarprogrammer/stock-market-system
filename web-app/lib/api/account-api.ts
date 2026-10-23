import createClient from "openapi-fetch";
import { API_BASE, apiFetch, unwrap } from "@/lib/api/client";
import type { components, paths } from "@/lib/api/account-service";

// Typed client for account-service. Types come from openapi/account-service.json (npm run api:spec:account, then
// npm run api:types), so a changed endpoint breaks the build here instead of in the browser.
const client = createClient<paths>({ baseUrl: API_BASE, fetch: apiFetch });
// Uploads get a longer time limit: the statement is parsed and imported inside the request.
const uploadClient = createClient<paths>({ baseUrl: API_BASE, fetch: (request) => apiFetch(request, undefined, 60_000) });

export type Account = components["schemas"]["AccountResponse"];
export type CreateAccount = components["schemas"]["CreateAccountRequest"];
export type AccountSummary = components["schemas"]["AccountSummaryResponse"];
export type Performance = components["schemas"]["PerformanceResponse"];
export type Trade = components["schemas"]["TradeResponse"];
export type ManualTrade = components["schemas"]["ManualTradeRequest"];
export type Holding = components["schemas"]["HoldingResponse"];
export type Statement = components["schemas"]["AccountStatementResponse"];

export const accountApi = {
  /** The caller's own accounts. account-service checks the user against X-User-Id (set by the gateway), not this id alone. */
  listForUser: (userId: string) => unwrap(client.GET("/api/v1/accounts/user/{userId}", { params: { path: { userId } } })),
  get: (id: string) => unwrap(client.GET("/api/v1/accounts/{id}", { params: { path: { id } } })),
  create: (body: CreateAccount) => unwrap(client.POST("/api/v1/accounts", { body })),
  summary: (id: string) => unwrap(client.GET("/api/v1/accounts/{id}/summary", { params: { path: { id } } })),
  performance: (id: string) => unwrap(client.GET("/api/v1/accounts/{id}/performance", { params: { path: { id } } })),

  trades: (accountId: string, range: { from?: string; to?: string } = {}) =>
    unwrap(client.GET("/api/v1/trades/account/{accountId}", { params: { path: { accountId }, query: range } })),
  trade: (id: string) => unwrap(client.GET("/api/v1/trades/{id}", { params: { path: { id } } })),
  recordTrade: (body: ManualTrade) => unwrap(client.POST("/api/v1/trades", { body })),

  holdings: (accountId: string) => unwrap(client.GET("/api/v1/holdings/account/{accountId}", { params: { path: { accountId } } })),

  statements: (accountId: string) => unwrap(client.GET("/api/v1/accounts/{accountId}/statements", { params: { path: { accountId } } })),
  uploadStatement: (accountId: string, file: File) =>
    unwrap(
      uploadClient.POST("/api/v1/accounts/{accountId}/statements/upload", {
        params: { path: { accountId } },
        body: { file: file as unknown as string }, // the spec says "binary string"; the serializer below sends the File itself
        bodySerializer: () => {
          const form = new FormData();
          form.append("file", file);
          return form; // fetch sets the multipart Content-Type with its boundary
        },
      }),
    ),
  deleteStatement: (statementId: string) => unwrap(client.DELETE("/api/v1/statements/{statementId}", { params: { path: { statementId } } })),
};
