# Use Cases

Back to [Spec](SPEC.md).

Written as Given/When/Then scenarios, grouped by feature, so they can double as the basis for later test cases (unit, instrumented, or UI).

## Feature: Feed browsing

```gherkin
Scenario: Feed displays a heterogeneous mix of content in a fixed structure
  Given the user has network connectivity
  When the user opens the app
  Then the feed shows a weather section at the top, once, above a single scrollable list
  And that list mixes article cards with service cards, a service card appearing after every 5 articles

Scenario: Scrolling loads more content
  Given the user is viewing the feed
  When the user scrolls to the bottom of the currently loaded list
  Then the app automatically loads and appends the next page of articles
  And the user does not need to tap a "load more" control
  And a service card is inserted at the same fixed interval within the newly-loaded articles
  And the weather section does not repeat or reappear

Scenario: Service cards are drawn in order from a fixed-size pool
  Given the service card pool consists of 20 products bundled with the app as a static local JSON file
  When each service card slot in the feed is filled
  Then it uses the next unused product from the pool, in order

Scenario: The service card pool cycles once exhausted
  Given the user has scrolled deep enough that more service card slots are needed than the pool holds
  When the next service card slot is filled
  Then the app reuses products from the start of the pool rather than leaving the slot empty or fetching more

Scenario: The service card interval counts articles cumulatively across pages
  Given the user has scrolled across multiple loaded pages of articles
  When the total number of articles shown so far, across all pages, reaches another multiple of 5
  Then a service card is inserted at that point
  And the count is not reset to zero at the start of each new page

Scenario: A service card's position, once set, does not move
  Given a service card has already been inserted at a position in the list
  When new articles are later added anywhere in the list (via pagination at the bottom, or a refresh prepending new articles at the top)
  Then that service card's position relative to its existing neighbors does not change
  And the running count that placed it is never recalculated or renumbered because of content added elsewhere in the list

Scenario: Prepended articles get their own fresh count, independent of the list already below them
  Given a refresh prepends new articles above the currently loaded list
  When the total number of newly prepended articles reaches a multiple of 5
  Then a new service card is inserted among the prepended articles, using the next unused product from the pool
  And this count starts from zero for the prepended batch — it does not continue the cumulative count that had already placed service cards further down the list
  And no already-placed service card's position, or the count that produced it, changes as a result

Scenario: A service card's position survives its neighboring article being pruned from the cache
  Given a service card is positioned after a specific article
  And that article is later removed from the local cache by the 7-day unsaved-article retention cleanup (see Feature: Feed freshness)
  When the feed is rendered again
  Then the service card still appears at the same relative position among the articles that remain
  And it does not disappear, move, or duplicate as a result of its former neighbor being removed
```

## Feature: Feed freshness

Applies to the two live network sources (weather, articles), each with its own staleness threshold reflecting how often its real-world data actually changes. Exact numbers below; the reasoning for picking them goes in `DECISIONS.md`. Service cards are excluded — they're a local bundled mock, not a live network source, so there's nothing to refetch or go stale (see Feature: Feed browsing for how the fixed local pool is used).

```gherkin
Scenario Outline: Cached content is refetched once it passes its freshness window
  Given the cached <source> data was last fetched <age>
  When the user opens or returns to the app
  Then the app <action>

  Examples:
    | source   | age                       | action                               |
    | weather  | more than 15 minutes ago  | refetches the weather data           |
    | weather  | less than 15 minutes ago  | shows the cached weather data as-is  |
    | articles | more than 1 hour ago      | refetches the article list           |
    | articles | less than 1 hour ago      | shows the cached article list as-is  |

Scenario: Manual pull-to-refresh bypasses the freshness window
  Given the user is viewing the feed, regardless of how recently weather or articles last refreshed
  When the user performs a manual pull-to-refresh
  Then weather and articles are refetched immediately
  And the service card pool is left as-is, since it isn't a live source

Scenario: Cached content is shown immediately on startup, independent of any refresh check
  Given the user has previously loaded the feed and cached content exists
  When the user opens the app
  Then the feed shows the cached content immediately, without waiting on a network response
  And a freshness check for weather and articles happens independently in the background
  And the feed updates in place if that check determines a refetch is needed

Scenario: New articles are added without disrupting the user's reading position
  Given the user has scrolled partway through the list
  When the article list refreshes and new articles are found
  Then the new articles are added at the top of the list
  And the articles and service cards the user was already viewing remain in place, unshifted

Scenario: Retention cleanup runs after the initial open/return refresh succeeds
  Given the local cache contains an unsaved article older than 7 days
  And the local cache contains a saved article older than 7 days
  And the local cache contains an unsaved article from within the last 7 days
  When the app opens or returns to the foreground, its freshness check triggers an article refresh, and that refresh completes successfully
  Then the expired unsaved article is removed from the local cache
  And the saved article is retained
  And the recent unsaved article is retained

Scenario: Failed article refresh does not prune the existing cache
  Given the local cache contains an unsaved article older than 7 days
  When the initial open/return article refresh fails
  Then the expired article remains available in the local cache
  And the cached feed can still be shown

Scenario: A manual pull-to-refresh or a reconnect refresh does not run retention cleanup
  Given the user is mid-session, already viewing feed content that may include articles older than 7 days
  When the user performs a manual pull-to-refresh, or connectivity is restored and triggers a reconnect refresh, and either completes successfully
  Then the article list is refetched and updated as usual
  And no cache rows are pruned as a result — retention cleanup only ever runs from the initial open/return freshness check, so it never removes an article the user could currently have scrolled to
```

