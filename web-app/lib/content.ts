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
  getStarted: { label: "Get Started", href: "/signup" },
  trial: { label: "Start Free Trial", href: "/signup" },
};

export const hero = {
  eyebrow: "AI-Powered Trading Intelligence Platform",
  title: "The Intelligent Trading Operating System",
  subtitle:
    "Transform Market Data Into Alpha With Artificial Intelligence, Quantitative Analytics, Portfolio Intelligence, and Advanced Trading Automation.",
  primaryCta: { label: "Start Free Trial", href: "/signup" },
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
    { label: "Per-user data ownership checks on every request", status: "Live" },
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
