# AI Usage

Which AI tools were used, what was accepted/rejected/rewritten, and why. Include at least one example of an AI suggestion that was rejected or significantly changed.

<!-- Add entries below, newest last. Use `/log-ai-usage` to append one. -->

## 2026-09-16 — T8 visual hardening

**Tool/flow:** Impeccable context and refinement guidance, followed by a Pixel_9 emulator review.
**Accepted:** The Signal Desk direction was preserved; the service-card assets were rewritten as local, wide vector product plates, and the shared image fallback became an icon-plus-label component that adapts to thumbnail size.
**Rejected/rewrote:** An initial text-only fallback and an asset-only polish pass were not kept. The emulator screenshot showed that text was cramped inside 96 × 72 dp thumbnails, exposed unbounded article summaries that made the feed unusably tall, and revealed black unkept bookmark icons in dark mode; the implementation was rewritten to use compact icon-only fallbacks, two-/three-line title/summary limits, and an explicit semantic tint.
**Why:** Runtime screenshots, rather than source inspection alone, revealed the scanability, overflow, and contrast defects. `./gradlew build ktlintCheck`, SVG XML validation, and `git diff --check` then passed.
