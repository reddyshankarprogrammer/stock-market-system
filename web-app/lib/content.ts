// All landing page copy lives here. Components only render it.
// Rule: state only facts that are true today; use status "Live", "In progress" or "Planned" honestly.

export const site = {
  name: "Quantiva",
  url: "https://quantiva.ai",
  tagline: "Trade With Intelligence.",
  category: "AI-Powered Trading Intelligence Platform",
  title: "Quantiva | AI Trading Platform and Trading Journal",
  description:
    "Quantiva is an AI-powered trading intelligence platform: trading journal, portfolio analytics, backtesting and an AI trading coach in one place.",
  keywords: [
    "AI Trading Platform",
    "Trading Journal",
    "Portfolio Analytics",
    "Algorithmic Trading",
    "TradingView Alternative",
    "Backtesting Software",
    "Trading Intelligence Platform",
    "Quantitative Research",
    "AI Trading Coach",
    "Trading Automation",
  ],
};

export const nav = {
  links: [
    { label: "Features", href: "#platform" },
    { label: "Solutions", href: "#solutions" },
    { label: "Pricing", href: "#pricing" },
    { label: "Research", href: "#ai" },
    { label: "Resources", href: "#faq" },
    { label: "Enterprise", href: "#security" },
  ],
  login: { label: "Login", href: "/login" },
  getStarted: { label: "Get Started", href: "/register" },
  trial: { label: "Start Free Trial", href: "/register" },
};

export const hero = {
  eyebrow: "AI-Powered Trading Intelligence Platform",
  title: "The Intelligent Trading Operating System",
  subtitle:
    "Transform Market Data Into Alpha With Artificial Intelligence, Quantitative Analytics, Portfolio Intelligence, and Advanced Trading Automation.",
  primaryCta: { label: "Start Free Trial", href: "/register" },
  secondaryCta: { label: "Watch Demo", href: "#platform" },
  sampleNote: "Sample data for illustration",
  // Illustrative figures for the mock dashboard only; labelled as sample data on the page.
  metrics: [
    { label: "Win rate", value: "58.4%", delta: 2.1, lowerIsBetter: false },
    { label: "Profit factor", value: "1.62", delta: 0.08 },
    { label: "Expectancy", value: "0.32R", delta: 0.04 },
    { label: "Max drawdown", value: "4.1%", delta: -0.6, lowerIsBetter: true },
  ],
  coach: {
    question: "Why was March weaker than February?",
    answer:
      "7 of your 9 losing trades were opened in the first 30 minutes after the open (trades 212, 218, 224, 231, 236, 240, 247).",
    label: "Example, not live data",
  },
  allocation: [
    { name: "Equities", pct: 52 },
    { name: "Index", pct: 28 },
    { name: "Crypto", pct: 12 },
    { name: "Cash", pct: 8 },
  ],
};

export const trust = {
  heading: "Built for every kind of trader and team",
  audiences: [
    "Retail Traders",
    "Professional Traders",
    "Portfolio Managers",
    "Hedge Funds",
    "Financial Advisors",
    "Institutions",
  ],
  indicatorsHeading: "Security you can check, status shown honestly",
  // Status must match reality. Update when a control ships or is audited.
  indicators: [
    { label: "Role-based access (user and admin)", status: "Live" },
    { label: "Per-user data ownership checks across all services", status: "In progress" },
    { label: "Encryption in transit (TLS at the edge)", status: "Live" },
    { label: "Audit logging", status: "In progress" },
    { label: "Independent penetration test", status: "Planned" },
    { label: "SOC 2 readiness", status: "Planned" },
  ] as { label: string; status: "Live" | "In progress" | "Planned" }[],
  note: "No security certification has been awarded yet. We will list one only after an independent audit.",
};

export const footer = {
  columns: [
    {
      title: "Product",
      links: [
        { label: "Features", href: "#platform" },
        { label: "AI Trading Coach", href: "#ai" },
        { label: "Pricing", href: "#pricing" },
      ],
    },
    {
      title: "Company",
      links: [
        { label: "Security", href: "#security" },
        { label: "FAQ", href: "#faq" },
        { label: "Login", href: "/login" },
      ],
    },
    {
      title: "Legal",
      links: [
        { label: "Terms & Conditions", href: "/legal/terms" },
        { label: "Privacy Policy", href: "/legal/privacy" },
        { label: "Risk Disclosure", href: "/legal/risk" },
      ],
    },
  ],
  risk:
    "Trading involves risk, including the loss of capital. Quantiva provides analytics and educational information, not investment advice, and does not recommend buying or selling any security. Past performance does not guarantee future results. Figures shown on this page are sample data.",
  copyright: `© ${new Date().getFullYear()} Quantiva. Quantiva is a trademark.`,
};

