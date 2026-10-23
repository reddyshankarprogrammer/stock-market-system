// Renders the clip scenes (app/clips/*) into public/video/<name>.webm plus a .jpg poster, frame by frame with Playwright.
// Each frame sets every CSS animation to an exact time, takes a screenshot and pipes it to ffmpeg, so the result is
// reproducible, starts on the first frame and is exactly DURATION seconds long (hard limit 8).
// Usage (production server running on BASE_URL, default http://localhost:3100):
//   npm run build && npx next start -p 3100     # in one terminal
//   npm run clips:record                         # in another. CLIPS=calendar to render one clip.
// Playwright installs a minimal ffmpeg next to its browsers (VP9 encoder, mjpeg decoder); no other tool is needed.
import { spawn } from "node:child_process";
import { mkdirSync, readdirSync, statSync } from "node:fs";
import path from "node:path";
import { chromium } from "playwright";

const BASE = process.env.BASE_URL ?? "http://localhost:3100";
const DURATION = 8; // seconds
const FPS = 30;
const clips = (process.env.CLIPS ?? "calendar,import").split(",");

const out = path.resolve("public/video");
mkdirSync(out, { recursive: true });

function findFfmpeg() {
  // executablePath is <ms-playwright>/chromium-N/chrome-win64/chrome.exe, so go up three levels.
  const browsers = path.dirname(path.dirname(path.dirname(chromium.executablePath())));
  const dir = readdirSync(browsers).find((d) => d.startsWith("ffmpeg-"));
  const file = dir && readdirSync(path.join(browsers, dir)).find((f) => f.startsWith("ffmpeg"));
  if (!file) throw new Error("Playwright ffmpeg not found; run: npx playwright install chromium");
  return path.join(browsers, dir, file);
}
const ffmpeg = findFfmpeg();

const browser = await chromium.launch();
try {
  for (const name of clips) {
    const started = Date.now();
    const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });
    await page.goto(`${BASE}/clips/${name}`, { waitUntil: "networkidle" });
    await page.evaluate(() => document.fonts.ready);
    await page.evaluate(() => document.getAnimations().forEach((a) => a.pause()));

    const dest = path.join(out, `${name}.webm`);
    const enc = spawn(
      ffmpeg,
      ["-y", "-loglevel", "error", "-f", "image2pipe", "-framerate", String(FPS), "-vcodec", "mjpeg", "-i", "pipe:0", "-an",
        "-c:v", "libvpx-vp9", "-b:v", "0", "-crf", "36", "-deadline", "good", "-cpu-used", "2", "-pix_fmt", "yuv420p", dest],
      { stdio: ["pipe", "inherit", "inherit"] },
    );
    const done = new Promise((resolve, reject) => {
      enc.on("close", (code) => (code === 0 ? resolve() : reject(new Error(`ffmpeg exited with ${code}`))));
      enc.on("error", reject);
    });

    const frames = DURATION * FPS;
    for (let i = 0; i < frames; i++) {
      const t = (i * 1000) / FPS;
      await page.evaluate((ms) => document.getAnimations().forEach((a) => { a.currentTime = ms; }), t);
      const shot = await page.screenshot({ type: "jpeg", quality: 88 });
      if (!enc.stdin.write(shot)) await new Promise((r) => enc.stdin.once("drain", r));
    }
    enc.stdin.end();
    await done;

    // Poster = the end frame, so reduced-motion users and slow connections see the finished scene.
    await page.evaluate((ms) => document.getAnimations().forEach((a) => { a.currentTime = ms; }), DURATION * 1000);
    await page.screenshot({ path: path.join(out, `${name}.jpg`), type: "jpeg", quality: 80 });
    await page.close();

    const kb = (f) => Math.round(statSync(f).size / 1024);
    console.log(`${name}: ${frames} frames, ${kb(dest)} KB video, ${kb(path.join(out, `${name}.jpg`))} KB poster, ${((Date.now() - started) / 1000).toFixed(1)}s`);
  }
} finally {
  await browser.close();
}
