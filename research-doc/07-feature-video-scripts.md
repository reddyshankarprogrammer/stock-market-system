# 7. Landing page feature videos: 8 shorts

**Question:** Write 8 shorts (6–8 seconds, no voice-over) for the Quantiva landing page: P&L calendar, statement import, journal and tags, trade replay, AI coach, portfolio live prices, alerts, strategy backtest. For each: on-screen text (max 6 words), the exact UI actions in order, camera movement, and the end frame, written so a Remotion or Playwright recording can reproduce it.

Source prompt: A3 in [06-ai-prompt-library.md](06-ai-prompt-library.md). Where the clips are used: landing page storyboard in [04-product-technology-roadmap.md](04-product-technology-roadmap.md) section 2 and [roadmap.html](roadmap.html); schedule in [05-daily-implementation-plan.md](05-daily-implementation-plan.md) unit 24 (hero clip and two feature clips). Rules applied from the landing page review (mockup `Landing page-html/Main.dc.html`): brand colours only, no colour-only gain/loss, sample-data labels, no "would have saved" claims.

## Shared spec

| Item | Value |
|---|---|
| Canvas | 1280×720, 30 fps, no audio. Export WebM (AV1) and MP4, plus a poster PNG of the end frame. |
| Colours | Navy `#0B1F3A` background, Graphite `#1E293B` panels, White text, Electric Blue `#146EF5` for actions, Growth Green `#00C878` for gains. See [branding/QUANTIVA_BRAND_IDENTITY.md](branding/QUANTIVA_BRAND_IDENTITY.md). |
| Losses | Silver `#DCE3EA` text on a faint silver tint with a **−** sign. No red or orange. |
| Fonts | Manrope for captions and headings, Inter for UI, tabular figures for numbers. |
| Caption | One line, max 6 words, bottom-left. Fades in at 0.5 s, out 0.5 s before the end. |
| Fixed badge | Top-right, always visible: "Sample data" (clips 6–8 use the badge text given in the clip). Not counted in the 6 words. |
| Cursor | White dot with a navy ring. Easing `cubic-bezier(.22,1,.36,1)`. Click = 120 ms ring pulse. |
| Loop | The last 0.5 s holds the end frame. The end frame equals the poster, so reduced-motion users see it as a still. |
| Rules | No broker logos (names as plain text only). No "would have saved" claims. Sample account "Sample account", October 2026. |

**Remotion:** one `<Composition>` per clip. Drive everything from `useCurrentFrame()`, `interpolate()` and `spring()`, with no wall-clock timers. Seed the data in one `sample.ts`.
**Playwright:** viewport 1280×720, `recordVideo`, `page.clock.install()` for fixed time, and the UI actions below as `page.mouse.move({steps})` and clicks. The `data-testid` names are proposals.

## 1. P&L calendar (8 s, 240 frames)

**Caption:** "Every trading day at a glance" (5)

1. 0.0–0.6 s: dashboard, October 2026 calendar grid already drawn, all cells empty.
2. 0.6–2.4 s: cells fill week by week, left to right, 40 ms apart. Green tint strength follows the day's P&L. Loss days show a silver tint and "−₹820".
3. 2.4–3.4 s: the "Week" totals column counts up.
4. 3.8 s: cursor glides to the 9 Oct cell (`data-testid=cal-2026-10-09`), pulses a click.
5. 4.2–5.6 s: a side drawer slides in from the right with 3 trade rows (symbol, side, P&L).
6. 5.6–7.0 s: cursor moves to "Filter: tag" and picks "Opening range breakout"; non-matching cells dim to 30%.

**Camera:** slow push-in 1.00→1.05 on the grid; at 4.2 s pan 80 px left to make room for the drawer.
**End frame:** calendar dimmed to one tag, drawer open on 9 Oct, caption visible.

## 2. Statement import (8 s)

**Caption:** "Drop a statement. Trades appear." (5)

1. 0.0–0.8 s: empty "Import statement" drop zone, dashed border.
2. 0.8–2.0 s: file chip `tradebook_oct2026.xlsx` drags in from the left and drops. Border turns blue.
3. 2.0–3.0 s: progress bar fills; text "Reading 212 trades".
4. 3.0–5.2 s: table rows (date, symbol, side, qty, P&L) appear one every 350 ms, five rows; a "Charges matched" tag ticks on.
5. 5.2–6.6 s: plain-text row under the table: "Groww · Zerodha · Binance · Exness". Each name highlights in turn (350 ms each).
6. 6.6–7.0 s: "Import 212 trades" button gets a click pulse.

**Camera:** static at the start, then a 6% push-in toward the table at 3.0 s.
**End frame:** table with five rows, four broker names visible, button pressed.

## 3. Journal and tags (8 s)

**Caption:** "Tag the setup. Note the mistake." (6)

1. 0.0–0.8 s: trade detail "INFY · Long", P&L +₹2,880 and 2.1R.
2. 0.8–2.2 s: cursor clicks "Add tag"; a menu opens; clicks "Opening range breakout". A chip appears in green tint.
3. 2.2–3.2 s: second tag "Followed plan" is added. Third tag "Mistake: late exit" is added as a silver chip with a "!" icon (not colour only).
4. 3.2–5.4 s: the note box is clicked; text types at 35 ms per character: "Waited for the 15-min high. Sold late."
5. 5.4–6.8 s: a screenshot thumbnail slides into an "Attachments" strip. The tag row gets a small "3 tags" count.

**Camera:** start wide, push to the tag row at 0.8 s, then drift down to the note box at 3.2 s (smooth vertical pan, 160 px).
**End frame:** three tags, the typed note and one attachment visible.

