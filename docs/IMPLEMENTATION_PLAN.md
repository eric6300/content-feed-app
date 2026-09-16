# Implementation Plan

Back to [README](../README.md) · [Spec](SPEC.md) · [Use Cases](USE_CASES.md) · [Coding Style](CODING_STYLE.md) · [Design Tokens](DESIGN_TOKENS.md).

## Current state

T0 is complete as documentation, and T1–T4 are complete and merged into `develop`. The repository now has the `:app`, `:core`, and `:feed` modules, centralized dependency pins, KSP/code-generation wiring, strict lint/ktlint checks, Koin application bootstrap, CI, the local persistence layer (Room entities/DAOs, `FreshnessGate`, the bundled service-card pool), remote data sources for articles and weather, repository orchestration, and feed use cases. T5 adds local saved-article commands, offline image copies, and undo finalization; feature UI remains for T6/T7.

The T1 baseline was verified with `./gradlew build`, `./gradlew ktlintCheck`, and `./gradlew testDebugUnitTest`. The exact compatibility decisions are recorded in [`DECISIONS.md`](../DECISIONS.md).

The implementation order is deliberately bottom-up: local/remote data and persistence first, orchestration next, MVI contracts after the data behavior is testable, and Compose UI last.

## Mandatory loop for every implementation task

Every task below follows the same loop. A task is not ready to implement until its first two artifacts are written down in the task branch/commit description:

1. **Task-level coding style contract:** state the owning module and package/layer, public interfaces, naming, source-of-truth boundary, state/error representation, and allowed dependencies. The global rules remain in `docs/CODING_STYLE.md`; this is a scoped clarification, not a competing style.
2. **Use-case test matrix:** map the task to named scenarios in `docs/USE_CASES.md`, choose the smallest meaningful unit boundary, and write unit tests before production implementation. Unit tests are mandatory; UI tests are optional and never replace them.
3. **Implementation:** change only the task's layer and keep DTOs, database entities, domain models, UI state, and effects separated according to the architecture.
4. **Verification:** run the task's focused tests, `ktlintCheck`, and the relevant build/test command. Fix failures before moving upward.
5. **Commit gate:** use one focused Conventional Commit after the gate is green. Do not combine a failing setup with feature implementation.

## Task sequence

### T0 — Product, visual world, and execution plan

**Status:** complete as documentation.

- Capture product facts in `PRODUCT.md` and the approved original visual direction in `DESIGN.md`.
- Define the Material 3 token contract in `docs/DESIGN_TOKENS.md` without borrowing palette, geometry, or layout from external reference screens.
- Keep external reference screens as functional evidence only; they do not become the implementation target.
- No code or test commit belongs to this task.

### T1 — Dependency, module, lint, and build bootstrap

**Status:** complete as the bootstrap baseline.

**Style contract:** version everything through `gradle/libs.versions.toml`; keep `:app` as composition root, `:core` as shared infrastructure, and `:feed` as feature code; keep Gradle convention and plugin wiring out of feature packages; use official Material 3 and Android APIs for platform behavior.

**Work:**

- Add `:core` and `:feed` modules and wire their source-set dependencies.
- Add the compatible bootstrap stack: KSP, Room, DataStore Preferences, Retrofit, OkHttp, Moshi, sandwich, Koin, Coil, Custom Tabs, ktlint, Compose rules, MockK, Turbine, and coroutine test support.
- Keep Navigation 3 as a planned dependency, but defer it because the stable Android artifacts currently require compileSdk 36 and AGP 8.9.1 or newer. T6 must either upgrade the build baseline or record a revised navigation decision before adding the graph.
- Configure code generation, Android test dependencies, strict lint checks, and the GitHub Actions build/test workflow.
- Add only the minimum module/application scaffolding needed to prove dependency resolution; do not begin feature code.

**Compatibility baseline:** AGP `8.7.3`, Kotlin `2.0.21`, KSP `2.0.21-1.0.28`, Gradle `8.13`, compileSdk/targetSdk `35`, minSdk `24`, Java/Kotlin target `11`, Compose BOM `2025.03.00`, Lifecycle `2.8.7`, Browser `1.8.0`, Room `2.7.2`, and the remaining pins in `gradle/libs.versions.toml`. These are latest-compatible pins for the retained baseline, not latest-overall releases.

