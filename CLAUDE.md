# content-feed-app

A content-feed Android app: a scrollable, heterogeneous feed (articles, weather, service cards) that can be opened, saved for later, and read offline. Read `docs/SPEC.md` (scope, architecture, module structure) and `docs/USE_CASES.md` (expected behavior) before writing code.

## Coding style

Follow `docs/CODING_STYLE.md` for every file touched in this repo: the ktlint/compose-rules lint setup, the MVI Contract pattern (`State`/`Event`/`Effect` per screen), package layout by layer, Koin module naming, and testing conventions (MockK + Turbine). That file is the single source of truth for these conventions — don't restate or duplicate them here or elsewhere.

Don't introduce a different pattern (a different state-management style, flattening the layered package structure, a different DI or testing approach) without flagging it to the user first. If a convention changes, update `docs/CODING_STYLE.md` itself, not just the code.

## Docs

- `README.md` — build command, plan & sequencing, known limitations.
- `DECISIONS.md` — architecture decision log: what was considered, what was decided, the trade-off. Keep entries genuinely useful, not filler.
- `AI_USAGE.md` — AI-collaboration log: which tools were used, what was accepted, rejected, or rewritten, and why.
- `docs/SPEC.md` — a short index/overview doc that links out to focused sub-docs (`USE_CASES.md`, `CODING_STYLE.md`, and more as they're written).
- Two skills exist for appending entries to `DECISIONS.md`/`AI_USAGE.md` (`/log-decision`, `/log-ai-usage`, defined one level up in the outer workspace — see its `CLAUDE.md`) — use them only when the user asks or at commit time (see Working conventions below), not proactively after every step.
- Tech choices already made (see `DECISIONS.md` for the real record): Kotlin, Jetpack Compose, package `com.eric.contentfeed`, minSdk 24, compileSdk/targetSdk 35, AGP 8.7.3, Kotlin 2.0.21, Gradle 8.13 (wrapper committed).
- Build: `./gradlew build` from inside this repo.

## Working conventions

- Don't commit `README.md`, `DECISIONS.md`, `AI_USAGE.md`, `docs/`, or `.claude/` until told to — they were deliberately left untracked after the initial app-scaffold commit so the user controls when they get committed.
- Git commits here have no Claude attribution trailer (intentional — commits reflect the user's own authorship).
- Commit messages use **Conventional Commits** style (`type: short summary`, e.g. `docs:`, `feat:`, `fix:`, `refactor:`, `test:`, `chore:`), with an optional body explaining the *why*.
- **All authored documents are written in English** — `SPEC.md`, `USE_CASES.md`, `README.md`, `DECISIONS.md`, `AI_USAGE.md`, `CODING_STYLE.md`, and any future doc, even though the working conversation with the user is in Traditional Chinese (Taiwan).
- Don't proactively call `/log-decision` or `/log-ai-usage` after each individual step. The user wants those written at commit time, not eagerly during discussion.
