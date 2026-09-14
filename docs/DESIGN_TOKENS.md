# Design Tokens — Signal Desk / Dispatch Ledger

Back to [Spec](SPEC.md) · [Visual direction](../DESIGN.md).

This is the visual contract to implement later in Compose. It is an original system for this product, not a transcription of external reference screens. Those references establish only that the product needs a weather section, a heterogeneous feed, saved articles, and offline states.

## Principles

- Use Material 3 semantic roles as the public API; feature UI must not consume raw colors directly.
- Use the dispatch vocabulary consistently: source, signal, archive, freshness, offline, error, and kept.
- Prefer tonal elevation and spacing over decorative borders or shadows.
- Preserve legibility at Android system font scales and keep every interactive target at least 48 dp.
- Keep light and dark role names aligned even if dark-mode implementation is deferred behind the project's core feature work.

## Color roles

The values below are starting references for the static brand scheme. They must be contrast-tested in Compose before implementation is considered complete. Android 12+ dynamic color is intentionally not the default: source/status meanings need to remain stable across devices. A future dynamic-color experiment must preserve the semantic state colors and pass the same contrast checks.

### Light scheme

| Token | Value | Material mapping | Use |
|---|---|---|---|
| `signalGround` | `#F4F6F3` | `background` | App canvas and quiet reading space |
| `signalSurface` | `#FCFDF9` | `surface` | Primary content surface |
| `signalSurfaceRaised` | `#FFFFFF` | `surfaceContainerLowest` | Raised content where separation is needed |
| `signalSurfaceMuted` | `#E8EEEC` | `surfaceVariant` / `surfaceContainer` | Supporting panels and inactive regions |
| `signalInk` | `#18262B` | `onBackground`, `onSurface` | Primary text and icons |
| `signalInkMuted` | `#53646A` | `onSurfaceVariant` | Metadata and secondary text |
| `signalOutline` | `#C5D0D0` | `outlineVariant` | Quiet dividers and field boundaries |
| `signalPrimary` | `#2E536C` | `primary` | Main actions, active destination, source emphasis |
| `signalOnPrimary` | `#FFFFFF` | `onPrimary` | Content on primary |
| `signalPrimaryContainer` | `#C7E8F5` | `primaryContainer` | Selected/current source surface |
| `signalOnPrimaryContainer` | `#0A2D3D` | `onPrimaryContainer` | Text on primary container |
| `signalSecondary` | `#715A32` | `secondary` | Measurement and supporting signal |
| `signalSecondaryContainer` | `#F1E1B9` | `secondaryContainer` | Non-critical information callout |
| `signalOnSecondaryContainer` | `#2B210B` | `onSecondaryContainer` | Text on secondary container |
| `signalTertiary` | `#A24B36` | `tertiary` | Deliberate alert/attention signal, never decoration |
| `signalTertiaryContainer` | `#F7D9CE` | `tertiaryContainer` | Attention state background |
| `signalOnTertiaryContainer` | `#3F140B` | `onTertiaryContainer` | Text on tertiary container |
| `signalError` | `#B3261E` | `error` | Recoverable source/action error |
| `signalOnError` | `#FFFFFF` | `onError` | Content on error |
| `signalErrorContainer` | `#F9DEDC` | `errorContainer` | Error message surface |
| `signalOnErrorContainer` | `#410E0B` | `onErrorContainer` | Text on error container |
| `signalSuccess` | `#37694E` | custom semantic | Save success and back-online confirmation |
| `signalWarning` | `#815E1F` | custom semantic | Stale/limited data where an error is too strong |

### Dark scheme reservation

The same semantic names map to a dark scheme rather than being inverted at runtime:

| Role family | Dark reference |
|---|---|
| Ground / surface | `#111719` / `#171E20` |
| Ink / muted ink | `#E6F0F1` / `#B7C6C8` |
| Outline | `#3E4A4D` |
| Primary / on-primary | `#9FD5E9` / `#003544` |
| Primary container / on-container | `#124B5B` / `#C7E8F5` |
| Secondary / on-secondary | `#DDC48D` / `#3D2F0F` |
| Tertiary / on-tertiary | `#FFB59E` / `#5B1D0E` |
| Error / error-container | `#FFB4AB` / `#93000A` |

Dark mode remains a post-must-have implementation slice, but no future screen may introduce a light-only raw color that makes this mapping impossible.

## Typography

Use the Material 3 type scale with the Android system sans family. The type system is intentionally quiet so source, freshness, and reading content carry the personality.

