import json, sys, uuid
out = sys.argv[1]


def tests(*lines):
    return [{"listen": "test", "script": {"type": "text/javascript", "exec": list(lines)}}]


def pre(*lines):
    return [{"listen": "prerequest", "script": {"type": "text/javascript", "exec": list(lines)}}]


def url(raw):
    base, _, query = raw.partition("?")
    host, path = base.split("}}", 1)
    u = {"raw": raw, "host": [host + "}}"], "path": [p for p in path.split("/") if p]}
    if query:
        u["query"] = [dict(zip(("key", "value"), q.split("=", 1))) for q in query.split("&")]
    return u


def req(name, method, raw, body=None, auth=True, headers=(), events=(), desc="", file=None):
    r = {"method": method, "header": [{"key": k, "value": v} for k, v in headers], "url": url(raw), "description": desc}
    if body is not None:
        r["header"].append({"key": "Content-Type", "value": "application/json"})
        r["body"] = {"mode": "raw", "raw": json.dumps(body, indent=2)}
    if file is not None:
        r["body"] = {"mode": "formdata", "formdata": [{"key": "file", "type": "file", "src": file}]}
    if not auth:
        r["auth"] = {"type": "noauth"}
    return {"name": name, "event": list(events), "request": r}


def status(code):
    return f"pm.test('Status is {code}', () => pm.response.to.have.status({code}));"


def folder(name, desc, items):
    return {"name": name, "description": desc, "item": items}


G = "{{gatewayUrl}}"

ADMIN_SKIP = pre("// Needs an ADMIN user: set adminEmail/adminPassword in the environment (see the user manual).",
                 "if (!pm.environment.get('adminAccessToken')) { pm.execution.skipRequest(); }")
ADMIN_AUTH = [("Authorization", "Bearer {{adminAccessToken}}")]
A = "{{gatewayUrl}}/api/v1"


def order(order_type, quantity, symbol="{{symbol}}"):
    return {"symbol": symbol, "type": order_type, "quantity": quantity}


SERVICES = [("api-gateway", "gatewayUrl"), ("user-service", "userServiceUrl"),
            ("market-data-service", "marketDataServiceUrl"), ("order-service", "orderServiceUrl"),
            ("portfolio-service", "portfolioServiceUrl"), ("notification-service", "notificationServiceUrl"),
            ("account-service", "accountServiceUrl")]