// ---------------------------------------------------------------------------------------------
// Stage 2 sections. Quantiva is pre-launch: every feature states where it really stands.
// "In development" = being built now. "Planned" = on the roadmap, not started. Update as things ship.
// ---------------------------------------------------------------------------------------------

export type FeatureStatus = "In development" | "Planned";
export type Art = "bars" | "line" | "nodes" | "chat" | "table";

export const showcase = {
  eyebrow: "See it working",
  title: "Watch a month of trades become a clear next step",
  intro: "Short clips of the interface we are building, using sample data. Nothing here is live yet.",
  note: "Sample data. The features shown are in development.",
  items: [
    {
      name: "calendar",
      title: "See every trading day at a glance",
      body: "Profit and loss by day with weekly totals. Open a day to see its trades, and filter by tag.",
      points: [
        "Colour strength shows the size of the day, and every loss carries a minus sign",
        "Weekly totals in the last column",
        "Filter by tag, symbol or account (planned)",
      ],
      label: "Clip, 8 seconds: a profit and loss calendar fills in, a day opens to show its trades, and the calendar is filtered by tag. Sample data.",
    },
    {
      name: "import",
      title: "Drop a statement, get a clean journal",
      body: "Upload a broker statement and watch the trades appear, with charges matched to them.",
      points: [
        "Statement formats for Groww, Zerodha, Binance and Exness are supported in our backend",
        "Charges matched to the right trades",
        "The import screen itself is in development",
      ],
      label: "Clip, 8 seconds: a statement file is dropped, trades are read and listed, and the four supported broker formats are shown. Sample data.",
    },
  ],
};

export const platform = {
  eyebrow: "Platform",
  title: "Everything a trader needs, in one platform",
  intro:
    "Quantiva is pre-launch. Each module below shows whether it is being built now or planned, so you can see where we really are.",
  moreHeading: "What it is designed to do",
  blocks: [
    {
      slug: "journal",
      title: "Journal",
      status: "In development" as FeatureStatus,
      art: "bars" as Art,
      benefit: "Record why you took every trade and what happened.",
      more: ["Notes, setup tags and mistake tags on every trade", "Screenshots and a daily journal", "Reports grouped by your own tags"],
    },
    {
      slug: "charts",
      title: "Charts",
      status: "Planned" as FeatureStatus,
      art: "line" as Art,
      benefit: "TradingView-style candlestick charts with your entries and exits marked.",
      more: ["Candles from one minute to daily", "Trade replay, candle by candle", "Entry and exit markers on the chart"],
    },
    {
      slug: "strategy-lab",
      title: "Strategy Lab",
      status: "Planned" as FeatureStatus,
      art: "nodes" as Art,
      benefit: "Write entry and exit rules and keep them in a plan you can check.",
      more: ["Rule builder for entries, exits and sizing", "Daily loss and trade-count limits", "Plan adherence score"],
    },
    {
      slug: "backtesting",
      title: "Backtesting",
      status: "Planned" as FeatureStatus,
      art: "line" as Art,
      benefit: "Replay your rules on past data. Results are always hypothetical.",
      more: ["Choose a date range and run a test", "Equity curve, drawdown and trade statistics", "Every result labelled hypothetical"],
    },
    {
      slug: "ai-coach",
      title: "AI Coach",
      status: "Planned" as FeatureStatus,
      art: "chat" as Art,
      benefit: "Ask questions about your own trades and get answers with the numbers.",
      more: ["Answers cite the trades they are based on", "Suggests rules for your plan", "Educational, never buy or sell advice"],
    },
    {
      slug: "portfolio",
      title: "Portfolio",
      status: "In development" as FeatureStatus,
      art: "bars" as Art,
      benefit: "Holdings, allocation and performance across your accounts.",
      more: ["Multiple broker accounts in one view", "Allocation and performance breakdowns", "Charges, taxes and dividends matched to trades"],
    },
    {
      slug: "market-intelligence",
      title: "Market Intelligence",
      status: "Planned" as FeatureStatus,
      art: "nodes" as Art,
      benefit: "News and sentiment next to the symbols you trade.",
      more: ["News feed for your watched symbols", "Sentiment summaries", "Needs a licensed news data source first"],
    },
    {
      slug: "broker-integrations",
      title: "Broker Integrations",
      status: "In development" as FeatureStatus,
      art: "table" as Art,
      benefit: "Import statements from Groww, Zerodha, Binance and Exness.",
      more: [
        "Statement formats for these four brokers are supported in our backend; the import screen is being built",
        "Generic CSV import with AI column mapping is planned",
        "Broker names are trademarks of their owners; no affiliation is implied",
      ],
    },
  ],
};

