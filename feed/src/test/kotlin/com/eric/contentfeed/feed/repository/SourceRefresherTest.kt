package com.eric.contentfeed.feed.repository

import com.eric.contentfeed.core.freshness.FreshnessGate
import com.eric.contentfeed.feed.data.remote.RemoteResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class SourceRefresherTest {
    @Test
    fun concurrentFreshnessChecksOnlyFetchOneSource() =
        runTest {
            val freshnessGate = mockk<FreshnessGate>(relaxed = true)
            coEvery { freshnessGate.isStale("articles", 1.minutes) } returnsMany listOf(true, false)
            val fetchStarted = CompletableDeferred<Unit>()
            val releaseFetch = CompletableDeferred<Unit>()
            val refresher = SourceRefresher(freshnessGate)

            val first =
                async {
                    refresher.refreshIfNeeded(
                        key = "articles",
                        ttl = 1.minutes,
                        bypassFreshness = false,
                        fetch = {
                            fetchStarted.complete(Unit)
                            releaseFetch.await()
                            RemoteResult.Loaded(42)
                        },
                        persist = {},
                    )
                }
            fetchStarted.await()

            val second =
                async {
                    refresher.refreshIfNeeded(
                        key = "articles",
                        ttl = 1.minutes,
                        bypassFreshness = false,
                        fetch = { RemoteResult.Loaded(43) },
                        persist = {},
                    )
                }
            runCurrent()
            assertFalse(second.isCompleted)

            releaseFetch.complete(Unit)

            assertEquals(RefreshOutcome.Succeeded, first.await())
            assertEquals(RefreshOutcome.Skipped, second.await())
            coVerify(exactly = 1) { freshnessGate.markFetched("articles") }
        }
}