**Test/build gate:** passed with `./gradlew build`, `./gradlew ktlintCheck`, and `./gradlew testDebugUnitTest`. The version choices resolve together on the committed AGP/Kotlin/Gradle baseline.

**Commit:** `chore: bootstrap modules and dependencies`. This is the required clean green baseline before feature implementation starts.

### T2 — Domain contracts and local persistence

**Status:** complete on `feature/t2-domain-and-local-persistence`.

**Style contract:** local persistence owns Room entities/DAOs and DataStore adapters; feature/domain models do not expose Room or Retrofit types; DAOs expose `Flow` for reads and suspend commands for writes; all persistence names carry their role suffix.

**Work:**

- Define the article, weather snapshot/forecast, service card, saved-article, and feed-placement contracts needed by the use cases.
- Implement Room entities/DAOs for article content, saved state/local image reference/pending-undo bookkeeping, weather cache, and sticky feed placement. A placement stores a snapshot of its neighboring article's sort key (`published_at`, `id`), not a foreign key to that row, so it stays valid after the article is pruned. It also carries a `contentType` key (not a shared enum) and a `poolIndex` that cycles independently per content type, plus a global `assignmentSequence` tiebreaker — service cards are the only content type today, but this costs a future second insertable type no schema migration (see `DECISIONS.md`).
- Implement the parameterized `FreshnessGate` backed by DataStore and keep its timestamps separate from Room data and per-article `fetchedAt` bookkeeping.
- Define the hybrid cache retention policy: keep fetched feed content in Room for cache-first startup, prune unsaved article rows older than 7 days after the initial open/return article refresh succeeds (not after a manual pull-to-refresh or reconnect refresh), and protect saved articles from that cleanup. Ordinary feed images remain in Coil's cache; only saved images are copied to app-internal storage.
- Add the bundled service-card JSON and local image references without a runtime DummyJSON dependency; a malformed pool entry is skipped at parse time instead of failing the whole pool.

**Mandatory unit tests:** DAO query ordering, saved filtering, and Saved-list most-recently-saved ordering; unsaved-article retention query excludes saved rows and retains recent rows; upsert/idempotence; article `published_at` fallback to `date unknown`; sticky placement anchor stability, including after its anchor article is pruned, and per-content-type pool-index cycling; FreshnessGate fresh/stale/bypass behavior; local service pool order/cycling and malformed-entry skipping.

**Verification:** `:core`/`:feed` unit tests and ktlint green; Room DAO tests run as instrumented `androidTest` (17 tests) against the `Pixel_9` emulator via `./gradlew :core:connectedDebugAndroidTest` — not part of `testDebugUnitTest`/CI (see `DECISIONS.md`); full `./gradlew build` green across `:core`/`:feed`/`:app`.

**Commit:** `feat: add local feed persistence`.

### T3 — Remote data sources and mapping

**Status:** complete on `feature/t3-remote-data-sources`.

**Style contract:** Retrofit APIs and DTOs live only in `:feed`'s `data/remote` layer and stay `internal`; `:core` owns only the shared, source-agnostic network plumbing (one `OkHttpClient`, the `Moshi` singleton). Mappers normalize missing/invalid source fields into domain-safe values and perform no interpretation of raw source codes. Sandwich `ApiResponse`, Retrofit/OkHttp types, and DTOs are converted at the `RemoteDataSource` boundary into a shared `RemoteResult<T>` (`Loaded`/`Failure`) wrapping per-source payloads (`ArticlePage`, `WeatherData`) plus a shared `RemoteFailure` vocabulary, and never leak past it. No Room, repository, or UI type imports.

**Work:**