export const aiDemo = {
  eyebrow: "AI Trading Coach",
  title: "Ask questions about your own trading history",
  intro:
    "The coach is designed to read your trades, notes and tags, then answer with numbers from your own history and cite the trades behind each answer.",
  points: [
    "Answers cite the trades they are based on",
    "Says so when there is not enough data",
    "Never predicts prices or says buy, sell or hold",
  ],
  label: "Example conversation · scripted · sample data, not live",
  disclaimer: "Educational analysis, not investment advice.",
  cite: "Based on 212 sample trades",
  questions: [
    {
      q: "At what time do I trade best?",
      a: "In this sample, your win rate peaks between 10:00 and 11:30 and falls after 14:30.",
      unit: "win rate",
      bars: [
        { label: "9:15", value: 38, text: "38%" },
        { label: "10:00", value: 64, text: "64%" },
        { label: "11:00", value: 68, text: "68%" },
        { label: "12:00", value: 55, text: "55%" },
        { label: "13:00", value: 51, text: "51%" },
        { label: "14:30", value: 33, text: "33%" },
        { label: "15:00", value: 29, text: "29%" },
      ],
      goodAt: 50,
    },
    {
      q: "Which mistakes repeat most?",
      a: "Late exits were tagged on 14 of the 212 sample trades, more than any other mistake tag.",
      unit: "trades tagged",
      bars: [
        { label: "Late exit", value: 14, text: "14" },
        { label: "Oversized", value: 9, text: "9" },
        { label: "Off-plan entry", value: 7, text: "7" },
        { label: "Revenge trade", value: 5, text: "5" },
      ],
      goodAt: Infinity,
    },
    {
      q: "How did October compare with September?",
      a: "In this sample, October net P&L was +₹18,640 against +₹11,200, and win rate was 59% against 54%.",
      unit: "win rate",
      bars: [
        { label: "Sep", value: 54, text: "54%" },
        { label: "Oct", value: 59, text: "59%" },
      ],
      goodAt: 55,
    },
  ],
};

export const solutions = {
  eyebrow: "Solutions",
  title: "One platform, shaped for how you trade",
  intro: "Pick the description closest to you. Items marked planned are on the roadmap, not available yet.",
  tabs: [
    {
      id: "retail",
      label: "Retail",
      heading: "For traders who want to understand their own results",
      body: "Start with a clean record of what you did, then add structure at your own pace.",
      points: ["Import broker statements instead of typing trades", "A calendar of winning and losing days", "Tag setups and mistakes (planned)"],
    },
    {
      id: "professional",
      label: "Professional",
      heading: "For full-time traders who need a repeatable process",
      body: "Measure what works by symbol, time of day and setup, and hold yourself to a plan.",
      points: ["Reports by symbol, hour, weekday and tag (planned)", "Risk plan with daily limits (planned)", "Trade replay on the chart (planned)"],
    },
    {
      id: "funds",
      label: "Funds and advisors",
      heading: "For portfolio managers and advisors",
      body: "See several accounts together and export what you need to share.",
      points: ["Portfolio analytics across accounts", "Exports in CSV, Excel, PDF and JSON (planned)", "Team access with roles (planned)"],
    },
    {
      id: "institutions",
      label: "Institutions",
      heading: "For institutions with security and audit requirements",
      body: "The Enterprise plan is designed around the controls large organisations ask for.",
      points: [
        "Single sign-on and team workspaces (planned)",
        "Audit logging (in progress)",
        "Security documentation for vendor review is being prepared",
      ],
    },
  ],
};

export type Plan = { name: string; audience: string; points: string[]; cta: { label: string; href: string } };

export const pricing = {
  eyebrow: "Pricing",
  title: "Five plans, from free to enterprise",
  intro: "Prices and limits are not decided yet and are shown as TBC. The contents below are our current proposal.",
  priceText: "TBC",
  plans: [
    { name: "Free", audience: "New traders", points: ["One broker account", "Journal and P&L calendar", "Limited history"], cta: { label: "Start Free Trial", href: "/register" } },
    { name: "Pro", audience: "Active traders", points: ["Unlimited accounts and history", "Full journal and reports", "Trade replay"], cta: { label: "Start Free Trial", href: "/register" } },
    { name: "AI Pro", audience: "Traders who want coaching", points: ["Everything in Pro", "AI trading coach", "Auto-tagging and CSV mapping"], cta: { label: "Start Free Trial", href: "/register" } },
    { name: "Quant Pro", audience: "Systematic traders", points: ["Everything in AI Pro", "Backtesting and price forecasts", "API access"], cta: { label: "Start Free Trial", href: "/register" } },
    { name: "Enterprise", audience: "Teams, prop firms, institutions", points: ["Everything in Quant Pro", "Team workspaces and single sign-on", "Audit export and support SLA"], cta: { label: "Contact sales", href: "/contact" } },
  ] as Plan[],
  // true = included, false = not included, string = limit or note
  rows: [
    { feature: "Broker accounts", values: ["1", "Unlimited", "Unlimited", "Unlimited", "Unlimited"] },
    { feature: "History", values: ["Limited", "Unlimited", "Unlimited", "Unlimited", "Unlimited"] },
    { feature: "Statement import", values: ["Limited", true, true, true, true] },
    { feature: "Journal, tags and P&L calendar", values: [true, true, true, true, true] },
    { feature: "Reports and trade replay", values: [false, true, true, true, true] },
    { feature: "AI trading coach", values: [false, false, true, true, true] },
    { feature: "Auto-tagging and CSV mapping", values: [false, false, true, true, true] },
    { feature: "Backtesting", values: [false, false, false, true, true] },
    { feature: "Price forecasts", values: [false, false, false, true, true] },
    { feature: "API access", values: [false, false, false, true, true] },
    { feature: "Team workspaces", values: [false, false, false, false, true] },
    { feature: "Single sign-on", values: [false, false, false, false, true] },
    { feature: "Audit log export", values: [false, false, false, false, true] },
    { feature: "Support SLA", values: [false, false, false, false, true] },
  ] as { feature: string; values: (boolean | string)[] }[],
};