collection = {
    "info": {
        "_postman_id": "8f3d6c2a-5b1e-4c7d-9a0f-2e6b4d8c1a57",
        "name": "Stock Market System",
        "description": (
            "End-to-end tests for the stock-market microservices.\n\n"
            "Run folders 0-6 in order, or the whole collection with the Collection Runner. "
            "Register saves accessToken / refreshToken / userId to the environment; every later "
            "gateway request uses that token (collection-level Bearer auth).\n\n"
            "Folder 7 calls each service directly on its own port with an X-User-Id header, "
            "for debugging one service without the gateway."),
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "auth": {"type": "bearer", "bearer": [{"key": "token", "value": "{{accessToken}}", "type": "string"}]},
    "item": [
        folder("0. Health", "Actuator health of each service on its own port. All should be UP.", [
            req(f"{name} health", "GET", "{{" + var + "}}/actuator/health", auth=False,
                events=tests(status(200),
                             "pm.test('UP', () => pm.expect(pm.response.json().status).to.eql('UP'));"))
            for name, var in SERVICES]),

        folder("1. Users & Auth",
               "user-service through the gateway. Register creates a fresh user on every run.", [
                   req("Register", "POST", f"{G}/api/v1/users/register", auth=False,
                       body={"email": "{{email}}", "password": "{{password}}", "firstName": "Asha",
                             "lastName": "Rao", "initialBalance": 10000,
                             "mobileCountryCode": "+91", "mobileNumber": "9876543210", "gender": "FEMALE",
                             "country": "IN", "termsVersion": "2026-10-24-draft"},
                       events=pre("// Unique email per run so Register never collides with an existing user",
                                  "pm.environment.set('email', `user${Date.now()}@example.com`);")
                       + tests(status(201),
                               "const b = pm.response.json();",
                               "pm.environment.set('accessToken', b.accessToken);",
                               "pm.environment.set('refreshToken', b.refreshToken);",
                               "pm.environment.set('userId', b.userId);",
                               "pm.test('Wallet starts at 10000', () => pm.expect(b.walletBalance).to.eql(10000));"),
                       desc="Publishes user.registered; notification-service creates a WELCOME notification."),
                   req("Login", "POST", f"{G}/api/v1/users/login", auth=False,
                       body={"email": "{{email}}", "password": "{{password}}"},
                       events=tests(status(200),
                                    "pm.environment.set('accessToken', pm.response.json().accessToken);")),
                   req("Refresh token", "POST", f"{G}/api/v1/users/refresh", auth=False,
                       body={"refreshToken": "{{refreshToken}}"},
                       events=tests(status(200),
                                    "const b = pm.response.json();",
                                    "pm.test('New access token, same refresh token', () => { pm.expect(b.accessToken).to.be.a('string').and.not.empty; pm.expect(b.refreshToken).to.eql(pm.environment.get('refreshToken')); });",
                                    "pm.environment.set('accessToken', b.accessToken); // the next requests prove it works"),
                       desc="Public: the refresh token goes in the body, because the gateway rejects it as a Bearer token."),
                   req("Refresh - an access token is rejected", "POST", f"{G}/api/v1/users/refresh", auth=False,
                       body={"refreshToken": "{{accessToken}}"},
                       events=tests(status(401),
                                    "pm.test('Invalid refresh token', () => pm.expect(pm.response.json().message).to.eql('Invalid refresh token'));")),
                   req("Refresh - garbage is rejected", "POST", f"{G}/api/v1/users/refresh", auth=False,
                       body={"refreshToken": "not-a-token"}, events=tests(status(401))),
                   req("Refresh - missing token", "POST", f"{G}/api/v1/users/refresh", auth=False,
                       body={}, events=tests(status(400))),
                   req("Forgot password", "POST", f"{G}/api/v1/users/forgot-password", auth=False,
                       body={"email": "{{email}}"},
                       events=tests(status(202),
                                    "pm.test('Generic message', () => pm.expect(pm.response.json().message).to.eql('If an account exists for that email, a reset link has been sent'));"),
                       desc="Emails a single-use link (local inbox: Mailpit at http://localhost:8025). Never reveals whether the email is registered."),
                   req("Forgot password - unknown email gives the same answer", "POST", f"{G}/api/v1/users/forgot-password", auth=False,
                       body={"email": "nobody-{{$timestamp}}@example.com"},
                       events=tests(status(202),
                                    "pm.test('Same message as for a real email', () => pm.expect(pm.response.json().message).to.eql('If an account exists for that email, a reset link has been sent'));")),
                   req("Forgot password - invalid email", "POST", f"{G}/api/v1/users/forgot-password", auth=False,
                       body={"email": "not-an-email"}, events=tests(status(400))),
                   req("Reset password - invalid token", "POST", f"{G}/api/v1/users/reset-password", auth=False,
                       body={"token": "not-a-real-token", "newPassword": "SomeNewPass123"},
                       events=tests(status(400),
                                    "pm.test('One generic message', () => pm.expect(pm.response.json().message).to.eql('This reset link is invalid or has expired'));")),
                   req("Reset password - password too short", "POST", f"{G}/api/v1/users/reset-password", auth=False,
                       body={"token": "not-a-real-token", "newPassword": "123"}, events=tests(status(400))),
                   req("Login - wrong password", "POST", f"{G}/api/v1/users/login", auth=False,
                       body={"email": "{{email}}", "password": "wrong-password"},
                       events=tests(status(401),
                                    "pm.test('Generic message', () => pm.expect(pm.response.json().message).to.eql('Invalid Credentials'));")),
                   req("Login - unknown email gives the same 401", "POST", f"{G}/api/v1/users/login", auth=False,
                       body={"email": "nobody-{{$timestamp}}@example.com", "password": "wrong-password"},
                       events=tests(status(401),
                                    "pm.test('Same message as a wrong password', () => pm.expect(pm.response.json().message).to.eql('Invalid Credentials'));")),
                   req("Register - invalid body", "POST", f"{G}/api/v1/users/register", auth=False,
                       body={"email": "not-an-email", "password": "123"},
                       events=tests(status(400),
                                    "pm.test('Field errors', () => pm.expect(pm.response.json().errors).to.have.property('email'));")),
                   req("Get my profile", "GET", f"{G}/api/v1/users/me",
                       events=tests(status(200),
                                    "pm.test('Is me', () => pm.expect(pm.response.json().id).to.eql(pm.environment.get('userId')));",
                                    "const me = pm.response.json();",
                                    "pm.test('Profile fields were saved', () => { pm.expect(me.mobileCountryCode).to.eql('+91'); pm.expect(me.mobileNumber).to.eql('9876543210'); pm.expect(me.gender).to.eql('FEMALE'); pm.expect(me.country).to.eql('IN'); pm.expect(me.termsVersion).to.eql('2026-10-24-draft'); });",
                                    "pm.test('Terms acceptance time is set by the server', () => pm.expect(me.termsAcceptedAt).to.be.a('string'));")),
                   req("Onboarding - not started", "GET", f"{G}/api/v1/users/me/onboarding",
                       events=tests(status(200),
                                    "const o = pm.response.json();",
                                    "pm.test('Empty for a new user', () => { pm.expect(o.completed).to.eql(false); pm.expect(o.brokerPreferences).to.eql([]); pm.expect(o.tradingExperience).to.eql(null); });")),
                   req("Onboarding - bad values are rejected", "PUT", f"{G}/api/v1/users/me/onboarding",
                       body={"tradingExperience": "GURU"}, events=tests(status(400))),
                   req("Onboarding - NONE cannot be combined with a broker", "PUT", f"{G}/api/v1/users/me/onboarding",
                       body={"brokerPreferences": ["NONE", "GROWW"]}, events=tests(status(400))),
                   req("Onboarding - complete needs all answers", "POST", f"{G}/api/v1/users/me/onboarding/complete", body={},
                       events=tests(status(400))),
                   req("Onboarding - save answers (partial saves keep earlier ones)", "PUT", f"{G}/api/v1/users/me/onboarding",
                       body={"tradingExperience": "ADVANCED", "brokerPreferences": ["ZERODHA", "GROWW", "GROWW"], "plan": "PRO"},
                       events=tests(status(200),
                                    "const o = pm.response.json();",
                                    "pm.test('Saved, sorted, de-duplicated', () => { pm.expect(o.tradingExperience).to.eql('ADVANCED'); pm.expect(o.brokerPreferences).to.eql(['GROWW', 'ZERODHA']); pm.expect(o.plan).to.eql('PRO'); pm.expect(o.completed).to.eql(false); });")),
                   req("Onboarding - complete", "POST", f"{G}/api/v1/users/me/onboarding/complete", body={},
                       events=tests(status(200),
                                    "pm.test('Completed with a time', () => { const o = pm.response.json(); pm.expect(o.completed).to.eql(true); pm.expect(o.completedAt).to.be.a('string'); });")),
                   req("Profile now shows onboarding as done", "GET", f"{G}/api/v1/users/me",
                       events=tests(status(200),
                                    "pm.test('onboardingCompletedAt is set', () => pm.expect(pm.response.json().onboardingCompletedAt).to.be.a('string'));")),
                   req("Settings - defaults for a user who never saved", "GET", f"{G}/api/v1/users/me/preferences",
                       events=tests(status(200),
                                    "const p = pm.response.json();",
                                    "pm.test('Defaults', () => { pm.expect(p.theme).to.eql('SYSTEM'); pm.expect(p.notifyInApp).to.eql(true); pm.expect(p.notifyPush).to.eql(false); pm.expect(p.notifyEmail).to.eql(false); pm.expect(p.exportFormat).to.eql('CSV'); });")),
                   req("Settings - bad values are rejected", "PUT", f"{G}/api/v1/users/me/preferences",
                       body={"theme": "NEON"}, events=tests(status(400))),
                   req("Settings - save one field, the others keep their value", "PUT", f"{G}/api/v1/users/me/preferences",
                       body={"theme": "DARK", "userId": "someone-else"},
                       events=tests(status(200),
                                    "const p = pm.response.json();",
                                    "pm.test('Theme saved, rest unchanged', () => { pm.expect(p.theme).to.eql('DARK'); pm.expect(p.exportFormat).to.eql('CSV'); pm.expect(p.notifyInApp).to.eql(true); });")),
                   req("Settings - read back", "GET", f"{G}/api/v1/users/me/preferences",
                       events=tests(status(200), "pm.test('Persisted', () => pm.expect(pm.response.json().theme).to.eql('DARK'));")),
                   req("Change password - wrong current password is 422, not 401", "POST", f"{G}/api/v1/users/me/password",
                       body={"currentPassword": "definitely-wrong-1", "newPassword": "SomethingNew12345"}, events=tests(status(422))),
                   req("Change password - new password too short", "POST", f"{G}/api/v1/users/me/password",
                       body={"currentPassword": "x", "newPassword": "abc"}, events=tests(status(400))),
                   req("Get user by id (own)", "GET", f"{G}/api/v1/users/{{{{userId}}}}", events=tests(status(200))),
                   req("Add funds", "POST", f"{G}/api/v1/users/{{{{userId}}}}/funds/add?amount=5000",
                       events=tests(status(200),
                                    "pm.test('Balance increased', () => pm.expect(pm.response.json().walletBalance).to.be.above(10000));")),
               ]),

        folder("2. Market Data",
               "market-data-service through the gateway (public, no token). Prices move every market.price-update-interval ms.", [
                   req("All stocks", "GET", f"{G}/api/v1/market-data/stocks", auth=False,
                       events=tests(status(200),
                                    "pm.test('Seeded stocks present', () => pm.expect(pm.response.json().length).to.be.above(0));")),
                   req("Stock by symbol", "GET", f"{G}/api/v1/market-data/stocks/{{{{symbol}}}}", auth=False,
                       events=tests(status(200), "pm.environment.set('lastPrice', pm.response.json().price);")),
                   req("Stock list (text)", "GET", f"{G}/api/v1/market-data/stocks/list", auth=False,
                       events=tests(status(200))),
                   req("Unknown symbol", "GET", f"{G}/api/v1/market-data/stocks/ZZZZ", auth=False,
                       events=tests(status(404))),
               ]),

        folder("3. Orders",
               "order-service through the gateway. Orders fill at the current market price; "
               "BUY deducts and SELL credits the wallet via user-service.", [
                   req("Place BUY order", "POST", f"{G}/api/v1/orders", body=order("BUY", 5),
                       events=tests(status(201),
                                    "const o = pm.response.json();",
                                    "pm.environment.set('orderId', o.id);",
                                    "pm.test('Executed', () => pm.expect(o.status).to.eql('EXECUTED'));",
                                    "pm.test('total = price x qty', () => pm.expect(o.totalAmount).to.be.closeTo(o.price * o.quantity, 0.01));"),
                       desc="Publishes order.executed; portfolio-service adds the holding and notification-service notifies."),
                   req("Place SELL order", "POST", f"{G}/api/v1/orders", body=order("SELL", 2),
                       events=tests(status(201),
                                    "pm.test('Executed', () => pm.expect(pm.response.json().status).to.eql('EXECUTED'));")),
                   req("Get order by id", "GET", f"{G}/api/v1/orders/{{{{orderId}}}}",
                       events=tests(status(200),
                                    "pm.test('Same order', () => pm.expect(pm.response.json().id).to.eql(pm.environment.get('orderId')));")),
                   req("My orders", "GET", f"{G}/api/v1/orders/my-orders",
                       events=tests(status(200),
                                    "pm.test('Has orders', () => pm.expect(pm.response.json().length).to.be.at.least(2));")),
                   req("BUY - insufficient funds", "POST", f"{G}/api/v1/orders", body=order("BUY", 1000000),
                       events=tests(status(422),
                                    "pm.test('Reason given', () => pm.expect(pm.response.json().message).to.include('Insufficient'));"),
                       desc="Saved as FAILED; order.failed is published and an ORDER_FAILED notification is created."),
                   req("BUY - unknown symbol", "POST", f"{G}/api/v1/orders", body=order("BUY", 1, "ZZZZ"),
                       events=tests(status(404))),
                   req("BUY - invalid body", "POST", f"{G}/api/v1/orders", body={"symbol": "", "quantity": 0},
                       events=tests(status(400))),
                   req("Order not found", "GET", f"{G}/api/v1/orders/does-not-exist", events=tests(status(404))),
               ]),

        folder("4. Portfolio",
               "portfolio-service through the gateway. Holdings update asynchronously from order.executed; "
               "if a check fails right after placing orders, send it again after a second.", [
                   req("My portfolio", "GET", f"{G}/api/v1/portfolio/me",
                       events=tests(status(200),
                                    "const p = pm.response.json();",
                                    "const h = p.holdings.find(x => x.symbol === pm.environment.get('symbol'));",
                                    "pm.test('Holding = BUY 5 - SELL 2', () => pm.expect(h && h.quantity).to.eql(3));")),
               ]),

        folder("5. Notifications",
               "notification-service through the gateway. Expect WELCOME, ORDER_EXECUTED and ORDER_FAILED for this run's user.", [
                   req("My notifications", "GET", f"{G}/api/v1/notifications",
                       events=tests(status(200),
                                    "const types = pm.response.json().map(n => n.type);",
                                    "['WELCOME', 'ORDER_EXECUTED', 'ORDER_FAILED'].forEach(t =>",
                                    "    pm.test(`Has ${t}`, () => pm.expect(types).to.include(t)));")),
               ]),

        folder("6. Gateway security", "Negative tests for JwtAuthFilter and the ownership checks.", [
            req("No token -> 401", "GET", f"{G}/api/v1/orders/my-orders", auth=False, events=tests(status(401))),
            req("Garbage token -> 401", "GET", f"{G}/api/v1/orders/my-orders", auth=False,
                headers=[("Authorization", "Bearer not-a-real-token")], events=tests(status(401))),
            req("Refresh token as access -> 401", "GET", f"{G}/api/v1/orders/my-orders", auth=False,
                headers=[("Authorization", "Bearer {{refreshToken}}")], events=tests(status(401))),
            req("Spoofed X-User-Id is ignored", "GET", f"{G}/api/v1/users/me",
                headers=[("X-User-Id", "someone-else")],
                events=tests(status(200),
                             "pm.test('Still me', () => pm.expect(pm.response.json().id).to.eql(pm.environment.get('userId')));")),
            req("Other user's profile -> 403", "GET", f"{G}/api/v1/users/someone-else", events=tests(status(403))),
            req("Internal deduct blocked -> 403", "POST", f"{G}/api/v1/users/{{{{userId}}}}/funds/deduct?amount=1",
                events=tests(status(403))),
        ]),

        folder("7. Account Service - platform trading",
               "account-service PLATFORM account through the gateway: cash, market-price trades, fees, taxes, ledger.", [
                   req("Create platform account", "POST", f"{A}/accounts",
                       body={"accountName": "Main", "accountType": "INDIVIDUAL"},
                       events=tests(status(201), "const a = pm.response.json();",
                                    "pm.environment.set('platformAccountId', a.id);",
                                    "pm.test('PLATFORM, ACTIVE, zero balance', () => { pm.expect(a.brokerName).to.eql('PLATFORM'); pm.expect(a.status).to.eql('ACTIVE'); pm.expect(a.balance).to.eql(0); });")),
                   req("Deposit", "POST", f"{A}/accounts/{{{{platformAccountId}}}}/deposit", body={"amount": 10000.00, "reference": "BANK-1"},
                       events=tests(status(201), "pm.test('Balance 10000', () => pm.expect(pm.response.json().balanceAfter).to.eql(10000));")),
                   req("Buy AAPL", "POST", f"{A}/trades/buy", body={"accountId": "{{platformAccountId}}", "symbol": "AAPL", "quantity": 5},
                       events=tests(status(201), "const t = pm.response.json();",
                                    "pm.environment.set('platformTradeId', t.id);",
                                    "pm.test('Executed at market price with fees', () => { pm.expect(t.source).to.eql('PLATFORM'); pm.expect(t.feeAmount).to.be.above(0); pm.expect(t.balanceAfter).to.be.below(10000); });")),
                   req("Sell AAPL", "POST", f"{A}/trades/sell", body={"accountId": "{{platformAccountId}}", "symbol": "AAPL", "quantity": 2},
                       events=tests(status(201), "pm.test('Realized profit set', () => pm.expect(pm.response.json().realizedProfit).to.not.be.null);")),
                   req("Sell more than held -> 400", "POST", f"{A}/trades/sell", body={"accountId": "{{platformAccountId}}", "symbol": "AAPL", "quantity": 100},
                       events=tests(status(400))),
                   req("Holdings", "GET", f"{A}/holdings/account/{{{{platformAccountId}}}}",
                       events=tests(status(200), "pm.test('AAPL 3 held', () => pm.expect(pm.response.json()[0].quantity).to.eql(3));")),
                   req("Withdraw", "POST", f"{A}/accounts/{{{{platformAccountId}}}}/withdraw", body={"amount": 100.00},
                       events=tests(status(201), "pm.test('Withdrawal fee charged', () => pm.expect(pm.response.json().feeAmount).to.eql(2));")),
                   req("Fund transactions", "GET", f"{A}/accounts/{{{{platformAccountId}}}}/fund-transactions",
                       events=tests(status(200), "const types = pm.response.json().map(t => t.transactionType);",
                                    "pm.test('Deposit and withdrawal listed', () => pm.expect(types).to.include.members(['DEPOSIT', 'WITHDRAWAL']));")),
                   req("Summary", "GET", f"{A}/accounts/{{{{platformAccountId}}}}/summary",
                       events=tests(status(200), "pm.test('One open position', () => pm.expect(pm.response.json().openPositions).to.eql(1));")),
                   req("Statement (today)", "GET", f"{A}/accounts/{{{{platformAccountId}}}}/statement?from={{{{today}}}}&to={{{{today}}}}",
                       events=pre("pm.environment.set('today', new Date().toISOString().slice(0, 10));")
                       + tests(status(200), "const s = pm.response.json();",
                               "pm.test('Closing = opening + credits - debits', () => pm.expect(s.closingBalance).to.be.closeTo(s.openingBalance + s.totalCredits - s.totalDebits, 0.001));")),
                   req("Ledger (today)", "GET", f"{A}/accounts/{{{{platformAccountId}}}}/ledger?from={{{{today}}}}&to={{{{today}}}}",
                       events=tests(status(200), "const l = pm.response.json();",
                                    "pm.test('Entries numbered from 1', () => pm.expect(l[0].entryNumber).to.eql(1));")),
                   req("Fee schedule", "GET", f"{A}/fees", events=tests(status(200), "pm.test('4 fees', () => pm.expect(pm.response.json().length).to.eql(4));")),
                   req("Fee preview", "POST", f"{A}/fees/calculate", body={"amount": 50000.00, "kind": "TRADE"},
                       events=tests(status(200), "pm.test('Brokerage capped: 27.25', () => pm.expect(pm.response.json().totalFees).to.eql(27.25));")),
                   req("Fees charged to account", "GET", f"{A}/fees/account/{{{{platformAccountId}}}}",
                       events=tests(status(200), "const types = pm.response.json().map(f => f.feeType);",
                                    "pm.test('Trade and withdrawal fees', () => pm.expect(types).to.include.members(['BROKERAGE', 'PLATFORM', 'WITHDRAWAL']));")),
                   req("Tax rules", "GET", f"{A}/taxes",
                       events=tests(status(200), "pm.test('3 active rules', () => pm.expect(pm.response.json().length).to.eql(3));")),
                   req("Tax preview", "POST", f"{A}/taxes/calculate", body={"side": "SELL", "tradeValue": 1000.00, "realizedProfit": 100.00},
                       events=tests(status(200), "pm.test('0.50 trade tax + 10.00 CGT', () => pm.expect(pm.response.json().totalTax).to.eql(10.5));")),
                   req("Taxes charged to account", "GET", f"{A}/taxes/account/{{{{platformAccountId}}}}",
                       events=tests(status(200), "pm.test('SELL trade tax recorded', () => pm.expect(pm.response.json().map(t => t.taxType)).to.include('TRADE_TAX'));")),
                   req("Performance", "GET", f"{A}/accounts/{{{{platformAccountId}}}}/performance",
                       events=tests(status(200), "const p = pm.response.json();",
                                    "pm.test('net = gross - charges - taxes', () => pm.expect(p.netProfit).to.be.closeTo(p.grossProfit - p.totalCharges - p.totalTaxes, 0.001));")),
                   req("Audit trail", "GET", f"{A}/accounts/{{{{platformAccountId}}}}/audit",
                       events=tests(status(200), "const actions = pm.response.json().map(a => a.action);",
                                    "['CREATE', 'DEPOSIT', 'BUY', 'SELL', 'FEE_CALCULATION', 'TAX_CALCULATION', 'HOLDING_UPDATE', 'WITHDRAW'].forEach(x =>",
                                    "    pm.test(`Has ${x}`, () => pm.expect(actions).to.include(x)));")),
                   req("Rename account", "PUT", f"{A}/accounts/{{{{platformAccountId}}}}",
                       body={"accountName": "Main portfolio", "remarks": "renamed from Postman"},
                       events=tests(status(200), "const a = pm.response.json();",
                                    "pm.test('Renamed', () => { pm.expect(a.accountName).to.eql('Main portfolio'); pm.expect(a.remarks).to.eql('renamed from Postman'); });")),
                   req("Close account with balance -> 400", "DELETE", f"{A}/accounts/{{{{platformAccountId}}}}",
                       events=tests(status(400))),
                   req("Open a spare account", "POST", f"{A}/accounts", body={"accountName": "Spare", "accountType": "INDIVIDUAL"},
                       events=tests(status(201), "pm.environment.set('spareAccountId', pm.response.json().id);")),
                   req("Close empty account", "DELETE", f"{A}/accounts/{{{{spareAccountId}}}}",
                       events=tests(status(200), "const a = pm.response.json();",
                                    "pm.test('CLOSED with closedAt', () => { pm.expect(a.status).to.eql('CLOSED'); pm.expect(a.closedAt).to.be.a('string'); });")),
                   req("Deposit into closed account -> 400", "POST", f"{A}/accounts/{{{{spareAccountId}}}}/deposit", body={"amount": 10.00},
                       events=tests(status(400))),
               ]),

        folder("8. Account Service - tracking accounts and statements",
               "Tracking accounts: a ZERODHA account end to end (docs/postman/samples/zerodha-tradebook.csv), then one upload "
               "per other broker sample (Groww, Binance, Exness). Covers import history, "
               "holdings, charges, taxes, manual trades, adjustments. In the Postman app, select the sample file on the upload request "
               "if it is not found.", [
                   req("Create Zerodha account", "POST", f"{A}/accounts",
                       body={"accountName": "Zerodha Equity", "accountType": "EQUITY", "brokerName": "ZERODHA", "remarks": "Postman"},
                       events=tests(status(201), "pm.environment.set('trackingAccountId', pm.response.json().id);")),
                   req("Upload statement", "POST", f"{A}/accounts/{{{{trackingAccountId}}}}/statements/upload",
                       file="samples/zerodha-tradebook.csv",
                       events=tests(status(201), "const s = pm.response.json();",
                                    "pm.environment.set('statementId', s.id);",
                                    "pm.test('Imported', () => { pm.expect(s.status).to.eql('PROCESSED'); pm.expect(s.latestImport.tradesImported).to.eql(3); pm.expect(s.latestImport.chargesImported).to.eql(2); pm.expect(s.latestImport.taxesImported).to.eql(1); });")),
                   req("Upload same file again -> 409", "POST", f"{A}/accounts/{{{{trackingAccountId}}}}/statements/upload",
                       file="samples/zerodha-tradebook.csv",
                       events=tests(status(409), "pm.test('Points to the first upload', () => pm.expect(pm.response.json().existingId).to.eql(pm.environment.get('statementId')));")),
                   req("List statements", "GET", f"{A}/accounts/{{{{trackingAccountId}}}}/statements",
                       events=tests(status(200), "pm.test('One statement', () => pm.expect(pm.response.json().length).to.eql(1));")),
                   req("Get statement", "GET", f"{A}/statements/{{{{statementId}}}}", events=tests(status(200))),
                   req("Import history", "GET", f"{A}/statements/{{{{statementId}}}}/imports",
                       events=tests(status(200), "pm.test('Attempt 1 SUCCESS', () => pm.expect(pm.response.json()[0].status).to.eql('SUCCESS'));")),
                   req("Download statement", "GET", f"{A}/statements/{{{{statementId}}}}/download",
                       events=tests(status(200), "pm.test('Original CSV', () => pm.expect(pm.response.text()).to.include('order_execution_time'));")),
                   req("Holdings (INFY 15 @ 1600)", "GET", f"{A}/holdings/account/{{{{trackingAccountId}}}}",
                       events=tests(status(200), "const h = pm.response.json()[0];",
                                    "pm.test('INFY 15 units, avg 1600, realized 1000', () => { pm.expect(h.quantity).to.eql(15); pm.expect(h.averagePrice).to.eql(1600); pm.expect(h.realizedProfit).to.eql(1000); });")),
                   req("Imported trades", "GET", f"{A}/trades/account/{{{{trackingAccountId}}}}",
                       events=tests(status(200), "pm.test('3 imported trades', () => pm.expect(pm.response.json().filter(t => t.source === 'IMPORT').length).to.eql(3));")),
                   req("Charge summary 2026", "GET", f"{A}/charges/account/{{{{trackingAccountId}}}}/summary?year=2026",
                       events=tests(status(200), "pm.test('35.50 charges', () => pm.expect(pm.response.json().totalCharges).to.eql(35.5));")),
                   req("Tax summary 2026", "GET", f"{A}/taxes/account/{{{{trackingAccountId}}}}/summary?year=2026",
                       events=tests(status(200), "pm.test('3.60 GST', () => pm.expect(pm.response.json().totalTaxes).to.eql(3.6));")),
                   req("Record manual trade", "POST", f"{A}/trades",
                       body={"accountId": "{{trackingAccountId}}", "symbol": "TCS", "exchange": "NSE", "side": "BUY", "quantity": 2,
                             "buyPrice": 3600, "brokerage": 5, "tradeDate": "2026-10-06T10:00:00", "externalTradeId": "MAN-{{$timestamp}}"},
                       events=tests(status(201), "const t = pm.response.json();", "pm.environment.set('manualTradeId', t.id);",
                                    "pm.test('MANUAL, no cash moved', () => pm.expect(t.source).to.eql('MANUAL'));")),
                   req("Get trade", "GET", f"{A}/trades/{{{{manualTradeId}}}}", events=tests(status(200))),
                   req("Manual trade on platform account -> 400", "POST", f"{A}/trades",
                       body={"accountId": "{{platformAccountId}}", "symbol": "AAPL", "side": "BUY", "quantity": 1, "buyPrice": 100,
                             "tradeDate": "2026-10-06T10:00:00"},
                       events=tests(status(400))),
                   req("Cash adjustment", "POST", f"{A}/accounts/{{{{trackingAccountId}}}}/adjustments",
                       body={"amount": 250.00, "description": "Opening cash at Zerodha", "statementId": "{{statementId}}"},
                       events=tests(status(201), "pm.test('ADJUSTMENT_IN', () => pm.expect(pm.response.json().transactionType).to.eql('ADJUSTMENT_IN'));")),
                   req("Charges list", "GET", f"{A}/charges/account/{{{{trackingAccountId}}}}",
                       events=tests(status(200), "pm.test('Statement and manual charges', () => pm.expect(pm.response.json().length).to.eql(3));")),
                   req("Performance", "GET", f"{A}/accounts/{{{{trackingAccountId}}}}/performance",
                       events=tests(status(200), "const p = pm.response.json();",
                                    "pm.test('Realized 1000', () => pm.expect(p.realizedProfit).to.eql(1000));",
                                    "pm.test('net = gross - charges - taxes', () => pm.expect(p.netProfit).to.be.closeTo(p.grossProfit - p.totalCharges - p.totalTaxes, 0.001));")),
                   req("Reprocess statement", "POST", f"{A}/statements/{{{{statementId}}}}/reprocess",
                       events=tests(status(200), "pm.test('Attempt 2, no duplicates', () => { const i = pm.response.json().latestImport; pm.expect(i.attemptNumber).to.eql(2); pm.expect(i.tradesImported).to.eql(3); });")),
                   req("Delete statement", "DELETE", f"{A}/statements/{{{{statementId}}}}", events=tests(status(204))),
                   req("Holdings after delete (TCS only)", "GET", f"{A}/holdings/account/{{{{trackingAccountId}}}}",
                       events=tests(status(200), "pm.test('Only the manual TCS trade remains', () => pm.expect(pm.response.json().map(h => h.symbol)).to.eql(['TCS']));")),
                   req("Groww: create account", "POST", f"{A}/accounts",
                       body={"accountName": "Groww", "accountType": "EQUITY", "brokerName": "GROWW"},
                       events=tests(status(201), "pm.environment.set('growwAccountId', pm.response.json().id);")),
                   req("Groww: upload groww-pnl.csv", "POST", f"{A}/accounts/{{{{growwAccountId}}}}/statements/upload",
                       file="samples/groww-pnl.csv",
                       events=tests(status(201), "const s = pm.response.json(); const i = s.latestImport;",
                                    "pm.test('2 closed positions, 3 charges, GST', () => { pm.expect(s.status).to.eql('PROCESSED'); pm.expect(i.tradesImported).to.eql(2); pm.expect(i.chargesImported).to.eql(3); pm.expect(i.taxesImported).to.eql(1); });")),
                   req("Binance: create account", "POST", f"{A}/accounts",
                       body={"accountName": "Binance", "accountType": "CRYPTO", "brokerName": "BINANCE"},
                       events=tests(status(201), "pm.environment.set('binanceAccountId', pm.response.json().id);")),
                   req("Binance: upload binance-trade-history.csv", "POST", f"{A}/accounts/{{{{binanceAccountId}}}}/statements/upload",
                       file="samples/binance-trade-history.csv",
                       events=tests(status(201), "const s = pm.response.json(); const i = s.latestImport;",
                                    "pm.test('2 fills imported', () => { pm.expect(s.status).to.eql('PROCESSED'); pm.expect(i.tradesImported).to.eql(2); });")),
                   req("Exness: create account", "POST", f"{A}/accounts",
                       body={"accountName": "Exness", "accountType": "FOREX", "brokerName": "EXNESS"},
                       events=tests(status(201), "pm.environment.set('exnessAccountId', pm.response.json().id);")),
                   req("Exness: upload exness-closed-positions.csv", "POST", f"{A}/accounts/{{{{exnessAccountId}}}}/statements/upload",
                       file="samples/exness-closed-positions.csv",
                       events=tests(status(201), "const s = pm.response.json(); const i = s.latestImport;",
                                    "pm.test('2 closed positions, 3 charges', () => { pm.expect(s.status).to.eql('PROCESSED'); pm.expect(i.tradesImported).to.eql(2); pm.expect(i.chargesImported).to.eql(3); });")),
               ]),

        folder("9. Account Service - roles",
               "USER vs ADMIN. ADMIN requests are skipped unless adminEmail/adminPassword are set for a user promoted with "
               "UPDATE users SET role='ADMIN' WHERE email='...'.", [
                   req("USER posts dividend -> 403", "POST", f"{A}/accounts/{{{{platformAccountId}}}}/dividends",
                       body={"symbol": "AAPL", "dividendPerShare": 0.5, "paymentDate": "2026-10-10"}, events=tests(status(403))),
                   req("Admin login", "POST", f"{G}/api/v1/users/login", auth=False,
                       body={"email": "{{adminEmail}}", "password": "{{adminPassword}}"},
                       events=pre("pm.environment.unset('adminAccessToken');",
                                  "if (!pm.environment.get('adminEmail')) { pm.execution.skipRequest(); }")
                       + tests(status(200), "pm.environment.set('adminAccessToken', pm.response.json().accessToken);")),
                   req("ADMIN reads the user's account", "GET", f"{A}/accounts/{{{{platformAccountId}}}}", auth=False,
                       headers=ADMIN_AUTH, events=ADMIN_SKIP + tests(status(200))),
                   req("ADMIN lists the user's accounts", "GET", f"{A}/accounts/user/{{{{userId}}}}", auth=False,
                       headers=ADMIN_AUTH, events=ADMIN_SKIP + tests(status(200), "const ids = pm.response.json().map(a => a.id);",
                                                   "pm.test('Includes the platform and tracking accounts', () => pm.expect(ids).to.include.members([pm.environment.get('platformAccountId'), pm.environment.get('trackingAccountId')]));")),
                   req("ADMIN posts dividend", "POST", f"{A}/accounts/{{{{platformAccountId}}}}/dividends", auth=False,
                       headers=ADMIN_AUTH, body={"symbol": "AAPL", "dividendPerShare": 0.5, "paymentDate": "2026-10-10"},
                       events=ADMIN_SKIP + tests(status(201), "pm.test('3 x 0.50 gross', () => pm.expect(pm.response.json().grossAmount).to.eql(1.5));")),
                   req("ADMIN posts interest", "POST", f"{A}/accounts/{{{{platformAccountId}}}}/interest", auth=False,
                       headers=ADMIN_AUTH, body={"annualRate": 4.5, "periodStart": "2026-09-01", "periodEnd": "2026-09-30"},
                       events=ADMIN_SKIP + tests(status(201))),
                   req("USER sees the dividend", "GET", f"{A}/dividends/account/{{{{platformAccountId}}}}",
                       events=ADMIN_SKIP + tests(status(200), "pm.test('One dividend', () => pm.expect(pm.response.json().length).to.eql(1));")),
                   req("USER sees the interest", "GET", f"{A}/interest/account/{{{{platformAccountId}}}}",
                       events=ADMIN_SKIP + tests(status(200), "pm.test('One interest posting', () => pm.expect(pm.response.json().length).to.eql(1));")),
                   req("ADMIN cannot deposit into the user's account -> 403", "POST", f"{A}/accounts/{{{{platformAccountId}}}}/deposit", auth=False,
                       headers=ADMIN_AUTH, body={"amount": 1.00}, events=ADMIN_SKIP + tests(status(403))),
               ]),

        folder("10. Direct service calls (no gateway)",
               "Each service on its own port, sending X-User-Id yourself. For debugging one service in isolation; "
               "these ports must never be exposed publicly.", [
                   req("user-service: me", "GET", "{{userServiceUrl}}/api/v1/users/me", auth=False,
                       headers=[("X-User-Id", "{{userId}}")], events=tests(status(200))),
                   req("user-service: deduct funds (internal)", "POST",
                       "{{userServiceUrl}}/api/v1/users/{{userId}}/funds/deduct?amount=1", auth=False,
                       events=tests(status(200))),
                   req("user-service: credit funds (internal)", "POST",
                       "{{userServiceUrl}}/api/v1/users/{{userId}}/funds/credit?amount=1", auth=False,
                       events=tests(status(200))),
                   req("market-data-service: stock", "GET",
                       "{{marketDataServiceUrl}}/api/v1/market-data/stocks/{{symbol}}", auth=False,
                       events=tests(status(200))),
                   req("order-service: my orders", "GET", "{{orderServiceUrl}}/api/v1/orders/my-orders", auth=False,
                       headers=[("X-User-Id", "{{userId}}")], events=tests(status(200))),
                   req("portfolio-service: portfolio by userId (internal)", "GET",
                       "{{portfolioServiceUrl}}/api/v1/portfolio/{{userId}}", auth=False, events=tests(status(200))),
                   req("notification-service: my notifications", "GET",
                       "{{notificationServiceUrl}}/api/v1/notifications", auth=False,
                       headers=[("X-User-Id", "{{userId}}")], events=tests(status(200))),
                   req("account-service: platform account", "GET",
                       "{{accountServiceUrl}}/api/v1/accounts/{{platformAccountId}}", auth=False,
                       headers=[("X-User-Id", "{{userId}}")], events=tests(status(200))),
                   req("account-service: Swagger OpenAPI", "GET", "{{accountServiceUrl}}/v3/api-docs", auth=False,
                       events=tests(status(200), "pm.test('Documents the API', () => pm.expect(Object.keys(pm.response.json().paths).length).to.be.above(30));")),
               ]),
    ],
}

env_values = [("gatewayUrl", "http://localhost:8080"), ("userServiceUrl", "http://localhost:8081"),
              ("marketDataServiceUrl", "http://localhost:8082"), ("orderServiceUrl", "http://localhost:8083"),
              ("portfolioServiceUrl", "http://localhost:8084"), ("notificationServiceUrl", "http://localhost:8085"),
              ("accountServiceUrl", "http://localhost:8086"),
              ("symbol", "AAPL"), ("email", ""), ("password", "Secret123"),
              ("accessToken", ""), ("refreshToken", ""), ("userId", ""), ("orderId", ""), ("lastPrice", ""),
              ("adminEmail", ""), ("adminPassword", ""), ("adminAccessToken", ""),
              ("platformAccountId", ""), ("platformTradeId", ""), ("trackingAccountId", ""), ("statementId", ""),
              ("manualTradeId", ""), ("today", ""), ("spareAccountId", ""),
              ("growwAccountId", ""), ("binanceAccountId", ""), ("exnessAccountId", "")]
env = {"id": "3c9a1e57-2d4b-4f86-b0c1-7e5a9d2f4b13", "name": "Stock Market - Local",
       "values": [{"key": k, "value": v,
                   "type": "secret" if k in ("password", "accessToken", "refreshToken", "adminPassword", "adminAccessToken") else "default",
                   "enabled": True} for k, v in env_values],
       "_postman_variable_scope": "environment"}

with open(f"{out}/stock-market-system.postman_collection.json", "w", encoding="utf-8") as f:
    json.dump(collection, f, indent=2)
with open(f"{out}/stock-market-local.postman_environment.json", "w", encoding="utf-8") as f:
    json.dump(env, f, indent=2)
print("requests:", sum(len(fo["item"]) for fo in collection["item"]))