- Move the `Moshi` singleton from `feedModule` to `coreModule` and add the shared `OkHttpClient` (with a `BASIC` `HttpLoggingInterceptor`, active only in debug builds via `BuildConfig.DEBUG`) alongside it; declare the `INTERNET` permission in `core/src/main/AndroidManifest.xml` so the manifest merger folds it into `:app`.
- Add Spaceflight News article paging (`GET articles/`, with `offset`/`limit`) and Open-Meteo weather (`GET forecast`, with current + daily field lists). The local service-card source is **not** part of this task — `ServiceCardCatalog` was already built in T2.
- Normalize article image URLs, summaries, authors, and dates; map the raw Open-Meteo `current`/columnar `daily` payloads onto `WeatherData`/`WeatherForecastDay` without interpreting the WMO `weather_code` (it passes through as a raw `Int?`; its icon/label fallback is T6's view-state concern).
- Convert sandwich outcomes into `RemoteResult<ArticlePage>`/`RemoteResult<WeatherData>`/`RemoteFailure` (network-unavailable / HTTP code / unknown). A successful empty page and end-of-pagination both collapse into `Loaded`, with `isLastPage` derived from the source's `next` field being null — never from comparing result count to the requested limit. A 2xx response with no body (sandwich substitutes `kotlin.Unit` for the DTO) is also normalized into `Failure(RemoteFailure.Unknown)` rather than being allowed to throw.
- Take `offset`/`limit` as plain parameters; T4 owns the next-page cursor.
- Enable core library desugaring in `:feed` and `:app` so `java.time` (`IsoTimestampParser`) is usable on minSdk 24, keeping ISO-8601 parsing a pure, separately-testable technical concern from the epoch-sentinel business rule (which lives in `ArticleMapper`).

**Not in this task:** the weather unrecognized-code icon/label fallback (`USE_CASES.md` → Per-source data handling → Weather) is a rendering concern, not a parsing one — it is covered by T6's WMO-code-to-`WeatherCondition` mapping below. The empty service-card title fallback was cut outright (see `DECISIONS.md`, "Trimmed defensive per-field scenarios") and has no implementation here.

**Mandatory unit tests:** DTO parsing against captured real responses from both sources; article mapping with missing image, missing summary, and missing/empty authors; `date unknown` for the epoch sentinel `1970-01-01T00:00:00Z`; ISO-8601 parsing of `Z`, fractional-second, and explicit-offset timestamps, and null for unparseable input; successful empty page; end-of-pagination from a null `next`; weather current/daily mapping including a raw unrecognized WMO code passing through unmodified; source failure mapping (HTTP error code, IO exception, unknown throwable, no-body success) for both sources; Retrofit call-adapter/converter wiring validated eagerly for both API interfaces.

**Verification:** `:core`/`:feed` ktlintCheck and `testDebugUnitTest` green (62 tests in `:feed`); full `./gradlew build` green (Android Lint's `NewApi` check is what actually proves the desugaring config, since JVM unit tests have `java.time` natively; the release variant is also what proves the logging interceptor is compiled out via `BuildConfig.DEBUG`); confirmed `INTERNET` reaches `:app`'s merged manifest.

**Commit:** `feat: add feed data sources`.

### T4 — Repositories and feed use cases

**Status:** complete on `feature/t4-repositories-and-use-cases`.

**Style contract:** repositories are the only boundary that coordinates remote fetch, Room persistence, freshness, and pagination. Room is the single source of truth for UI reads. Use cases expose domain data and explicit scoped errors; they do not leak `ApiResponse`, DTOs, DAOs, or DataStore keys.

**Work:**

- Read cached feed data immediately from Room and run freshness checks/refetches independently.
- Implement weather/article TTLs, manual refresh bypass, reconnect refresh, and source-isolated failure behavior.
- Run the unsaved-article retention cleanup only after the initial open/return freshness refresh completes successfully — never after a manual pull-to-refresh or a reconnect refresh, since either could otherwise prune an article the user is currently scrolled to (see `DECISIONS.md`). Do not add background cleanup or an arbitrary fixed row cap in the first release.
- Maintain the independent next-page offset cursor; freshness top-check always requests offset zero and compares stable article ids rather than deriving offsets from Room row count.
- Compose weather once at the top, article order by `published_at DESC, id ASC`, and sticky service-card inserts without recalculating existing placements — robust to a placement's anchor article having since been pruned, and with prepended articles counted in their own independent window that never renumbers already-placed cards.
- Expose loading/empty/error/end-of-pagination signals as domain-level state inputs.

**Mandatory unit tests:** cached-first startup; each TTL boundary; manual refresh; reconnect refresh; the initial open/return refresh runs retention cleanup on success and preserves the cache on failure, while manual pull-to-refresh and reconnect refresh never trigger retention cleanup; new article prepend without moving existing content; stable article ordering; independent source failure; page append/end/retry; sticky service-card behavior across refresh, pagination, and anchor-article pruning; prepended-article count window stays independent of already-placed cards.

**Verification:** `:core`/`:feed` unit tests and ktlint green (107 `:feed` unit tests, including the new composition/repository/use-case suites); full `./gradlew build` green across `:core`/`:feed`/`:app`; `./gradlew :core:connectedDebugAndroidTest` green (25 tests) against the `Pixel_9` emulator, covering the new `FeedPlacementDao` queries (`nextPoolIndex`/`nextAssignmentSequence`/`deleteOrphanedBelow`) added for this task — not part of `testDebugUnitTest`/CI, consistent with T2.

**Post-review hardening:** a code-review pass on this task's branch found two correctness bugs, fixed before merge: the pagination end-of-stream flag was only ever set `true`, never cleared, so a stream that shrank below one page and later grew past it would permanently stop paginating; and the shared repository's pagination cursor and placement-assignment counters had no concurrency guard, so a reconnect refresh racing a scroll-driven page load could silently lose one operation's cursor advance. Both fixed in `DefaultArticleRepository` — the flag assignment made unconditional, and a `Mutex` added around each method's local read-modify-write (network fetches stay outside the lock), with the append path's cursor write changed from an absolute to a lock-scoped relative update so it can no longer clobber a concurrent refresh's commit. Verified with a real-fake-based test that fails against the pre-fix code (see `DECISIONS.md`).

**Commit:** `feat: implement feed orchestration`.

### T5 — Save, offline, and undo use cases

**Status:** implementation complete on `feature/t5-save-offline-undo`; this section is synchronized with the shipped T5 behavior.

**Style contract:** save behavior is article-only and expressed as explicit use-case commands; local persistence is authoritative; image copying is an injectable boundary and never blocks article text save; destructive cleanup is delayed behind an undo policy.

**Work:**

- Implement save/unsave from feed, detail, and Saved contexts with one shared path; feed/detail toggles are immediate (tapping again re-saves, no undo), while a removal from the Saved list goes through the undo window below (see `DECISIONS.md`).
- Persist saved article content and local image references; reuse an already-loaded image offline and tolerate an unavailable image cache.
- Implement the brief undo window, scoped to Saved-list removals, and final local data/image deletion after expiry — including finalizing on reopen if the window lapsed while the app was closed, and tracking multiple pending undos independently.

**Fixed decisions:** finalization clears save state and deletes the saved image while retaining the cached article row for the 7-day prune; the visible undo window is 5 seconds with a 1-second persisted grace period; reopening the app finalizes every pending removal regardless of its remaining deadline; saved-image files are addressed by article id under app-internal storage; and a cache miss never starts a network fetch or fails the save.

**Mandatory unit tests:** save from each entry point; offline save with/without cached image; cross-screen saved-state consistency; immediate feed/detail unsave with no undo; Saved-list undo restore; timeout finalization; restart finalization regardless of remaining deadline; independent concurrent undos; Saved list empty/populated/offline reads/most-recently-saved ordering.

**Verification:** `:core`/`:feed` ktlint and JVM unit tests are green; the Room DAO additions are covered by the existing emulator-only `:core` instrumented suite; full `./gradlew build` is the required Android Lint gate for Coil and file I/O. The known limitation is that T5 has no main-source-set writer to Coil's disk cache, so real-app image copies miss until T7 renders remote images with an explicit `diskCacheKey`.

**T6 obligation:** call `FinalizePendingUnsavesUseCase(AppStart)` from the composition root on foreground and `UndoWindowElapsed(articleId)` when each undo Snackbar is dismissed. Dismissal explicitly finalizes only that pending article; the persisted 1-second grace remains the deadline guard for late undo and the restart/fallback sweep. Because `lifecycle-process` is not currently pinned, T6 must either add it for real foreground transitions or accept `MainActivity.onStart` and its configuration-change trade-off.

**T7 obligation:** every remote image request must set `diskCacheKey(article.imageUrl)`; `localImagePath != null` renders from `File(path)`, while null renders a placeholder.

**Commit:** `feat: add offline article saving`.

### T6 — MVI contracts, ViewModels, and navigation

**Style contract:** each screen owns one `XxxContract` with immutable `State`, verb-named `Event`, and one-shot `Effect`; ViewModels consume repositories/use cases only; navigation and external-link requests are effects, never state fields; state collection is lifecycle-aware.

**Work:**

- Add Feed, Article Detail, and Saved contracts/ViewModels.
- Represent source-scoped loading/empty/error states, pagination retry, connectivity banner, save/undo, and external-link effects explicitly.
- Wire the navigation graph and preserve feed scroll position when returning from detail. Before implementation, resolve the Navigation 3 compatibility boundary recorded in T1; do not add an incompatible stable artifact or silently switch navigation libraries.
- Add app-wide connectivity observation at the composition root without duplicating banner logic per screen.
- Map the raw WMO `weatherCode: Int?` carried through by T3 onto a `WeatherCondition` view-state enum, with an explicit neutral fallback for any code this app's table does not recognize (`USE_CASES.md` → Per-source data handling → Weather). The enum-to-icon/label binding is T7's; the code-to-enum decision is unit-tested here.

**Mandatory unit tests:** initial/cache-first state sequences; feed refresh/pagination event handling; scoped errors; save/undo effects; detail navigation/external-link effects; Saved state; connectivity transitions; WMO weather-code mapping, including an unrecognized code (and a null code) falling back to the neutral `WeatherCondition` rather than crashing or producing a blank. Use Turbine for Flow/effect assertions.

**Commit:** `feat: add feed presentation contracts`.

### T7 — Compose UI and token implementation

**Style contract:** implement only the tokens and component contracts in `DESIGN.md` and `docs/DESIGN_TOKENS.md`; use Material 3 components and semantic theme roles; no raw colors, arbitrary dimensions, emoji icons, or external-reference imitation; preserve Android Back/insets and 48 dp targets.

**Work:**

- Implement `ContentFeedTheme` from the token contract and the compact/expanded navigation shell.
- Build Feed, article/detail, and Saved screens using the source-mark/dispatch-ledger grammar.
- Implement skeleton, empty, scoped error, offline/back-online, pagination retry, save, and undo states.
- Use Coil loading/error placeholders for remote article images and bundled assets for service cards.
- Keep UI tests optional until all required unit tests and build/lint checks are green; add focused Compose tests only for high-value semantics/navigation if time remains.

**Verification gate:** unit tests remain mandatory; perform emulator/manual checks for system Back, insets, font scale, light token mapping, offline banner, saved article offline detail, and compact/expanded navigation. Confirm that the reserved dark-token names remain implementable, but keep full dark-theme implementation deferred and outside this gate. Capture evidence only after the flow works.

**Commit:** `feat: build feed screens`.

### T8 — Hardening, review, and release documentation

**Style contract:** no behavior changes without a corresponding use-case/test/document update; keep README, SPEC, DECISIONS, AI_USAGE, and known limitations truthful to the shipped code.

**Work and gates:**

- Run clean-checkout `./gradlew build`, `ktlintCheck`, and all unit tests.
- Run CI-equivalent checks on push/PR workflow.
- Perform one requirement traceability review from `PRODUCT.md` / `SPEC.md` → `USE_CASES.md` → tests → implementation.
- Record significant implementation decisions and honest AI accept/reject/rewrite examples at the relevant commit, following the repo's logging conventions.
- Update README build command, Plan & Sequencing status, known limitations, and deliberate cuts.

**Commit:** `docs: finalize release notes`.

## Priority and cut line

T1–T6 and the core portion of T7 are the delivery line: paginated heterogeneous feed, detail, article save/unsave, offline saved access, freshness policy, explicit states, and unit tests. T7 UI polish, full dark-theme implementation, UI tests, animations, search/filter, and additional source types are cut or deferred if the delivery window tightens. Never cut the unit-test coverage for a completed data/presentation task to make room for optional UI polish.
