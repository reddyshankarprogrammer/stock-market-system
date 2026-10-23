// Minimal stand-in for api-gateway + user-service, for the auth end-to-end test only. It mimics the documented behaviour
// in user-service/README.md: register (duplicate email -> 400), login (unknown email -> 404, wrong password -> 401), /me (Bearer token).
// Special emails trigger failures: down@ -> 500, slow@ -> responds after 9 s (longer than the 8 s server-side limit).
import { createServer } from "node:http";

const b64 = (o) => Buffer.from(JSON.stringify(o)).toString("base64url");
const jwt = (email, role = "USER") => `${b64({ alg: "HS256" })}.${b64({ sub: "u-1", email, role, exp: Math.floor(Date.now() / 1000) + 900 })}.sig`;

export function startMockGateway(port = 8099) {
  const users = new Map(); // email -> {firstName, lastName}, so /me returns what was registered
  const server = createServer(async (req, res) => {
    const send = (status, body) => {
      res.writeHead(status, { "Content-Type": "application/json" });
      res.end(JSON.stringify(body));
    };
    let raw = "";
    for await (const c of req) raw += c;
    const body = raw ? JSON.parse(raw) : {};

    if (req.method === "POST" && req.url === "/api/v1/users/login") {
      if (body.email === "down@example.com") return send(500, { status: 500, message: "boom" });
      if (body.email === "slow@example.com") return setTimeout(() => send(200, {}), 9000);
      if (body.email === "unknown@example.com") return send(404, { status: 404, message: `User not found: ${body.email}` });
      if (body.password === "WrongPass123") return send(401, { status: 401, message: "Invalid Credentials" });
      return send(200, { userId: "u-1", email: body.email, firstName: "Test", lastName: "User", walletBalance: 10000, accessToken: jwt(body.email), refreshToken: jwt(body.email), tokenType: "Bearer" });
    }
    if (req.method === "POST" && req.url === "/api/v1/users/register") {
      if (body.email === "taken@example.com") return send(400, { status: 400, message: `Email already registered ${body.email}` });
      users.set(body.email, { firstName: body.firstName, lastName: body.lastName });
      return send(200, { userId: "u-2", email: body.email, firstName: body.firstName, lastName: body.lastName, walletBalance: 10000, accessToken: jwt(body.email), refreshToken: jwt(body.email), tokenType: "Bearer" });
    }
    if (req.method === "GET" && req.url === "/api/v1/users/me") {
      const token = (req.headers.authorization ?? "").replace("Bearer ", "");
      if (!token) return send(401, { status: 401, message: "Unauthorized" });
      const claims = JSON.parse(Buffer.from(token.split(".")[1], "base64url").toString());
      const who = users.get(claims.email) ?? { firstName: "Test", lastName: "User" };
      return send(200, { id: claims.sub, email: claims.email, firstName: who.firstName, lastName: who.lastName, walletBalance: 10000, status: "ACTIVE", createdAt: "2026-10-24T00:00:00", role: claims.role });
    }
    send(404, { message: "not found" });
  });
  return new Promise((resolve) => server.listen(port, () => resolve(server)));
}
