# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Kotlin, Jetpack Compose, Material 3, minimum SDK 24. The implementation plan already chooses a small multi-module structure (`:app`, `:core`, `:feed`) and the repository's existing SPEC records the planned supporting libraries.

## Users

Inferred from the current product brief: a mobile reader who wants to scan content from several sources during short sessions, save articles, and return to them when connectivity is unreliable.

## Product Purpose

Content Feed is a small multi-source reading app. It combines article content with weather and service information in one scrollable feed, lets the reader open article details, save articles, and read saved articles offline.

Success means that a reader can understand what is available at a glance, open or save an article with low friction, and still access saved article content without a network connection.

## Positioning

Inferred from the product brief: a trustworthy multi-source reading entry point. Its differentiator is not a novel content format; it is making heterogeneous sources understandable while being explicit about freshness, source failure, and offline availability.

## Operating Context

- The primary flow is short, phone-sized feed sessions: scan, open, save, or continue scrolling.
- Connectivity can disappear while the app is open. Previously loaded content should remain usable while it is within the cache-retention window, and saved articles must remain readable offline.
- The project is developed in small, reviewable increments, so implementation reasoning, test coverage, commit boundaries, and documented trade-offs remain visible alongside the product behavior.

## Capabilities and Constraints

- The feed contains live articles from Spaceflight News, live weather from Open-Meteo, and bundled local service-card data.
- The article stream is paginated and heterogeneous; weather is a single top section; service-card placement is sticky once assigned.
- Only articles are saveable. Saved article data is persisted locally and supports an undo window when unsaved.
- Loading, empty, source-scoped error, offline, back-online, pagination-end, and pagination-error states are explicit requirements.
- The freshness policy, local database as the source of truth, and source-specific data handling are defined in `docs/USE_CASES.md` and `DECISIONS.md`.
- The app must respect Android system Back, window insets, 48 dp minimum touch targets, Material 3 semantics, and user font-scale settings.

## Brand Commitments

- External reference screens are functional references only. They describe content categories and rough flows; they are not visual authority for color, typography, layout, spacing, card treatment, or navigation.
- The visual system must be designed as an original, reusable Material 3 theme for this product rather than copied from external reference screens or another named app.
- No existing brand assets or approved brand palette were supplied. Do not invent brand claims, source endorsements, or content provenance beyond the real data sources.

## Evidence on Hand

- Product requirements and behavior scenarios are in `docs/SPEC.md` and `docs/USE_CASES.md`.
- Existing architecture and source decisions are in `docs/SPEC.md` and `DECISIONS.md`.
- The app currently contains only the Android Compose scaffold and example tests; no production UI or established visual system exists yet.

## Product Principles

- Make mixed content legible: different source types may feel distinct, but their hierarchy and actions must remain predictable.
- Make freshness honest: show cached content immediately and keep refresh policy separate from the data users read.
- Make offline behavior dependable: saved article text and local images are more important than a decorative online-only state.
- Prefer native Android behavior: Material 3 components, system navigation, accessible semantics, and predictable touch targets.
- Keep engineering judgment visible: each implementation slice starts with a local style contract, use-case-derived tests, and an explicit verification gate.

## Accessibility & Inclusion

The app targets Android phones first and must remain usable with system font scaling, screen-reader semantics, adequate contrast, and 48 dp touch targets. Light and dark semantic roles remain aligned in the token contract, while full dark-theme implementation is deferred until after the core release. Detailed accessibility verification is a required part of the UI completion pass even though UI tests are secondary to unit tests for this project.
