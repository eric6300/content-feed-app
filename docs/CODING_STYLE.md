# Coding Style

Back to [Spec](SPEC.md).

What's decided so far: which tools/rules are enforced, and the naming/structure conventions the codebase follows. Not a tutorial — just the settled decisions, so later code can be checked against them instead of drifting.

## Formatting & lint

- **ktlint**, using the strict `ktlint_official` code style — no relaxed rules (wildcard imports stay banned, line length stays capped). Enforced via the `org.jlleitschuh.gradle.ktlint` Gradle plugin, applied to every module.
- **Compose-specific lint** on top of ktlint (`compose-rules`), catching Compose pitfalls plain ktlint doesn't (unstable parameters, missing `remember`, modifier-ordering issues).
- Both run in CI (`ktlintCheck`) on every push and pull request — not just a local pre-commit convenience.

## MVI screen pattern

Each screen gets one `XxxContract` interface bundling its three pieces:

```kotlin
interface FeedContract {
    data class State(
        val isLoading: Boolean = false,
        val items: List<FeedItem> = emptyList(),
    ) : ViewState

    sealed interface Event : ViewEvent {
        data object Refresh : Event
        data class SaveArticle(val articleId: Int) : Event
    }

    sealed interface Effect : ViewEffect {
        data class ShowError(val message: String) : Effect
    }
}
```

- **State**: an immutable data class with sensible defaults, updated only via `copy(...)`.
- **Event**: a sealed interface, one entry per user intent, verb-named (`Refresh`, `SaveArticle`, not `RefreshClicked`/`OnSaveArticle`).
- **Effect**: a sealed interface for one-shot outcomes only (navigation, a snackbar, an external link) — never used to carry state that should survive recomposition or a config change.

A shared base ViewModel owns the plumbing (state holder, event dispatch, effect channel); a concrete `XxxViewModel` only implements `handleEvents(event)` and its private per-event handlers, and never exposes mutable state directly to the UI.

## Naming & package layout

Per feature, group files by layer, not by mixing everything into one package:

```
feed/
  contract/    FeedContract.kt
  viewmodel/   FeedViewModel.kt
  repository/  FeedRepository.kt
  datasource/  FeedRemoteDataSource.kt, FeedLocalDataSource.kt
  api/         FeedApi.kt
  db/          ArticleEntity.kt, ArticleDao.kt
  di/          FeedModule.kt
```

Suffix names by role, not by module: `XxxViewModel`, `XxxRepository`, `XxxRemoteDataSource`, `XxxApi`, `XxxEntity`, `XxxDao`. A file's name should tell you its layer without opening it.

## Dependency injection

One Koin module per feature/layer, named `xxxModule`:

```kotlin
val feedModule = module {
    viewModel { FeedViewModel(get(), get()) }
    single { FeedRepository(get(), get()) }
}
```

`viewModel { }` for view models, `single`/`factory` for everything else — no field injection, no service locators outside the Koin module definitions.

## Testing

- One test class per view model / repository / data source, suffixed `Test`, in `src/test/` mirroring the main source's package path.
- **MockK** for mocking dependencies (repositories, data sources, API interfaces).
- **Turbine** for asserting `Flow` emissions (state sequences, cache/freshness behavior) instead of manual `collect` + latch juggling.