## Feature: Item detail

```gherkin
Scenario: Opening an article shows its full detail
  Given the user is viewing the feed
  When the user taps an article card
  Then the app opens a detail view showing the article's summary, source, author, and published date, along with a "read full article" action

Scenario: Tapping an article's "read full article" action opens its source externally
  Given the user is viewing an article's detail view
  When the user taps "read full article"
  Then the app opens the article's original source page in an external browser or custom tab, not a further in-app screen

Scenario: Opening a service card shows its full detail
  Given the user is viewing the feed
  When the user taps a service card
  Then the app opens a detail view showing the product's full description, image, price, and its target action

Scenario: Tapping a service card's target action opens an external link
  Given the user is viewing a service card, in the feed or its detail view
  When the user taps the card's target action (its CTA)
  Then the app opens that action's link in an external browser or custom tab, not the in-app detail view
  And this is a separate interaction from tapping elsewhere on the card, which opens the in-app detail view

Scenario: Returning from detail keeps the same items in view
  Given the user opened an item's detail view from partway down the feed
  When the user navigates back
  Then the feed shows the same items the user was looking at before, at the same scroll position

Scenario: An article's detail view shows a loading state for its image
  Given the user opens an article's detail view whose image has not finished loading
  When the image is still being fetched
  Then a loading placeholder is shown in place of the image, without blocking the rest of the detail content (the text is already in hand; only the image is a network dependency)

Scenario: An article's detail view shows a placeholder if its image fails to load
  Given the user opens an article's detail view whose image fails to load
  When the image load fails
  Then a placeholder image is shown instead of a broken image or blank space, without blocking the rest of the detail content
```

Service cards have no equivalent scenario: their images are bundled as local app assets (see `DECISIONS.md`), not loaded over the network, so there is no loading or failure state to handle at the detail-view level.

Weather has no separate detail view: its current conditions and forecast are shown directly in its feed card, and tapping it does nothing further (see `DECISIONS.md`).

## Feature: Save for later

Applies to articles only. Weather and service cards have no save action — see "Weather and service cards have no save action" below.

```gherkin
Scenario: Weather and service cards have no save action
  Given the user is viewing the weather section or a service card, in the feed or its detail view
  When the user looks for a way to save it
  Then no save action is shown — weather is continuously-updating data with no fixed version to save, and a service card is a promotional entry with its own target action, not content to save for later reading

Scenario: Saving an article from the feed
  Given the user is viewing the feed
  When the user taps the save action on an article card
  Then the card's save indicator updates immediately
  And the article appears in the Saved list

Scenario: Saving an article from its detail view
  Given the user is viewing an article's detail view
  When the user taps the save action
  Then the save indicator updates immediately
  And the article appears in the Saved list

Scenario: A saved article is available in the Saved list
  Given the user has saved an article
  When the user opens the Saved list
  Then the saved article is present

Scenario: The Saved list orders articles by when they were saved
  Given the user has saved multiple articles at different times
  When the user opens the Saved list
  Then the most recently saved article appears first

Scenario: Saving an article while offline reuses its already-cached image
  Given the user is offline and viewing an article whose image was already loaded
  When the user saves that article
  Then the article is saved successfully
  And its already-cached image is copied to local storage without requiring network access

Scenario: Saving an article while offline when its image is no longer cached
  Given the user is offline and the article's image is not available in the local image cache
  When the user saves that article
  Then the article is saved successfully without a local image copy
  And no error is shown to the user

Scenario: Save state stays in sync between detail view and feed
  Given the user saves an article from its detail view
  When the user navigates back to the feed
  Then that article's card in the feed shows the save indicator as saved

Scenario: Save state stays in sync between the Saved list and the feed
  Given the user unsaves an article directly from the Saved list
  When the user returns to the feed and locates that same article
  Then its save indicator shows as not saved
```