export const security = {
  eyebrow: "Security",
  title: "How we protect your data, and where we are",
  intro: "Each control shows its real status. We will not claim a certification until an independent audit awards it.",
  controls: [
    { title: "Role-based access", text: "Users and admins have separate permissions, and admin functions are kept apart.", status: "Live" },
    { title: "Encryption in transit", text: "Traffic between your browser and Quantiva is encrypted with TLS.", status: "Live" },
    { title: "Data scoped to you", text: "Account and trade data is scoped to the signed-in user. We are extending the same checks to every service.", status: "In progress" },
    { title: "Audit logging", text: "A tamper-evident record of who changed what, for users and admins.", status: "In progress" },
    { title: "Passkeys and two-step sign-in", text: "Fingerprint, face or device PIN sign-in, and authenticator codes for sensitive actions.", status: "Planned" },
    { title: "AI data handling", text: "When the AI coach launches, only the data needed to answer a question will go to our AI provider, and the privacy policy will say so.", status: "Planned" },
    { title: "Independent testing and certification", text: "A third-party penetration test and SOC 2 readiness. No certification has been awarded yet.", status: "Planned" },
  ] as { title: string; text: string; status: "Live" | "In progress" | "Planned" }[],
};

export const faq = {
  title: "Questions about importing, AI coaching and data safety",
  items: [
    { q: "Is Quantiva available now?", a: "Quantiva is in development. The sign-up page is a placeholder today, and each feature on this page shows whether it is in development or planned." },
    { q: "Which brokers can I import from?", a: "Statement formats for Groww, Zerodha, Binance and Exness are supported in our backend, and the import screen is being built. A generic CSV import with AI column mapping is planned. Broker names are trademarks of their owners; no affiliation is implied." },
    { q: "Does the AI coach tell me what to buy or sell?", a: "No. It is designed to explain patterns in your own past trades and suggest rules for your plan. It is educational and is not investment advice." },
    { q: "What data goes to the AI?", a: "When the AI coach is released, only the trades, tags and notes needed to answer a question will be sent to our AI provider. The privacy policy will say so before launch." },
    { q: "Is my data secure?", a: "Traffic is encrypted in transit and access is role-based. Audit logging is in progress and independent testing is planned. No security certification has been awarded yet; the security section shows the status of each control." },
    { q: "Can I use Quantiva on my phone?", a: "The site and app are designed to work in a browser on phones, tablets and desktops. An installable web app and native iOS and Android apps are planned." },
    { q: "How will sign-in work?", a: "Email and password, with Google, Microsoft and GitHub sign-in planned first, then Apple and LinkedIn. Passkeys (fingerprint, face or device PIN) and authenticator codes are planned." },
    { q: "What will the plans cost?", a: "Five plans are planned: Free, Pro, AI Pro, Quant Pro and Enterprise. Prices and limits have not been decided and are shown as TBC." },
  ],
};

export const finalCta = {
  title: "Review your next 100 trades with a clearer plan.",
  primary: { label: "Start Free Trial", href: "/register" },
  secondary: { label: "See the platform", href: "#platform" },
};

export const legal = {
  terms: { title: "Terms & Conditions", body: "The Terms & Conditions are being prepared and are not yet in effect. Quantiva is not open for general use." },
  privacy: {
    title: "Privacy Policy",
    body: "The Privacy Policy is being prepared. It will describe what personal and trading data we collect, why, how long we keep it, which providers process it (including any AI provider), and how to ask for deletion.",
  },
  risk: { title: "Risk Disclosure", body: footer.risk },
} as const;