## 4. Trade replay (8 s)

**Caption:** "Replay any trade candle by candle" (6)

1. 0.0–0.6 s: candlestick chart for INFY, 5 min, 21 Oct 2026. Five candles shown.
2. 0.6–5.0 s: candles are added at 4× replay speed, one every 180 ms, 22 in total. Up candles green; down candles silver outline with a hollow body (not colour only).
3. 1.6 s: marker "Buy 120" pops on candle 7 with a vertical entry line.
4. 3.4 s: marker "Sell 120" pops on candle 17. A shaded band between the two markers shows hold time.
5. 5.0–6.4 s: cursor clicks the speed control, switching 4× to 8×. Final caption fades in.
6. 6.4–7.0 s: P&L chip "+₹2,880" lands in the corner.

**Camera:** steady wide shot; at 1.6 s a gentle zoom to 1.08 centred on the entry marker, easing back to 1.0 at 5.0 s.
**End frame:** full 22-candle chart with both markers, shaded hold band, P&L chip.

## 5. AI coach (8 s)

**Caption:** "Ask your own trading history" (5)

1. 0.0–0.6 s: chat panel, empty, label "Example, not live data".
2. 0.6–2.0 s: question types in at 40 ms per character: "At what time do I trade best?"
3. 2.0–2.6 s: three-dot "thinking" indicator.
4. 2.6–4.2 s: answer streams in: "Your win rate peaks 10:00–11:30 in this sample."
5. 4.2–6.0 s: a bar chart grows, 7 hourly bars. Bars at or above 50% are green; bars below 50% are silver with the value printed above ("33%").
6. 6.0–7.0 s: citation chip "Based on 212 sample trades" appears under the answer; below it a button "Add rule to my plan" gets a hover.

**Camera:** push-in 1.00→1.06 toward the chart as it grows; slight upward drift at the end.
**End frame:** question, answer, labelled chart, citation chip, hovered button. Footer line: "Educational, not investment advice."

## 6. Portfolio and live prices (6 s)

**Caption:** "Watch positions update as prices move" (6)
**Badge:** "Sample prices"

1. 0.0–0.5 s: holdings table, 5 rows (symbol, qty, price, day change).
2. 0.5–4.5 s: prices tick every 600 ms on random rows. The changed cell flashes a blue underline for 300 ms; day change shows ▲ in green or ▼ with "−" in silver. The portfolio total in the header recounts each tick.
3. 4.5–5.2 s: a donut chart on the right grows to show allocation (Equities, Index, Cash).
4. 5.2–5.5 s: cursor hovers a donut slice; tooltip "Equities 52%".

**Camera:** static; slow 3% push-in toward the total.
**End frame:** table, total, donut with tooltip.
**Note:** market data is simulated today (root `CLAUDE.md` section 7), so the "Sample prices" badge is required.

## 7. Alerts (6 s)

**Caption:** "Know when you break rules" (5)
**Badge:** "Concept: coming soon"

1. 0.0–0.6 s: risk plan card: "Daily loss limit ₹5,000" bar at 40%, "Trades today 3 of 6".
2. 0.6–2.2 s: a new trade row appears; the loss bar animates to 100%.
3. 2.2–2.6 s: the bar's border switches to white and the card shakes 4 px (no flashing faster than 3 times a second).
4. 2.6–4.6 s: a phone-frame notification slides in from the top right: "Daily loss limit reached".
5. 4.6–5.4 s: the notification gets a tap pulse; a banner on the card reads "Take a break. Plan says stop."

**Camera:** start on the card, pan right 120 px to reveal the phone notification at 2.6 s.
**End frame:** full bar, banner, phone notification.
**Note:** planned feature (phase 7 in [04](04-product-technology-roadmap.md)), so the "Concept" badge stays.

## 8. Strategy backtest (8 s)

**Caption:** "Test an idea on past data" (6)
**Badge:** "Concept · Hypothetical results"

1. 0.0–0.8 s: strategy builder with 3 rule rows: "Price crosses above 20-day average", "Volume above average", "Exit at 2R".
2. 0.8–2.2 s: cursor changes the date range to "Last 3 years"; clicks "Run backtest" (blue button, click pulse).
3. 2.2–3.4 s: progress bar and text "Replaying 756 days".
4. 3.4–6.2 s: equity curve draws left to right in green (strokeDashoffset); drawdown shade appears under it in silver.
5. 6.2–7.2 s: stat tiles count up: Win rate, Profit factor, Max drawdown, Trades.
6. 7.2–7.5 s: footer line fades in: "Hypothetical. Past results do not predict future results."

**Camera:** wide shot at the start; at 3.4 s push to the curve (1.00→1.07); ease back to 1.0 for the stats.
**End frame:** curve, four stat tiles, the disclaimer line, caption.

## Reproduction notes

- **One data file** (`sample.ts`) feeds all eight clips so numbers match across the page.
- **Remotion:** build shared pieces once (`Cursor`, `Caption`, `SampleBadge`, `Candle`, `StatTile`); each clip is a sequence of `<Sequence from durationInFrames>` blocks with frame numbers = seconds × 30.
- **Playwright:** record the real `web-app` screens once they exist, with seeded data, then add captions and the badge in Remotion.
- **Accessibility:** each `<video>` is muted, looping, `playsinline`, with a poster, a short text alternative and a visible pause control. Reduced motion shows the poster only.
- **Truth labels:** everything is sample data. Clips 7 and 8 are explicit concepts. Clips 1–5 show features that are still being built (the coach is planned for phase 6); check clip 2's sample file against a real statement format before publishing.