## Feature: Unsave

Undo only applies to removals made from the Saved list. A feed or detail save toggle is a direct, immediate action — tapping the save action again re-saves the article, so a separate undo step would just duplicate the toggle itself.

```gherkin
Scenario: Unsaving an article from the feed or detail view
  Given the user has previously saved an article
  When the user taps the save action again from the feed or detail view
  Then the article's save indicator updates immediately
  And the article no longer appears in the Saved list
  And this is a direct toggle — tapping the save action again re-saves it, with no separate undo step

Scenario: Unsaving an article from the Saved list
  Given the user is viewing the Saved list
  When the user removes an article directly from the list
  Then the article disappears from the Saved list immediately

Scenario: Unsaving from the Saved list offers a brief undo option
  Given the user removes an article from the Saved list
  When the article disappears from the list
  Then the app shows an undo option for a few seconds
  And the article's local data and locally-copied image are not yet deleted

Scenario: Undoing an unsave restores the article
  Given the user just removed an article from the Saved list and the undo option is still showing
  When the user taps undo
  Then the article is restored to the Saved list
  And its previously-copied local image is restored without being re-downloaded

Scenario: Dismissing undo finalizes that removal
  Given the user removed an article from the Saved list
  When the user dismisses the undo option without tapping undo
  Then only that article's saved state and locally-copied image are deleted
  And its cached text remains as ordinary cached content subject to the 7-day retention rule

Scenario: Unsave is finalized once the undo window passes
  Given the user removed an article from the Saved list and did not tap undo
  When the undo window expires
  Then the article's saved state and locally-copied image are deleted
  And its cached text remains as ordinary cached content subject to the 7-day retention rule

Scenario: Reopening the app ends any pending undo window
  Given the user removed an article from the Saved list and the undo window has not yet expired
  When the user closes and reopens the app
  Then the pending removal is finalized on reopen, regardless of the remaining time
  And the article's cached text remains subject to the ordinary retention rule
  And an undo that the user can no longer see does not pin the row outside retention

Scenario: Multiple pending undos are independent
  Given the user has removed more than one article from the Saved list within the undo window
  When the user taps undo for one of them
  Then only that article is restored
  And any other pending removal continues counting down independently
```

## Feature: Offline access to saved items

Offline is indicated by the app-wide banner (see Feature: Connectivity) — not by anything specific to this screen. Only articles can be saved (see Feature: Save for later), so the Saved list only ever contains articles.

```gherkin
Scenario: Viewing the Saved list without a network connection
  Given the user has previously saved one or more articles
  And the device has no network connectivity
  When the user opens the Saved list
  Then all previously saved articles are shown

Scenario: Opening a saved article's detail while offline
  Given the user has previously saved an article
  And the device has no network connectivity
  When the user opens that article from the Saved list
  Then its text content is displayed
  And its locally-copied image is displayed if one was captured at save time, or a placeholder if it was saved without a local image copy — never a broken image
```

Its "read full article" action follows the app-wide rule in Feature: Connectivity.

## Feature: Connectivity

```gherkin
Scenario: An "offline" banner appears app-wide when connectivity is lost
  Given the user is using the app, on any screen, with content already loaded
  When network connectivity is lost
  Then the app shows an "offline" banner, regardless of which screen is currently shown
  And that screen continues to show its previously loaded content, unchanged

Scenario: Feed refreshes automatically when connectivity returns
  Given the user was offline and the feed was showing previously loaded content
  When network connectivity is restored
  Then any source whose cached data is now past its freshness window (see Feed freshness) is refetched
  And sources still within their freshness window are left as-is

Scenario: A "back online" banner appears app-wide when connectivity is restored
  Given the user was offline, on any screen
  When network connectivity is restored
  Then the app shows a brief "back online" banner, regardless of which screen is currently shown
  And the banner disappears on its own after a few seconds without any user action

Scenario: Saved articles are unaffected by a feed refresh
  Given the user has saved articles
  When the feed refreshes after connectivity returns
  Then the Saved list is unchanged

Scenario: External-link actions are inert while offline, wherever they appear
  Given the user is offline
  When the user taps an article's "read full article" action or a service card's target action, on any screen
  Then the action is shown as disabled or clearly inert instead of attempting to open an external link
```

## Feature: Loading, empty, and error states

