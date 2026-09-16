# AI Usage

## The process

The whole project ran through Claude Code on a document-first loop: extract the brief into a spec, turn the spec into Gherkin `Given/When/Then` use cases written specifically so each scenario could become a test case, derive a bottom-up task plan from those use cases, and then for every task write a task-scoped style contract, write the unit tests from the mapped scenarios, implement, verify, and commit behind a green gate. A separate reviewer model was run against each task branch before merge, which is what the repeated `fix: address code-review findings` commits are. Codex was also used during the spec and use-case iteration as a second opinion.

My role was to own the requirements and the boundaries. I decided what the product does and does not do, which suggestions survived, and what got cut; the AI drafted, and I kept, changed, or rejected the drafts. Several times I sent work back rather than patching it.

## What I rejected or rewrote

- **The pagination cursor rested on an assumption nobody had checked.** The proposal derived the next page offset from `articleDao.count()`. That silently assumes the API's ordering never changes and is always newest-first, so I had it call the API instead of reasoning about it. Ordering was stable, but `id` does not track sort order and `published_at` carries `1970-01-01` placeholders — the sort key was not trustworthy. It was redesigned into two separate mechanisms: a persisted cursor advanced only by user-triggered pagination, and a freshness check that always reads `offset=0` and compares against the newest known `id`.
- **Calling the API also destroyed a premise a batch of scenarios rested on.** Spaceflight News has no "full article body" — the single-article endpoint returns the same fields as the list. Five scenarios written around fetching a body on the detail screen were deleted and replaced with an in-app summary plus an external link. Sampling deep pagination showed 100% of those articles carry a placeholder date and no authors, which turned two invented edge cases into real, required behavior.
- **A redundant sort field.** A monotonic `feedPosition` had been designed onto every feed item. Articles already have a stable key and the list only grows at its two ends, so I asked whether it was needed at all and dropped it; only service cards needed extra placement information.
- **A shortcut that had no delete path.** Relying on Coil's disk cache for saved-article images was rejected because nothing would remove them on unsave. Copying the file into app-internal storage ties its lifetime 1:1 to the saved row, so storage equals the saved count and no eviction logic is needed.
- **An enum that would have to be edited forever.** A `FreshnessSource` enum meant every new source required editing one shared file; it became a plain string key plus a `Duration`.

## What I accepted, and where it pushed back on me

Review passes found real defects I would not have found by reading: an undo window whose persisted deadline outlived its own cleanup sweep, a placement primary key missing `contentType`, and a missing DataStore corruption handler that would have blocked cache-first startup. It also rejected one of my own fixes as incomplete and showed the existing tests could not detect the bug at all, which is why that fix was verified by reverting the production code to confirm the new tests actually fail.

It also argued against me twice and was right both times: when I suggested mocking all three data sources to save time, it pointed out that this removes any demonstration of a real network layer; and when I proposed dropping the "a placed card never moves" requirement, it showed the requirement descends from not disrupting the user's reading position, so removing it would only move the problem. Both stayed.
