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
- Item detail views for articles and service cards (weather's forecast is shown inline in its feed card, not a separate detail screen — it's continuously-updating data with no fixed "version" to drill into)
- Save/unsave for articles, with offline-readable saved articles and an undo option (weather and service cards are not saveable, for the same reason plus — for service cards — the target action already being the card's one interaction)
- A freshness policy with per-source staleness thresholds for the two live sources, an app-wide offline/back-online banner, and explicit loading/empty/error states throughout

**Out of scope** (not attempted, or deferred — see `README.md` for the up-to-date list once implementation starts):
- Search / filtering
- Animations / transitions beyond what a standard Compose scaffold provides for free
- Dark-role contrast audit (the semantic dark `ColorScheme` is implemented; contrast verification is deferred)

## Architecture

- **Modules**: `:app` (composition root — Koin wiring, `MainActivity`, and the Navigation 3 shell), `:core` (network, Room, DataStore, and shared Koin modules), `:feed` (repositories/data sources, MVI state/view models, and feed/detail/saved Compose screens), and `:designsystem` (tokenized Material 3 theme and generic UI components).
- **Stack**: Kotlin + Jetpack Compose, MVI, and Navigation 3; Koin for DI; OkHttp + Moshi (KSP codegen) + Retrofit, wrapped in sandwich for API responses; Room + DataStore Preferences for persistence and freshness timestamps; Coil for images; Custom Tabs for external links.
- **CI**: GitHub Actions runs `build`, `ktlintCheck`, and `testDebugUnitTest` on push to any branch and on pull requests.
- **Bootstrap status**: T1 is complete and T6 upgraded the baseline for Navigation 3. The project uses AGP `8.9.1`, Kotlin `2.2.10`, Gradle `8.13`, compileSdk `36`, targetSdk `35`, and minSdk `24`. `targetSdk` was intentionally not raised with `compileSdk`: the Android 16 behavior changes that come with `targetSdk 36` have not been verified on device. All external versions are centralized in `gradle/libs.versions.toml` and selected as compatible with the committed build.
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
| Heterogeneous feed, ≥2 visually distinct card types, justified | Feature: Feed browsing ("fixed structure" scenario); Feature: Per-source data handling; source-selection rationale in `DECISIONS.md`; ordering rule in `README.md` → Freshness policy |
| Freshness policy (what "fresh" means, per-source cadence, single source of truth, offline banner, data-change behavior, ordering) | Feature: Feed freshness; Feature: Connectivity |
| All UI states handled explicitly: loading, empty, error, offline | Feature: Loading, empty, and error states; Feature: Item detail (image loading/error scenarios); Feature: Connectivity (offline/back-online banners, external-link actions offline) |
