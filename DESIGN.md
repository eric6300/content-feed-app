# Signal Desk / Dispatch Ledger

**Status:** approved visual direction for the Content Feed app
**Mode:** Operate — the reader is scanning, opening, saving, and recovering from connectivity changes.

## Visual authority

This is an original visual world derived from the product's mechanism: making several live and cached sources legible in one trustworthy reading flow. External reference screens are functional references only. They do not define this palette, typography, spacing, card treatment, navigation, or composition.

## Direction

Treat the app as a compact dispatch desk: a live source register feeding a continuous ledger of readable entries. The visual language borrows the discipline of source stamps, field notes, measurement marks, and archival indexing without imitating a literal newspaper, dashboard, or another content app.

- **First viewport:** establish the current source condition and the beginning of the dispatch stream immediately. Weather is a compact live-sensor readout; articles carry the reading hierarchy; service cards are deliberate dispatch inserts rather than interchangeable article cards.
- **Topology:** one continuous feed with explicit source boundaries. Saved is an archive destination, not a second feed personality. On compact Android widths use Material NavigationBar; on expanded widths use NavigationRail or an appropriate drawer.
- **Material grammar:** Material 3 surfaces, tonal elevation, semantic color roles, system typography, and standard Android controls. Use restrained rules and source marks for structure; do not use gradients, glass, decorative shadows, or nested card stacks.
- **State grammar:** freshness is a source/time mark, offline is a persistent app-wide status strip, loading is a content skeleton, errors are scoped to the failing source, saved is a durable keep mark, and undo is an actionable Snackbar.
- **Motion:** state change only — short Material transitions, no page-load choreography. Preserve content visibility and honor the system animation-reduction setting.
- **Adaptation:** preserve reading order and touch target size as width changes; reduce visible density on compact screens without shrinking type or controls below Android guidance.

## Cross-surface reach

The same source mark, status vocabulary, divider weight, type hierarchy, save affordance, and archive treatment must work on Feed, Article Detail, and Saved. A future source or detail surface should be able to join the system by choosing semantic roles, not by inventing local colors or card shapes.

## Anti-goals

- Do not reproduce any external reference screen's palette, carousel composition, navigation styling, or article-card geometry.
- Do not make every feed item a rounded floating card; hierarchy comes from layout, source marks, imagery, and tonal surfaces.
- Do not expose implementation metadata such as database `fetchedAt` as if it were the article's publication time.
- Do not let a live-source failure erase cached content or block unrelated sources.

## Implementation authority

The token contract is [docs/DESIGN_TOKENS.md](docs/DESIGN_TOKENS.md). The execution order, task-level style contracts, test mapping, and commit gates are [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md).
