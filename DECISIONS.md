# Decisions

Architecture log. Each entry: what was considered, what was decided, and the trade-off.

<!-- Add entries below, newest last. Use `/log-decision` to append one. -->

## 2026-09-12 — Synthesized target action for service cards

**Considered:** Leaving service cards without any actionable CTA (tapping only ever opens the in-app detail view), vs. synthesizing a target action to match the product's service-card contract (title, icon, a short blurb, and a target action).
**Decided:** DummyJSON has no native action/link field, so a target action is synthesized: a Google search URL built from the product's title (`https://www.google.com/search?q=<title>`), opened in an external browser or custom tab when the user taps the card's CTA. Tapping elsewhere on the card still opens the in-app detail view — two distinct interactions on the same card. The thumbnail doubles as the icon and a truncated description as the blurb, rather than inventing fields DummyJSON doesn't provide.
**Trade-off:** The link carries no real business meaning (not a genuine offer or redemption target), but it demonstrates handling an external-navigation intent as a distinct interaction from in-app navigation, and keeps the data model honest about what the mock source actually provides instead of fabricating unbacked fields.

## 2026-09-12 — Save/unsave is scoped to articles only

**Considered:** Making save/unsave uniform across all three card types (one code path, matching a literal reading of a generic "Save/Unsave" flow) vs. restricting it to articles.
**Decided:** Only articles are saveable. Weather has no save action, since it's continuously-updating data with no fixed "version" that makes sense to bookmark. Service cards have no save action either, since they're a promotional entry whose own target action already is the intended interaction — saving one doesn't fit the "read later" mental model the feature is named for.
**Trade-off:** Narrower than a literal "any feed item can be saved" reading, but the source/card mix is intentionally product-defined; a Saved list that only ever contains articles is easier to reason about and matches the intended offline-reading flow.

## 2026-09-12 — Service card images bundled locally, not loaded from DummyJSON's CDN

**Considered:** Keeping the 20 seeded products' `thumbnail`/`images` fields as the live DummyJSON CDN URLs (simpler to seed, no asset packaging step) vs. downloading those 20 images once and bundling them as local app assets.
**Decided:** Download and bundle the images locally, referenced from the local product JSON by local resource id/path instead of a remote URL.
**Trade-off:** A one-time asset-download/packaging step and a modest APK size increase, but this is what makes "service cards are a local mock excluded from the freshness policy, with no live network dependency" actually true end-to-end. Leaving the images on DummyJSON's CDN would have quietly reintroduced a live network dependency for this source (image loading could fail, be slow, or need a placeholder/retry path), contradicting the freshness policy's stated reason for excluding service cards.

## 2026-09-12 — Feed layout hardcoded for now

**Considered:** A JSON config to control feed layout/ordering rules (which card types appear, at what interval), inspired by a common pattern in production feed apps.
**Decided:** Keep the layout hardcoded for the first release due to time constraints.
**Trade-off:** Less flexible than a config-driven layout, but avoids building configuration infrastructure the product does not require yet. With more time, the natural extension is a server-driven composition: a backend endpoint that merges the sources and tells the client what card type goes in each slot, rather than the client hardcoding the interval.

## 2026-09-12 — Freshness/caching: key+TTL `FreshnessGate`, not an RxCache-style generic cache

**Considered:** (1) An RxCache-style generic cache — annotate/wrap each API call with a TTL, library transparently serializes and stores the response on disk, decides serve-cached-vs-refetch automatically. (2) A `FreshnessSource` enum (`WEATHER`, `ARTICLES`, ...) each carrying its own TTL constant, checked via a shared timestamp store. (3) A `FreshnessGate` keyed by a plain string + `Duration` (no enum, no annotations), storing only a last-fetched timestamp per key in DataStore Preferences, with Room remaining the sole store for the actual cached data.
**Decided:** Option 3. Only two live sources currently need TTL freshness (weather: 15 min, articles: 1 hour, per `USE_CASES.md` → Feature: Feed freshness), so a full annotation/reflection/disk-serialization framework solves a "many heterogeneous API call sites" problem that doesn't exist here — and RxCache is also RxJava-based and unmaintained, incompatible with this project's coroutines/Flow stack. Its actual mechanism would also duplicate storage: Room already has to be the source of truth for weather/article rows (offline reads, instant cached display on launch), so a second, parallel blob cache means two stores to keep in sync. The one thing Room doesn't give for free — "when was this last fetched" — is the only piece worth extracting into a shared utility. A first draft used a `FreshnessSource` enum, but that was rejected: every new source would require editing that shared enum file, coupling unrelated repositories through one shared file. Final shape: `FreshnessGate` takes a plain string key + `Duration` TTL (no shared enum to extend), paired with an optional `refreshIfStale(key, ttl, fetch, persist)` extension that collapses each repository's "check stale → refetch → persist → mark fetched" into one call.
**Trade-off:** Less "batteries included" than a generic caching library — no automatic request-signature hashing, no built-in disk serialization, no annotation processor; each repository still writes one line calling `refreshIfStale`/`isStale` with its own key and TTL. But adding a new live source later costs zero changes to shared files, and there's only one cache store (Room) instead of two.