| Token | Material role | Size / line height | Weight | Use |
|---|---|---:|---:|---|
| `displayCompact` | `displaySmall` | 36 / 44 sp | 600 | Rare top-level numeric/weather emphasis |
| `headlineScreen` | `headlineSmall` | 24 / 32 sp | 600 | Screen title or article title |
| `titleSection` | `titleLarge` | 22 / 28 sp | 600 | Major section heading |
| `titleItem` | `titleMedium` | 16 / 24 sp | 600 | Article/service title |
| `bodyReading` | `bodyLarge` | 16 / 24 sp | 400 | Article summary and detail body |
| `bodyMeta` | `bodyMedium` | 14 / 20 sp | 400 | Source, publication time, supporting copy |
| `labelAction` | `labelLarge` | 14 / 20 sp | 600 | Buttons, navigation labels, Snackbar action |
| `labelSource` | `labelSmall` | 11 / 16 sp | 700 | Source marks and compact state labels |
| `dataMeasure` | `labelMedium` | 12 / 16 sp | 500 | Weather values, freshness markers, compact measurements |

`dataMeasure` may use a monospace family only for actual measurements or source identifiers. It is not a decorative “technical” font. No feature may hand-pick a new text size outside this scale.

## Spacing and dimensions

Base unit: 4 dp. Use these named steps rather than one-off numbers.

| Token | Value | Use |
|---|---:|---|
| `space1` | 4 dp | Icon-to-label or tight metadata gap |
| `space2` | 8 dp | Related content and minimum control separation |
| `space3` | 12 dp | Card/row internal grouping |
| `space4` | 16 dp | Standard content padding |
| `space5` | 24 dp | Section separation and detail margins |
| `space6` | 32 dp | Major screen grouping |
| `space7` | 40 dp | First-viewport breathing room |
| `touchMin` | 48 dp | Minimum interactive target |
| `iconSmall` | 18 dp | Inline source/status icon |
| `iconStandard` | 24 dp | Material navigation/action icon |
| `thumbnailArticle` | 96 × 72 dp | Compact article row image |
| `thumbnailSaved` | 112 × 84 dp | Saved list image |

## Shape, elevation, and line

- `shapeControl`: 8 dp; buttons, chips, fields, and Snackbar action surfaces.
- `shapeContent`: 12 dp; a content surface that genuinely needs a boundary.
- `shapeHero`: 16 dp; reserved for the weather sensor panel, not every card.
- `shapePill`: full radius; only for compact source/status labels, never for large containers.
- `elevationNone`: 0 dp; default feed rows and archive rows.
- `elevationTonal1`: Material 3 tonal surface level 1; source/status panel.
- `elevationTonal2`: Material 3 tonal surface level 2; transient or focused surface only.
- `divider`: 1 dp using `signalOutline`; use as a ledger rule, never as a thick colored rail.

## Component contracts

### App shell

- `Scaffold` owns insets and the app-wide connectivity indicator.
- Compact widths use Material `NavigationBar` for Reading and Saved; expanded widths use `NavigationRail` or a drawer according to window size.
- Use a Material top app bar for screen context. Android Back and predictive Back remain system-owned.

### Feed items

- Article: reading-first row/card with source mark, publication time, title, optional summary, image state, and article-only save action.
- Weather: one live-sensor panel at the top with current condition and forecast; no save action.
- Service: clearly distinct local dispatch insert with title, blurb, bundled icon/image, and target action; no save action.
- No nested cards and no screen-specific card radii.

### States

| State | Visual contract | Feedback contract |
|---|---|---|
| Loading | Skeleton preserves final text/image geometry | Do not block cached content when it exists |
| Empty | Quiet instructional surface using `signalSurfaceMuted` | Explain what is empty and what can recover |
| Error | Scoped error container using error roles | Keep unrelated content visible and offer retry |
| Offline | App-wide status strip using warning/secondary roles | Explain that cached/saved content remains available |
| Back online | Brief Snackbar using success roles | Disappears without interrupting reading |
| Saved | Filled keep/bookmark icon with accessible label | State updates immediately and remains synced across screens |
| Undo | Actionable Snackbar | Delay destructive local deletion until timeout |

## Implementation and review gates

- `ContentFeedTheme` exposes the semantic `ColorScheme`, `Typography`, shapes, and dimensions; feature code consumes tokens through the theme or named design-system components.
- Raw hex colors, arbitrary `sp`/`dp`, emoji icons, and per-screen Material overrides are not accepted without a documented exception.
- Verify light-role contrast, system font scale, 48 dp targets, edge-to-edge insets, screen-reader labels, and compact/expanded navigation before calling the UI slice complete. When dark theme is implemented, verify the dark-role contrast before treating that work as complete.
- UI tests are useful but secondary for this project; the semantic token and state contracts must first be covered by unit tests in the corresponding data/presentation slices.
