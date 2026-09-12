# Content Feed App

A small content-feed app in the spirit of LINE TODAY / VOOM — a scrollable feed the user can open, save for later, and read offline.

## Build

```
# TODO: one-line build command
```

## Plan & Sequencing

The work was sequenced in four stages, in this order:

1. **Requirements first**: read the brief and pin down scope and use cases before writing any code — [`docs/SPEC.md`](docs/SPEC.md) and [`docs/USE_CASES.md`](docs/USE_CASES.md), the latter written as Given/When/Then scenarios grouped by feature specifically so each scenario doubles as the basis for a later test case. With a fixed time budget for the assignment, settling scope up front lowers the risk of spending time building the wrong thing rather than triaging the right thing.
2. **Task and test breakdown from the use cases**: implementation tasks (and the tests that verify them) were derived directly from each `Feature:` section in `USE_CASES.md`, rather than from an assumed architecture decided up front. Architecture-level decisions (data/presentation layer split, feed composition model, persistence) were made as they became necessary and are recorded in [`DECISIONS.md`](DECISIONS.md).
3. **Implementation**: built layer by layer — data sources, then repositories, then ViewModels, then UI — checking each layer against the use cases it's meant to satisfy before moving to the next. Within that, the brief's must-haves (paginated feed with detail, save/unsave with offline access, the heterogeneous feed, the freshness policy, and explicit UI states) were built before any nice-to-have, so that if time ran short, what's missing is a deliberate cutoff rather than an unfinished must-have.
4. **Code review**: a self-review plus an AI-assisted review pass before submission, checking the implementation against `DECISIONS.md` and `USE_CASES.md` for drift (e.g. a scenario the code doesn't actually satisfy, or a decision the code silently diverged from).

## Known limitations / with more time

<!-- What's missing, and what you'd do next given more time. -->
