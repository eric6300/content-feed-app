# Decisions

Architecture log. Each entry: what was considered, what was decided, and the trade-off.

<!-- Add entries below, newest last. Use `/log-decision` to append one. -->

## 2026-09-12 — Synthesized target action for service cards

**Considered:** Leaving service cards without any actionable CTA (tapping only ever opens the in-app detail view), vs. synthesizing a target action to match the brief's service-card definition (title, icon, a short blurb, and a target action).
**Decided:** DummyJSON has no native action/link field, so a target action is synthesized: a Google search URL built from the product's title (`https://www.google.com/search?q=<title>`), opened in an external browser or custom tab when the user taps the card's CTA. Tapping elsewhere on the card still opens the in-app detail view — two distinct interactions on the same card. The thumbnail doubles as the icon and a truncated description as the blurb, rather than inventing fields DummyJSON doesn't provide.
**Trade-off:** The link carries no real business meaning (not a genuine offer or redemption target), but it demonstrates handling an external-navigation intent as a distinct interaction from in-app navigation, and keeps the data model honest about what the mock source actually provides instead of fabricating unbacked fields.

## 2026-09-12 — Save/unsave is scoped to articles only

**Considered:** Making save/unsave uniform across all three card types (one code path, matches a literal reading of the brief's generic "Save/Unsave" flow) vs. restricting it to articles.
**Decided:** Only articles are saveable. Weather has no save action, since it's continuously-updating data with no fixed "version" that makes sense to bookmark. Service cards have no save action either, since they're a promotional entry whose own target action already is the intended interaction — saving one doesn't fit the "read later" mental model the feature is named for.
**Trade-off:** Narrower than a literal "any feed item can be saved" reading of the brief, but the brief itself says the source/card mix is the candidate's call to justify; a Saved list that only ever contains articles is easier to reason about and matches how the reference "Saved (Offline)" screen is actually used.

## 2026-09-12 — Service card images bundled locally, not loaded from DummyJSON's CDN

**Considered:** Keeping the 20 seeded products' `thumbnail`/`images` fields as the live DummyJSON CDN URLs (simpler to seed, no asset packaging step) vs. downloading those 20 images once and bundling them as local app assets.
**Decided:** Download and bundle the images locally, referenced from the local product JSON by local resource id/path instead of a remote URL.
**Trade-off:** A one-time asset-download/packaging step and a modest APK size increase, but this is what makes "service cards are a local mock excluded from the freshness policy, with no live network dependency" actually true end-to-end. Leaving the images on DummyJSON's CDN would have quietly reintroduced a live network dependency for this source (image loading could fail, be slow, or need a placeholder/retry path), contradicting the freshness policy's stated reason for excluding service cards.

## 2026-09-12 — Feed layout hardcoded for now

**Considered:** A JSON config to control feed layout/ordering rules (which card types appear, at what interval), inspired by a common pattern in production feed apps.
**Decided:** Keep the layout hardcoded for this submission due to time constraints.
**Trade-off:** Less flexible than a config-driven layout, but avoids building configuration infrastructure the assignment doesn't require. With more time, the natural extension is a server-driven composition: a backend endpoint that merges the sources and tells the client what card type goes in each slot, rather than the client hardcoding the interval.
