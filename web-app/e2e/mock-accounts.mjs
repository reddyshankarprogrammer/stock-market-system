// Stand-in for account-service behind the mock gateway (accounts, trades, holdings, statements, summary, performance).
// It follows the documented behaviour in account-service/README.md and docs/account-service-statement-import.md, with
// numbers that are easy to check by hand. The mock user id is always u-1.
//
// Special accounts and files:
//   email noaccounts@...   starts with no accounts          email accounts-down@...  every account call answers 500
//   file dup.csv           409 (already uploaded)           file bad.txt             400 (wrong type)
//   file fail.csv          201 with an import that FAILED   any other .csv           parsed: columns symbol, trade_date, trade_type, quantity, price[, trade_id]
//   GET /__set-price?symbol=AAPL&price=250   moves a live price (the portfolio screen polls)
const PRICES = { AAPL: 200, INFY: 1600, TCS: 3700, MSFT: 400 };
const round = (n) => Math.round(n * 100) / 100;

export function createAccountMock() {
  const accounts = new Map(); // email -> accounts
  const trades = new Map(); // accountId -> trades
  const statements = new Map(); // accountId -> statements
  let seq = 100;
  let cur = ""; // the signed-in user of the current request: every user has their own trades and statements
  const K = (id) => `${cur}|${id}`;

  const price = (symbol, avg) => PRICES[symbol] ?? round(avg * 1.1);

  function seed(email) {
    cur = email;
    if (accounts.has(email)) return;
    if (email.startsWith("noaccounts@")) return void accounts.set(email, []);
    accounts.set(email, [
      { id: "a-1", userId: "u-1", accountNumber: "ACC-0001", accountName: "Long-term growth", accountType: "INDIVIDUAL", brokerName: "PLATFORM", currency: "USD", balance: 12500.5, status: "ACTIVE" },
      { id: "a-2", userId: "u-1", accountNumber: "ACC-0002", accountName: "Zerodha equity", accountType: "EQUITY", brokerName: "ZERODHA", currency: "INR", balance: 0, status: "ACTIVE" },
    ]);
    trades.set(K("a-1"), [
      mkTrade("a-1", "AAPL", "BUY", 10, 180, "2026-10-01T10:00:00", 0),
      mkTrade("a-1", "AAPL", "SELL", 4, 200, "2026-10-05T10:00:00", 80),
    ]);
  }

  function mkTrade(accountId, symbol, side, qty, px, date, realized, extra = {}) {
    const gross = round(qty * px);
    return { id: `t-${++seq}`, accountId, userId: "u-1", symbol, side, quantity: qty, price: px, buyPrice: side === "BUY" ? px : undefined, sellPrice: side === "SELL" ? px : undefined,
      grossAmount: gross, feeAmount: 0, taxAmount: 0, netAmount: side === "BUY" ? -gross : gross, realizedProfit: realized, tradeDate: date, exchange: "NSE", source: "PLATFORM", tradeKind: "EXECUTION", ...extra };
  }

  /** Open positions by average cost, like account-service. */
  function holdingsOf(accountId) {
    const by = new Map();
    for (const t of [...(trades.get(K(accountId)) ?? [])].sort((a, b) => a.tradeDate.localeCompare(b.tradeDate))) {
      const h = by.get(t.symbol) ?? { qty: 0, cost: 0 };
      if (t.side === "BUY") { h.qty += t.quantity; h.cost += t.quantity * t.price; }
      else { const avg = h.qty ? h.cost / h.qty : 0; h.qty -= t.quantity; h.cost -= t.quantity * avg; }
      by.set(t.symbol, h);
    }
    return [...by.entries()].filter(([, h]) => h.qty > 0.0000001).map(([symbol, h]) => {
      const avg = h.cost / h.qty, cur = price(symbol, avg), value = round(h.qty * cur), invested = round(h.cost);
      return { id: `h-${accountId}-${symbol}`, accountId, userId: "u-1", symbol, quantity: h.qty, averagePrice: round(avg), totalCost: invested, investedAmount: invested, currentPrice: cur,
        marketValue: value, unrealizedProfit: round(value - invested), unrealizedProfitPercent: invested ? round(((value - invested) / invested) * 100) : 0, realizedProfit: 0, priceUpdatedAt: "2026-10-24T09:00:00" };
    });
  }

  function figures(account) {
    const ts = trades.get(K(account.id)) ?? [], hs = holdingsOf(account.id);
    const realized = round(ts.reduce((s, t) => s + (t.realizedProfit ?? 0), 0));
    const unrealized = round(hs.reduce((s, h) => s + h.unrealizedProfit, 0));
    const market = round(hs.reduce((s, h) => s + h.marketValue, 0));
    const invested = round(hs.reduce((s, h) => s + h.investedAmount, 0));
    const fees = round(ts.reduce((s, t) => s + (t.feeAmount ?? 0), 0)), taxes = round(ts.reduce((s, t) => s + (t.taxAmount ?? 0), 0));
    return { realized, unrealized, market, invested, fees, taxes, open: hs.length };
  }

  function parseCsv(text, accountId) {
    const [head, ...lines] = text.split(/\r?\n/).filter((l) => l.trim());
    const cols = head.split(",").map((c) => c.trim().toLowerCase());
    const at = (row, name) => row[cols.indexOf(name)];
    const known = new Set((trades.get(K(accountId)) ?? []).map((t) => t.externalTradeId).filter(Boolean));
    let imported = 0, duplicates = 0, skipped = 0; const warnings = [];
    const list = trades.get(K(accountId)) ?? [];
    const avgs = new Map();
    lines.forEach((line, n) => {
      const row = line.split(",").map((c) => c.trim());
      const side = String(at(row, "trade_type")).toUpperCase();
      if (side !== "BUY" && side !== "SELL") { skipped++; warnings.push(`Row ${n + 2}: unknown side '${at(row, "trade_type")}'`); return; }
      const ext = at(row, "trade_id") || `${accountId}-${at(row, "symbol")}-${n}`;
      if (known.has(ext)) { duplicates++; return; }
      known.add(ext);
      const symbol = at(row, "symbol"), qty = Number(at(row, "quantity")), px = Number(at(row, "price"));
      const prior = list.filter((t) => t.symbol === symbol && t.side === "BUY");
      const avgBuy = prior.length ? prior.reduce((s, t) => s + t.price * t.quantity, 0) / prior.reduce((s, t) => s + t.quantity, 0) : px;
      avgs.set(symbol, avgBuy);
      list.push(mkTrade(accountId, symbol, side, qty, px, `${at(row, "trade_date")}T09:30:00`, side === "SELL" ? round((px - avgBuy) * qty) : 0, { externalTradeId: ext, source: "IMPORT" }));
      imported++;
    });
    trades.set(K(accountId), list);
    return { rowsRead: lines.length, imported, duplicates, skipped, warnings };
  }

  /** Returns true when the request was handled. */
  function handle(req, raw, body, claims, send) {
    const url = req.url.split("?")[0], query = new URL(req.url, "http://x").searchParams;
    const isAcct = url.startsWith("/api/v1/accounts") || url.startsWith("/api/v1/trades") || url.startsWith("/api/v1/holdings") || url.startsWith("/api/v1/statements");
    if (req.method === "GET" && url === "/__set-price") { PRICES[query.get("symbol")] = Number(query.get("price")); send(200, PRICES); return true; }
    if (!isAcct) return false;
    if (!claims || claims.tokenType !== "ACCESS" || claims.exp < Math.floor(Date.now() / 1000)) return send(401, { status: 401, message: "Unauthorized" }), true;
    if (claims.email.startsWith("accounts-down@")) return send(500, { status: 500, message: "boom" }), true;
    cur = claims.email;
    seed(claims.email);
    const mine = accounts.get(claims.email);
    const own = (id) => mine.find((a) => a.id === id);
    const deny = () => send(404, { status: 404, error: "Not Found", message: "Account not found" });
    let m;

    if (req.method === "GET" && (m = url.match(/^\/api\/v1\/accounts\/user\/([^/]+)$/))) return (m[1] === "u-1" ? send(200, mine) : send(403, { status: 403, error: "Forbidden", message: "You can only view your own accounts" })), true;
    if (req.method === "POST" && url === "/api/v1/accounts") {
      if (!body.accountName) return send(400, { status: 400, message: "Account name is required" }), true;
      const created = { id: `a-${mine.length + 1}`, userId: "u-1", accountNumber: `ACC-${String(mine.length + 1).padStart(4, "0")}`, status: "ACTIVE", balance: 0, ...body };
      mine.push(created);
      return send(201, created), true;
    }
    if ((m = url.match(/^\/api\/v1\/accounts\/([^/]+)$/)) && req.method === "GET") return (own(m[1]) ? send(200, own(m[1])) : deny()), true;
    if ((m = url.match(/^\/api\/v1\/accounts\/([^/]+)\/summary$/))) {
      const a = own(m[1]); if (!a) return deny(), true;
      const f = figures(a);
      return send(200, { accountId: a.id, accountName: a.accountName, accountNumber: a.accountNumber, currency: a.currency, status: a.status, cashBalance: a.balance, investedAmount: f.invested, marketValue: f.market,
        totalEquity: round(a.balance + f.market), openPositions: f.open, pricesLive: true, realizedProfit: f.realized, unrealizedProfit: f.unrealized, totalFees: f.fees, totalTaxes: f.taxes,
        totalDeposits: 0, totalWithdrawals: 0, totalDividends: 0, totalInterest: 0 }), true;
    }
    if ((m = url.match(/^\/api\/v1\/accounts\/([^/]+)\/performance$/))) {
      const a = own(m[1]); if (!a) return deny(), true;
      const f = figures(a);
      return send(200, { accountId: a.id, userId: "u-1", brokerName: a.brokerName, realizedProfit: f.realized, unrealizedProfit: f.unrealized, grossProfit: round(f.realized + f.unrealized + f.fees),
        totalCharges: f.fees, totalTaxes: f.taxes, netProfit: round(f.realized + f.unrealized), pricesLive: true, calculatedAt: "2026-10-24T09:00:00" }), true;
    }
    if ((m = url.match(/^\/api\/v1\/trades\/account\/([^/]+)$/))) {
      if (!own(m[1])) return deny(), true;
      const from = query.get("from"), to = query.get("to");
      if (Boolean(from) !== Boolean(to)) return send(400, { status: 400, error: "Bad Request", message: "Give both 'from' and 'to', or neither" }), true; // like account-service
      const list = (trades.get(K(m[1])) ?? []).filter((t) => (!from || t.tradeDate.slice(0, 10) >= from) && (!to || t.tradeDate.slice(0, 10) <= to));
      return send(200, list), true;
    }
    if (req.method === "GET" && (m = url.match(/^\/api\/v1\/trades\/([^/]+)$/))) {
      const t = mine.flatMap((a) => trades.get(K(a.id)) ?? []).find((x) => x.id === m[1]);
      return (t ? send(200, t) : send(404, { status: 404, error: "Not Found", message: "Trade not found" })), true;
    }
    if (req.method === "POST" && (url === "/api/v1/trades/buy" || url === "/api/v1/trades/sell")) {
      const a = own(body.accountId); if (!a) return deny(), true;
      const side = url.endsWith("buy") ? "BUY" : "SELL", px = PRICES[body.symbol];
      if (!px) return send(400, { status: 400, message: `No price for ${body.symbol}` }), true;
      const held = holdingsOf(a.id).find((h) => h.symbol === body.symbol)?.quantity ?? 0;
      if (side === "SELL" && held < body.quantity) return send(400, { status: 400, message: "Insufficient shares to sell" }), true;
      if (side === "BUY" && a.balance < body.quantity * px) return send(400, { status: 400, message: "Insufficient funds" }), true;
      const avg = holdingsOf(a.id).find((h) => h.symbol === body.symbol)?.averagePrice ?? px;
      const t = mkTrade(a.id, body.symbol, side, body.quantity, px, "2026-10-24T09:30:00", side === "SELL" ? round((px - avg) * body.quantity) : 0);
      a.balance = round(a.balance + (side === "SELL" ? 1 : -1) * body.quantity * px);
      trades.set(K(a.id), [...(trades.get(K(a.id)) ?? []), t]);
      return send(201, t), true;
    }
    if (req.method === "POST" && url === "/api/v1/trades") {
      const a = own(body.accountId); if (!a) return deny(), true;
      if (a.brokerName === "PLATFORM" || a.brokerName === "PAPER") return send(400, { status: 400, message: "Use /trades/buy or /trades/sell for platform accounts" }), true;
      if (!body.symbol || !body.quantity || !(body.buyPrice || body.sellPrice)) return send(400, { status: 400, message: "Symbol, quantity and a price are required" }), true;
      const list = trades.get(K(a.id)) ?? []; trades.set(K(a.id), list);
      const px = Number(body.side === "SELL" ? body.sellPrice : body.buyPrice);
      const prior = list.filter((t) => t.symbol === body.symbol && t.side === "BUY");
      const avgBuy = prior.length ? prior.reduce((s, t) => s + t.price * t.quantity, 0) / prior.reduce((s, t) => s + t.quantity, 0) : px;
      const t = mkTrade(a.id, body.symbol, body.side, Number(body.quantity), px, body.tradeDate ?? "2026-10-24T09:30:00", body.side === "SELL" ? round((px - avgBuy) * Number(body.quantity)) : 0, { source: "MANUAL", notes: body.notes });
      list.push(t);
      return send(201, t), true;
    }
    if ((m = url.match(/^\/api\/v1\/holdings\/account\/([^/]+)$/))) return (own(m[1]) ? send(200, holdingsOf(m[1])) : deny()), true;
    if (req.method === "GET" && (m = url.match(/^\/api\/v1\/accounts\/([^/]+)\/statements$/))) return (own(m[1]) ? send(200, statements.get(K(m[1])) ?? []) : deny()), true;
    if (req.method === "POST" && (m = url.match(/^\/api\/v1\/accounts\/([^/]+)\/statements\/upload$/))) {
      const a = own(m[1]); if (!a) return deny(), true;
      const name = /filename="([^"]+)"/.exec(raw)?.[1] ?? "unknown";
      if (!/\.(csv|xls|xlsx)$/i.test(name)) return send(400, { status: 400, error: "Bad Request", message: "Only xlsx, xls and csv files are accepted" }), true;
      if (name === "dup.csv") return send(409, { status: 409, error: "Conflict", message: "This statement was already uploaded", existingId: "s-1" }), true;
      const content = raw.split(/\r?\n\r?\n/).slice(1).join("\n\n").replace(/\r?\n--[-\w]+--\s*$/, "");
      const started = "2026-10-24T09:00:00";
      let imp;
      if (name === "fail.csv") imp = { status: "FAILED", rowsRead: 0, rowsSkipped: 0, tradesImported: 0, duplicateTrades: 0, errorMessage: "No trade table found in the file", warnings: [], startedAt: started };
      else {
        const r = parseCsv(content, a.id);
        imp = { status: r.skipped ? "PARTIAL" : "SUCCESS", rowsRead: r.rowsRead, rowsSkipped: r.skipped, tradesImported: r.imported, duplicateTrades: r.duplicates, chargesImported: 0, taxesImported: 0, warnings: r.warnings, startedAt: started };
      }
      const s = { id: `s-${++seq}`, accountId: a.id, userId: "u-1", brokerName: a.brokerName, originalFileName: name, fileFormat: "CSV", fileSize: content.length, status: imp.status, uploadedAt: started, latestImport: imp };
      const list = statements.get(K(a.id)) ?? []; statements.set(K(a.id), [s, ...list]);
      return send(201, s), true;
    }
    if (req.method === "DELETE" && (m = url.match(/^\/api\/v1\/statements\/([^/]+)$/))) {
      for (const [key, list] of statements) if (key.startsWith(`${cur}|`) && list.some((s) => s.id === m[1])) statements.set(key, list.filter((s) => s.id !== m[1]));
      return send(204, {}), true;
    }
    return false;
  }

  return { handle };
}
