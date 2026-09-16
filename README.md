# Content Feed App

A small multi-source content feed for scanning articles, checking weather, opening service cards, and saving articles for offline reading.

## Quickstart

The documented compatibility baseline is JDK 17, Android SDK platform 36, build-tools 36.0.0, and the committed Gradle 8.13 wrapper. No API keys are required, and the service-card catalog is bundled locally at runtime.

```bash
./gradlew build ktlintCheck testDebugUnitTest
./gradlew :app:installDebug
```

Room DAO tests are instrumented tests and require a running emulator or device:

```bash
./gradlew :core:connectedDebugAndroidTest
```

## Product capabilities

- A paginated Spaceflight News article feed with article and service-card detail screens.
- A live Open-Meteo weather panel at the top of the feed.
- Service cards inserted after every five articles from a pool of 20 bundled products and images.
- Article save/unsave from the feed, detail, and Saved screens.
- Saved article text and any captured local image available without network access.
- Source-scoped loading, empty, error, pagination, offline, and back-online states.
- Compact `NavigationBar` and expanded `NavigationRail` layouts using an original Material 3 design system.

## Architecture

```text
Remote APIs / local catalog
            ↓
RemoteDataSource / local data source
            ↓
Repositories: freshness, pagination, persistence, and source isolation
            ↓
Use cases: domain-level operations and results
            ↓
ViewModels: MVI State / Event / Effect contracts
            ↓
Compose screens and design-system components
```

| Module | Responsibility |
|---|---|
| `:app` | Composition root, Koin wiring, process foreground coordination, connectivity banner, and Navigation 3 shell |
| `:core` | Shared Room/DataStore infrastructure, network plumbing, freshness primitives, and framework-level interaction utilities |
| `:feed` | Remote/local data sources, DTO mapping, repositories, domain models/use cases, MVI contracts/ViewModels, and product-specific screens |
| `:designsystem` | Material 3 theme, semantic tokens, typography, shapes, dimensions, and reusable state components |

Key boundaries:

- Room is the single source of truth for content rendered by the UI. DataStore stores freshness timestamps and the persisted article-pagination cursor.
- Remote DTOs and Retrofit/Sandwich results stop at the `:feed` data boundary; repositories expose domain models and scoped failures instead.
- Repositories own refresh orchestration and local mutation coordination. ViewModels depend on use cases, not DAOs or network types.
- `:app` owns navigation and app-wide connectivity/foreground behavior; `:feed` owns screen behavior; `:designsystem` owns reusable visual rules.

## Data sources and freshness

| Source | Policy |
|---|---|
| Spaceflight News | Live, no key; refresh after 1 hour when stale, with user-driven next-page loading |
| Open-Meteo | Live, no key; refresh after 15 minutes when stale for fixed coordinates `25.0330, 121.5654`; no location permission |
| Service-card catalog | 20 bundled products and images; no runtime network dependency or freshness window |

Room renders cached content first. Cold start, foreground return, and reconnection check each live source independently; pull-to-refresh bypasses the freshness window. There is no background scheduler. A failed refresh does not advance its freshness timestamp, so the next eligible check retries it.

New articles are prepended using the stable order `published_at DESC, id ASC`; existing service-card placements are persisted so refreshes, pagination, and cache pruning do not move them. Unsaved articles older than seven days are pruned only after a successful initial/foreground article refresh. Saved rows are protected, and a cached image is copied to app-internal storage when available; saving still succeeds if no image can be copied.

## Documentation map

- [`PRODUCT.md`](PRODUCT.md) — product purpose, principles, constraints, and accessibility commitments.
- [`DESIGN.md`](DESIGN.md) — visual direction and cross-surface design rules.
- [`docs/SPEC.md`](docs/SPEC.md) — scope, architecture baseline, data sources, and requirements traceability.
- [`docs/USE_CASES.md`](docs/USE_CASES.md) — behavior scenarios in Given/When/Then form.
- [`docs/CODING_STYLE.md`](docs/CODING_STYLE.md) — module boundaries, MVI, naming, lint, and test conventions.
- [`docs/DESIGN_TOKENS.md`](docs/DESIGN_TOKENS.md) — Material 3 semantic tokens and component contracts.
- [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md) — task sequence and verification gates.
- [`DECISIONS.md`](DECISIONS.md) — significant architecture choices and trade-offs.
- [`docs/SERVICE_CARD_ASSETS.md`](docs/SERVICE_CARD_ASSETS.md) — bundled image provenance.

## Known limitations

- Room DAO tests (`ArticleDaoTest`, `FeedPlacementDaoTest`, `WeatherDaoTest`) run under `androidTest`, not the CI JVM test task. Run them with `./gradlew :core:connectedDebugAndroidTest`.
- The semantic dark `ColorScheme` is implemented, but light/dark role contrast has not been audited on device.
- Offline/back-online banners have unit coverage, but a manual device check has not confirmed an observable connectivity transition.
- Compose UI tests, animation, search/filtering, and additional source types are deferred capabilities.
