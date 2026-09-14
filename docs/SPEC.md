# Content Feed — Spec

A small content-feed app: a scrollable, heterogeneous feed (articles, weather, service cards) the user can open, save for later, and read offline.

- [Use Cases](USE_CASES.md)
- [Coding Style](CODING_STYLE.md)
- [Design Direction](../DESIGN.md)
- [Design Tokens](DESIGN_TOKENS.md)
- [Implementation Plan](IMPLEMENTATION_PLAN.md)

## Scope

**In scope** (see [Use Cases](USE_CASES.md) for full behavior):
- A heterogeneous feed: a one-time weather hero (live), above a single paginated, infinite-scroll stream mixing articles (live) and service cards (local mock)
- Item detail views for articles and service cards (weather's forecast is shown inline in its feed card, not a separate detail screen — see `DECISIONS.md`)
- Save/unsave for articles, with offline-readable saved articles and an undo option (weather and service cards are not saveable — see `DECISIONS.md`)
- A freshness policy with per-source staleness thresholds for the two live sources, an app-wide offline/back-online banner, and explicit loading/empty/error states throughout

**Out of scope** (not attempted, or deferred — see `README.md` for the up-to-date list once implementation starts):
- Search / filtering
- Animations / transitions beyond what a standard Compose scaffold provides for free
- Full dark-theme implementation (semantic dark roles remain reserved in the token contract and are deferred until after the core release)

## Architecture

- **Modules**: `:app` (composition root — Koin wiring, `MainActivity`, and the future navigation graph), `:core` (network, Room, DataStore, and shared Koin modules), `:feed` (repositories/data sources, MVI state/view models, and the feed/detail/saved Compose screens).
- **Stack**: Kotlin + Jetpack Compose, MVI; Koin for DI; OkHttp + Moshi (KSP codegen) + Retrofit, wrapped in sandwich for API responses; Room + DataStore Preferences for persistence and freshness timestamps; Coil for images; Custom Tabs for external links. Navigation 3 remains the planned navigation library, but is deferred until the build baseline can support its stable Android artifacts.
- **CI**: GitHub Actions runs `build`, `ktlintCheck`, and `testDebugUnitTest` on push to any branch and on pull requests.
- **Bootstrap status**: T1 is complete. The committed baseline keeps AGP `8.7.3`, Kotlin `2.0.21`, Gradle `8.13`, compileSdk/targetSdk `35`, and minSdk `24`; all external versions are centralized in `gradle/libs.versions.toml` and selected as latest-compatible versions that pass this baseline's resolution, compilation, and lint checks.
- Naming/structure conventions and lint rules are in [Coding Style](CODING_STYLE.md); rationale for each architectural choice is recorded in `DECISIONS.md` as it's decided.
- The original visual direction and token contract are in [Design Direction](../DESIGN.md) and [Design Tokens](DESIGN_TOKENS.md); the task/test/commit sequence is in [Implementation Plan](IMPLEMENTATION_PLAN.md).

## Data sources

| Source | Provides | Live or mocked |
|---|---|---|
| Spaceflight News API | Article title, source, author, timestamp, image, summary, and a link to the original article | Live, no key |
| Open-Meteo | Current conditions + daily forecast | Live, no key |
| DummyJSON-seeded local mock | Product title, thumbnail (used as the card's icon), a short blurb (truncated from the product description), price, and a synthesized target-action link | Local static JSON, bundled with the app — including the 20 product images, downloaded once and bundled as local assets rather than loaded from DummyJSON's CDN |

## Non-functional requirements

- **Offline-first for saved content**: the Saved list and any previously-saved article's detail must be fully usable with no network connection (see Use Cases → Offline access to saved items).
- **Minimal mobile data usage**: no source is refetched more often than its freshness threshold requires; manual refresh is the only way to force an early refetch (see Use Cases → Feed freshness).
- **Device support**: min SDK 24.
- **Resilience**: no single source's failure, empty response, or missing field should crash the app or block the rest of the feed (see Use Cases → Loading, empty, and error states / Per-source data handling).

## Requirements traceability

Maps each core product requirement to where it is specified in [Use Cases](USE_CASES.md).

| Core requirement | Satisfied by |
|---|---|
| Paginated feed with a detail screen | Feature: Feed browsing; Feature: Item detail |
| Save/unsave, readable offline after first load | Feature: Save for later; Feature: Unsave; Feature: Offline access to saved items |
| Heterogeneous feed, ≥2 visually distinct card types, justified | Feature: Feed browsing ("fixed structure" scenario); Feature: Per-source data handling; rationale in `DECISIONS.md` (feed ordering rule, movies→service cards rationale, live API vs. mock split) |
| Freshness policy (what "fresh" means, per-source cadence, single source of truth, offline banner, data-change behavior, ordering) | Feature: Feed freshness; Feature: Connectivity |
| All UI states handled explicitly: loading, empty, error, offline | Feature: Loading, empty, and error states; Feature: Item detail (image loading/error scenarios); Feature: Connectivity (offline/back-online banners, external-link actions offline) |