```gherkin
Scenario: First launch shows a loading state
  Given the user opens the app for the first time
  And no cached content exists yet
  When the feed has not yet received its first page of content
  Then a loading state is shown instead of a blank screen

Scenario: Saved list is empty
  Given the user has never saved anything
  When the user opens the Saved list
  Then an explicit empty state is shown, not a blank screen

Scenario: A source returns successfully with no items on its first page
  Given a source (e.g. articles) completes its first request successfully but returns zero items
  When the user is viewing the feed
  Then that section shows an explicit empty state, distinct from a loading state, an error, or the end-of-pagination case (see Feature: Per-source data handling)

Scenario: A single failing data source does not break the rest of the feed
  Given the feed is loading content from multiple sources
  When one source (e.g. weather) fails to load
  Then the feed still displays content from the other sources
  And the failing source shows a scoped error instead of an app-wide failure

Scenario: A failed source can be retried
  Given a source failed to load and is showing a scoped error
  When the user retries (explicitly, or via the next automatic refresh)
  Then the app attempts to load that source again

Scenario: First launch with no network and nothing cached yet
  Given the user has never opened the app before
  And the device has no network connectivity
  When the user opens the app
  Then the app shows an explicit "nothing available offline yet" state
  And this state is distinct from both the ordinary loading state and a single-source error

Scenario: Loading the next page of articles fails
  Given the user has already scrolled through one or more pages of articles
  When the app fails to load the next page
  Then the articles already on screen remain visible
  And an inline retry control is shown at the bottom of the list instead of losing existing content

Scenario: A manual pull-to-refresh fails
  Given the user is viewing previously loaded feed content
  When the user performs a manual pull-to-refresh and it fails
  Then the existing content remains visible, unchanged
  And a brief error indication is shown instead of silently doing nothing or clearing the feed
```

## Feature: Per-source data handling

Beyond a source failing outright (covered above), each of the three integrated sources can return no data, partial data, or a source-specific error. These are distinct from a network-level failure and from the generic empty/error scenarios above.

### Articles (Spaceflight News)

The scenarios below reflect data quality issues actually observed on this source, not hypothetical edge cases.

```gherkin
Scenario: Reaching the end of the article list stops pagination gracefully
  Given the user has scrolled through all available articles
  When the app requests the next page and the source returns no further articles
  Then the app stops requesting more pages
  And no error or endless loading indicator is shown at the bottom of the list

Scenario: An article with a missing image shows a placeholder
  Given an article's `image_url` is empty in the source response
  When that article is rendered in the feed
  Then a placeholder image is shown instead of a broken image or blank space

Scenario: An article with a missing summary still opens
  Given an article's `summary` field is empty in the source response
  When the user opens that article's detail view
  Then the detail view still shows the title, source, and "read full article" action
  And a neutral placeholder is shown in place of the summary, instead of an error or blank space

Scenario: An article with no authors omits the author line
  Given an article's `authors` array is empty in the source response (common on older entries in this source)
  When that article's detail view is rendered
  Then the author line is omitted instead of showing blank space or a broken label

Scenario: An article with a placeholder published date shows a neutral fallback
  Given an article's `published_at` is a sentinel value indicating no real date was recorded (e.g. `1970-01-01T00:00:00Z`, seen on older entries in this source)
  When that article is rendered in the feed or its detail view
  Then a neutral "date unknown" label is shown instead of a nonsensical or misleading timestamp
```

### Weather (Open-Meteo)

No missing or null value has actually been observed on this source, so per-field defensive handling isn't attempted. An unrecognized weather code is different in kind: Open-Meteo's WMO code table can grow codes this app's mapping doesn't yet know about, so this is a structural gap rather than a hypothetical one.

```gherkin
Scenario: An unrecognized weather code falls back to a neutral icon and label
  Given the current or a daily `weather_code` value (a WMO numeric code) is not one this app's icon/label mapping recognizes
  When the weather section renders that value
  Then a neutral fallback icon and label (e.g. "Unknown conditions") are shown instead of a crash or a blank icon
```

### Service cards (local mock, seeded from DummyJSON)

The live DummyJSON catalog has no products with missing fields, and this app's bundled snapshot is authored from that catalog, so per-field defensive handling for an individual product isn't attempted. The one case worth guarding is the pool file itself being malformed.

```gherkin
Scenario: A malformed entry in the bundled product pool is skipped, not crashed on
  Given the bundled service-card pool JSON contains an entry that fails to parse into the expected product shape
  When the app loads the pool
  Then that entry is skipped
  And the remaining valid entries in the pool are loaded and used normally
```