## 2026-09-14 — Latest-compatible dependency pins and Navigation 3 deferral

**Considered:** Upgrading the AGP/Kotlin/compileSdk baseline to consume the newest stable releases, vs. retaining the committed AGP `8.7.3`, Kotlin `2.0.21`, Gradle `8.13`, compileSdk/targetSdk `35`, and minSdk `24` while selecting the newest stable dependencies that resolve and pass compilation/lint on that baseline.
**Decided:** Retain the existing build baseline and centralize latest-compatible pins in `gradle/libs.versions.toml`. The bootstrap uses KSP `2.0.21-1.0.28`, Compose BOM `2025.03.00`, Lifecycle `2.8.7`, Browser `1.8.0`, Room `2.7.2`, and the compatible versions of the network, DI, image, coroutine, and test libraries recorded in the catalog. Navigation 3 remains the planned navigation architecture, but is deferred because its stable Android artifacts require compileSdk `36` and AGP `8.9.1` or newer.
**Trade-off:** The project is not on the latest overall AndroidX/toolchain releases, and T6 must revisit the navigation boundary before adding a graph. In return, the current `develop` baseline stays buildable without an unplanned toolchain migration, and dependency choices are validated by actual Gradle resolution, Kotlin compilation, and Android Lint rather than version-number comparisons alone.

## 2026-09-14 — Hybrid feed cache with protected saved articles

**Considered:** Caching only explicitly saved articles, which minimizes disk usage but leaves the feed empty offline, vs. persisting the whole fetched feed indefinitely, which gives a stronger offline experience but allows unbounded growth.
**Decided:** Use a hybrid policy. Persist fetched article and weather content in Room so the feed is cache-first; keep freshness timestamps in DataStore; protect saved articles from cleanup and copy their images into app-internal storage for durable offline access. After a successful article refresh, prune unsaved article cache rows older than 7 days. Do not impose an arbitrary 50-item cap or add background cleanup in the first release, and do not copy ordinary feed images into permanent storage—Coil owns that cache.
**Trade-off:** Offline browsing is guaranteed for saved articles, while non-saved feed history is retained only within the seven-day cache window. The policy adds a cleanup query and saved-state boundary, but keeps the database bounded without disrupting the cache-first experience or introducing a background scheduling dependency.

## 2026-09-15 — Sticky service-card placement anchors on a sort-key snapshot, not a live article foreign key

**Considered:** (1) Keep `insertAfterArticleId` as a `ForeignKey` to the article table (the original T2 shape), accepting that the placement either cascades away or dangles once its neighboring article is pruned by the 7-day retention cleanup. (2) Exclude any article currently referenced by a placement from retention cleanup, coupling the prune query to the placement table. (3) Store the placement's anchor as a snapshot of the neighboring article's sort key (`published_at`, `id`) at assignment time — plain value columns, no `ForeignKey` — and render by comparing against that snapshot instead of requiring the referenced row to still exist.
**Decided:** Option 3. A placement now carries the sort-key values of the article it was set after, not a reference to that row. Rendering merges articles and placements by comparing sort keys, so a placement still lands in the same relative position even if its original neighbor has since been pruned.
**Trade-off:** Slightly more bookkeeping than a plain foreign key (two snapshot columns instead of one id), but retention cleanup stays entirely unaware of placements, and "a service card's position, once set, does not move" (see `USE_CASES.md`) holds even across cache pruning instead of only within a single session.

## 2026-09-15 — Undo is scoped to the Saved list, not the feed/detail save toggle

**Considered:** Applying the undo window uniformly everywhere an unsave can happen (feed, detail, Saved list), vs. limiting it to removals made from the Saved list.
**Decided:** Feed and detail save toggles are immediate — tapping the save action again re-saves the article, which is already its own undo. The undo window, with its pending-delete state and local image retention, applies only when an article is removed from the Saved list.
**Trade-off:** A literal "every unsave gets an undo" reading is narrower, but a toggle button doesn't need a second, slower undo path layered on top of itself. This also collapses the pending-delete state machine down to one surface (Saved list) instead of three, and forced two gaps to get resolved explicitly instead of being left implicit: a pending undo now must survive the app being closed and reopened (finalize on reopen if the window has already passed), and multiple pending undos must be tracked independently per article.

