# Content Feed App

A small content-feed app — a scrollable feed the user can open, save for later, and read offline.

## Build

```bash
./gradlew build
```

## What's implemented

- A paginated article feed backed by Spaceflight News, with an article detail screen.
- Save / unsave, with saved articles and their images readable with no network connection.
- A heterogeneous feed: article cards, a live weather panel (Open-Meteo), and service cards interleaved every five articles, each rendered as a visually distinct cell in one scrollable surface.
- A freshness policy with per-source windows, cache-first startup, and app-wide offline and back-online banners (see below).
- Explicit loading, empty, error, offline, pagination-end, and pagination-error states, scoped per source rather than collapsed into one feed-wide state.
- Multi-module structure (`:app`, `:core`, `:feed`, `:designsystem`), an original Material 3 token contract with a dark scheme, and GitHub Actions running build, ktlint, and unit tests.
- The current dependency pins and compatibility exceptions are recorded in [`gradle/libs.versions.toml`](gradle/libs.versions.toml), [`docs/SPEC.md`](docs/SPEC.md), and [`DECISIONS.md`](DECISIONS.md).

## Freshness policy

"Fresh" here means *when the app last checked a source for new content* — not whether what you are reading is still valid. Room is the single source of truth and always renders first; the freshness check runs beside it and updates the list in place only if something changed. The app therefore never blocks its own startup on a network response.

**Per-source cadence.** Each live source has its own staleness window, set to roughly the rate at which its underlying data changes rather than a single global interval: weather is refetched after 15 minutes, because conditions meaningfully change on that order; articles after 1 hour, because publication cadence is slower and a shorter window would spend data on an unchanged list. Service cards have no window at all — they are a bundled local mock with nothing to refetch. All of these live in one place, `FeedPolicy.kt`, rather than as literals spread across repositories.

**What triggers a refresh.** Cold start and return-to-foreground (via `ProcessLifecycleOwner`) run a freshness check that respects the windows above. Reconnecting after being offline does the same — sources still inside their window are left alone. Manual pull-to-refresh is the only path that bypasses the window, which is what makes it useful. There is no background scheduling: nothing refreshes while the user is not looking at the app.

**Mobile data.** No source is refetched more often than its staleness window requires, there is no background scheduling, and a per-source mutex stops a reconnect check and a foreground check from firing the same request twice — the only ways to trigger a network call are opening the app, returning to it, reconnecting, and pull-to-refresh.

**Failure, offline, and ordering.** A failed refresh does not mark the source as fetched, so the next check retries it instead of silencing it for a full window. One source failing never blocks the others: cached rows keep rendering and the error is attached as a scoped signal. An offline banner is app-wide, and a brief "back online" banner follows reconnection. New articles are prepended, so nothing the user is currently reading shifts under them; ordering is `published_at DESC, id ASC`, the secondary key being necessary because a large number of older articles share a `1970-01-01` placeholder timestamp and would otherwise reshuffle between queries.

**Retention.** After a successful open/return refresh, unsaved articles older than 7 days are pruned; saved articles are exempt and their images are copied into app-internal storage, so storage use tracks the saved count exactly and needs no separate eviction policy. Cleanup runs only from that one trigger — never after a manual or reconnect refresh — so it can never delete something the user has already scrolled to.

## Plan & Sequencing

The complete, executable task breakdown is in [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md). The work follows these four stages, in order:

1. **Requirements first**: pin down scope and use cases before writing any code — [`docs/SPEC.md`](docs/SPEC.md) and [`docs/USE_CASES.md`](docs/USE_CASES.md), the latter written as Given/When/Then scenarios grouped by feature specifically so each scenario doubles as the basis for a later test case. With a finite delivery window, settling scope up front lowers the risk of spending time building the wrong thing rather than triaging the right thing.

   This stage also records the product context and the original Signal Desk / Dispatch Ledger visual direction in [`PRODUCT.md`](PRODUCT.md) and [`DESIGN.md`](DESIGN.md). External reference screens remain functional references, not visual authority; the Material 3 token contract is in [`docs/DESIGN_TOKENS.md`](docs/DESIGN_TOKENS.md).

2. **Task and test breakdown from the use cases**: implementation tasks (and the tests that verify them) were derived directly from each `Feature:` section in `USE_CASES.md`, rather than from an assumed architecture decided up front. Architecture-level decisions (persistence, concurrency, and the technology baseline) were made as they became necessary and are recorded in [`DECISIONS.md`](DECISIONS.md); narrower implementation choices are documented alongside the code they shape in [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md).

   The task breakdown is explicit in [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md). Before each task, the owning module/layer, package boundary, public interfaces, source-of-truth boundary, state/error representation, and allowed dependencies are defined as a task-level coding-style contract. Unit tests are then written from the mapped use-case scenarios before production code; unit tests are mandatory, while UI tests are deferred until the required behavior is stable.

3. **Implementation**: built layer by layer — data sources, then repositories, then ViewModels, then UI — checking each layer against the use cases it's meant to satisfy before moving to the next. Core requirements (paginated feed with detail, save/unsave with offline access, the heterogeneous feed, the freshness policy, and explicit UI states) are built before optional enhancements, so that if time runs short, what's missing is a deliberate cutoff rather than an unfinished core behavior.

   Before feature code begins, all planned libraries, modules, code generation, lint rules, test tooling, and CI are configured. T1 now provides that baseline and passes `./gradlew build`, `./gradlew ktlintCheck`, and `./gradlew testDebugUnitTest`; it was committed separately as `chore: bootstrap modules and dependencies`. Feature work proceeds bottom-up from local persistence and remote data sources through repositories/use cases, MVI contracts/navigation, and finally Compose UI. Every task follows the same test → implementation → focused verification → commit gate.

4. **Code review**: a self-review plus an AI-assisted review pass before release, checking the implementation against `DECISIONS.md` and `USE_CASES.md` for drift (e.g. a scenario the code doesn't actually satisfy, or a decision the code silently diverged from).

   The final pass also checks the design-token contract, Android Back/insets/font-scale behavior, offline and source-scoped states, clean-checkout build/lint/unit-test gates, and requirement traceability. Deliberately deferred work — UI tests, dark-role contrast audit, animations, search/filtering, and additional source types — is documented as a trade-off rather than allowed to displace must-have behavior.

## Known limitations / with more time

- The Room DAO tests (`ArticleDaoTest`, `FeedPlacementDaoTest`, `WeatherDaoTest`) run as instrumented `androidTest`, not under `testDebugUnitTest`, so they don't run in CI's current `build`/`ktlintCheck`/`testDebugUnitTest` workflow — run them manually against an emulator or device with `./gradlew :core:connectedDebugAndroidTest`. Picking up Robolectric to fold these into the JVM unit-test suite (and CI) is deferred, not rejected.
- Foreground refresh is coordinated through `ProcessLifecycleOwner`, so cold start and later process-level foreground returns share the same lifecycle boundary.
- The dark `ColorScheme` is implemented, but light/dark semantic-role contrast has not yet been audited on device.
- The offline/back-online banners are covered by unit tests, but not by a manual device check: the emulator used for manual verification never produced an observable connectivity transition, so this hasn't been confirmed on real hardware.
- Compose UI tests, animation, search/filtering, and additional source types remain lower priority after the must-have flow and unit-test gates.
