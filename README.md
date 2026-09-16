# Content Feed App

A small content-feed app — a scrollable feed the user can open, save for later, and read offline.

## Build

```bash
./gradlew build
```

## Current status

- T0 (product, visual direction, and execution plan) is complete.
- T1 (dependency, module, lint, and build bootstrap) is complete and merged into `develop`.
- T2 (domain contracts and local persistence) is complete and merged into `develop`.
- T3 (remote data sources and mapping) is complete and merged into `develop`.
- T4 (repositories and feed use cases), T5 (save/offline/undo), and T6 (MVI contracts, ViewModels, and Navigation 3) are complete and merged into `develop`.
- T7 (Compose UI and design tokens) is complete on `feature/t7-compose-ui`; T8 is the remaining hardening and release-documentation pass.
- The current dependency pins and compatibility exceptions are recorded in [`gradle/libs.versions.toml`](gradle/libs.versions.toml), [`docs/SPEC.md`](docs/SPEC.md), and [`DECISIONS.md`](DECISIONS.md).

## Plan & Sequencing

The complete, executable task breakdown is in [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md). The work follows these four stages, in order:

1. **Requirements first**: pin down scope and use cases before writing any code — [`docs/SPEC.md`](docs/SPEC.md) and [`docs/USE_CASES.md`](docs/USE_CASES.md), the latter written as Given/When/Then scenarios grouped by feature specifically so each scenario doubles as the basis for a later test case. With a finite delivery window, settling scope up front lowers the risk of spending time building the wrong thing rather than triaging the right thing.

   This stage also records the product context and the original Signal Desk / Dispatch Ledger visual direction in [`PRODUCT.md`](PRODUCT.md) and [`DESIGN.md`](DESIGN.md). External reference screens remain functional references, not visual authority; the Material 3 token contract is in [`docs/DESIGN_TOKENS.md`](docs/DESIGN_TOKENS.md).

2. **Task and test breakdown from the use cases**: implementation tasks (and the tests that verify them) were derived directly from each `Feature:` section in `USE_CASES.md`, rather than from an assumed architecture decided up front. Architecture-level decisions (data/presentation layer split, feed composition model, persistence) were made as they became necessary and are recorded in [`DECISIONS.md`](DECISIONS.md).

   The task breakdown is explicit in [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md). Before each task, the owning module/layer, package boundary, public interfaces, source-of-truth boundary, state/error representation, and allowed dependencies are defined as a task-level coding-style contract. Unit tests are then written from the mapped use-case scenarios before production code; unit tests are mandatory, while UI tests are deferred until the required behavior is stable.

3. **Implementation**: built layer by layer — data sources, then repositories, then ViewModels, then UI — checking each layer against the use cases it's meant to satisfy before moving to the next. Core requirements (paginated feed with detail, save/unsave with offline access, the heterogeneous feed, the freshness policy, and explicit UI states) are built before optional enhancements, so that if time runs short, what's missing is a deliberate cutoff rather than an unfinished core behavior.

   Before feature code begins, all planned libraries, modules, code generation, lint rules, test tooling, and CI are configured. T1 now provides that baseline and passes `./gradlew build`, `./gradlew ktlintCheck`, and `./gradlew testDebugUnitTest`; it was committed separately as `chore: bootstrap modules and dependencies`. Feature work proceeds bottom-up from local persistence and remote data sources through repositories/use cases, MVI contracts/navigation, and finally Compose UI. Every task follows the same test → implementation → focused verification → commit gate.

4. **Code review**: a self-review plus an AI-assisted review pass before release, checking the implementation against `DECISIONS.md` and `USE_CASES.md` for drift (e.g. a scenario the code doesn't actually satisfy, or a decision the code silently diverged from).

   The final pass also checks the design-token contract, Android Back/insets/font-scale behavior, offline and source-scoped states, clean-checkout build/lint/unit-test gates, and requirement traceability. Deliberately deferred work — UI tests, dark-role contrast audit, animations, search/filtering, and additional source types — is documented as a trade-off rather than allowed to displace must-have behavior.

## Known limitations / with more time

- The Room DAO tests (`ArticleDaoTest`, `FeedPlacementDaoTest`, `WeatherDaoTest`) run as instrumented `androidTest`, not under `testDebugUnitTest`, so they don't run in CI's current `build`/`ktlintCheck`/`testDebugUnitTest` workflow — run them manually against an emulator or device with `./gradlew :core:connectedDebugAndroidTest`. Picking up Robolectric to fold these into the JVM unit-test suite (and CI) is deferred, not rejected.
- Foreground refresh is coordinated through `ProcessLifecycleOwner`, so cold start and later process-level foreground returns share the same lifecycle boundary.
- The dark `ColorScheme` is implemented, but light/dark semantic-role contrast has not yet been audited on device.
- Compose UI tests, animation, search/filtering, and additional source types remain lower priority after the must-have flow and unit-test gates.