## 2026-09-15 — Weather has no separate detail view

**Considered:** Giving weather its own detail screen (matching the pattern used for articles and service cards) vs. showing its forecast entirely within the feed card.
**Decided:** No separate weather detail view. The feed card already shows the current conditions and forecast; tapping it does nothing further. This mirrors the existing reasoning for excluding weather from Save/unsave — it's continuously-updating data with no fixed "version," which cuts the same way against a dedicated detail screen as it does against saving.
**Trade-off:** `SPEC.md`'s core requirement is "a detail screen" (singular), not "one per card type," so this isn't a scope reduction against the stated requirement — it removes a self-imposed third detail screen that wasn't adding a distinct capability over the feed card.

## 2026-09-15 — Retention cleanup only runs from the initial open/return refresh

**Considered:** Running the 7-day unsaved-article retention cleanup after every successful article refresh (initial open/return, manual pull-to-refresh, and reconnect refresh alike), vs. restricting it to the initial open/return refresh only.
**Decided:** Retention cleanup runs only after the freshness check triggered by opening or returning to the app. A manual pull-to-refresh or a reconnect refresh still refetches and updates articles as usual, but never prunes cache rows.
**Trade-off:** A stale unsaved article can now live slightly longer than exactly 7 days in an edge case (open the app, then stay in a long session past the 7-day mark without reopening). In exchange, retention cleanup can no longer remove an article the user is currently scrolled to — the initial refresh runs before the user has scrolled into any of that session's content, so pruning at that point never collides with what's on screen. Manual/reconnect refreshes, which can happen deep in an active scroll session, are excluded specifically to avoid that collision.

## 2026-09-15 — Trimmed defensive per-field scenarios for service cards and weather

**Considered:** Keeping the full set of per-field defensive scenarios for service cards (missing thumbnail, price, blurb, title) and weather (short forecast arrays, null field values) that were written as reserve handling for conditions never actually observed on either source, vs. trimming them down to only structurally-motivated cases.
**Decided:** Cut the per-field service-card scenarios to one: a malformed entry in the bundled pool JSON is skipped at load instead of crashing the app. Cut the weather scenarios to one: an unrecognized WMO weather code falls back to a neutral icon/label. Both remaining cases are structural (a hand-edited local file can be malformed; Open-Meteo's WMO code table can grow beyond this app's mapping) rather than defensive coding against a source that has never actually produced the bad value.
**Trade-off:** Less exhaustive-looking defensive coverage on paper, but the cut scenarios were guarding against inputs the app itself controls (a local JSON file it authors) or that were explicitly documented as never observed — writing tests for those reads as padding rather than resilience. `USE_CASES.md` → Feature: Per-source data handling still calls this out explicitly so the narrower scope reads as a decision, not an oversight.

## 2026-09-15 — Saved list orders by most-recently-saved

**Considered:** Ordering the Saved list by `savedAtEpochMillis` descending (most recently saved first) vs. by the article's own `published_at` (matching feed order).
**Decided:** Most-recently-saved first. `savedAtEpochMillis` already exists on the article entity for the undo/retention logic, so this needs no new column, and it matches the "read later" mental model — what you bookmarked recently is what you're most likely looking for.
**Trade-off:** A user who saves an old article won't see it grouped near other old content the way feed order would, but the Saved list isn't meant to mirror feed chronology — it's a personal queue, and recency-of-action is the more useful axis there.

## 2026-09-15 — Feed placement schema generalized beyond service cards up front

**Considered:** Shipping `FeedPlacementEntity` scoped to exactly what T2 needs today (an anchor snapshot plus a single service-card pool index), extending it only if a second insertable content type is ever added, vs. adding a `contentType` discriminator and per-type pool cycling now, while there is still only one type.
**Decided:** Generalized now: `FeedPlacementEntity` carries `contentType: String` (a plain key, not a shared enum — consistent with `FreshnessGate`'s key design) and `poolIndex: Int` that cycles independently per `contentType`, plus `assignmentSequence: Long` as the global insertion-order tiebreaker across types (replacing the old scheme's implicit assumption that the service-card pool index and assignment order were the same number). `FeedPlacementDao` gained `nextPoolIndex(contentType)` for the per-type cycle.
**Trade-off:** Slightly more schema than the one-type product needs today, and a small amount of indirection (the pool-index cycling math now needs a `contentType` argument even though there's only one caller). In exchange, adding a second interspersed content type later is a new constant and its own pool-cycling call, not a schema migration — asked and decided during T2 rather than deferred, since the cost of doing it now was small and the alternative (a real migration later) is not.
