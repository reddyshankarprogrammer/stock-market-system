// Saves a service's OpenAPI document into openapi/ so the TypeScript types can be generated from a file that is
// reviewed in Git. Needs the service running locally.
//   node scripts/fetch-openapi.mjs account-service http://localhost:8086
import { writeFileSync, mkdirSync } from "node:fs";

const [name, base] = process.argv.slice(2);
if (!name || !base) {
  console.error("Usage: node scripts/fetch-openapi.mjs <service> <base url>");
  process.exit(2);
}
const res = await fetch(`${base}/v3/api-docs`);
if (!res.ok) {
  console.error(`${base}/v3/api-docs answered ${res.status}`);
  process.exit(1);
}
// Browsers never send X-User-* headers (the gateway sets them from the JWT), so the typed client must not ask for them.
const GATEWAY_HEADERS = new Set(["x-user-id", "x-user-role"]);
const stripGatewayHeaders = (spec) => {
  for (const item of Object.values(spec.paths ?? {})) {
    for (const op of Object.values(item)) {
      if (Array.isArray(op?.parameters)) op.parameters = op.parameters.filter((p) => !(p.in === "header" && GATEWAY_HEADERS.has(String(p.name).toLowerCase())));
    }
  }
  return spec;
};
const sortKeys = (v) => (Array.isArray(v) ? v.map(sortKeys) : v && typeof v === "object" ? Object.fromEntries(Object.keys(v).sort().map((k) => [k, sortKeys(v[k])])) : v);
mkdirSync("openapi", { recursive: true });
writeFileSync(`openapi/${name}.json`, JSON.stringify(sortKeys(stripGatewayHeaders(await res.json())), null, 2) + "\n");
console.log(`openapi/${name}.json written`);
