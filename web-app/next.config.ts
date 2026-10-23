import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Self-contained server (server.js plus only the modules it needs) for the container image.
  output: "standalone",
  // Shown on Settings > About. npm sets npm_package_version for `npm run build`, also inside the Docker build.
  env: { NEXT_PUBLIC_APP_VERSION: process.env.npm_package_version ?? "dev" },
  cacheComponents: true,
  partialPrefetching: true,
  // The emailed reset link carries a secret token: never send it in a Referer header and never cache the page.
  async headers() {
    return [
      {
        source: "/reset-password",
        headers: [
          { key: "Referrer-Policy", value: "no-referrer" },
          { key: "Cache-Control", value: "no-store" },
        ],
      },
    ];
  },
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
