package com.eric.contentfeed.feed.repository

import com.eric.contentfeed.core.freshness.EpochClock
import com.eric.contentfeed.core.freshness.FreshnessGate
import com.eric.contentfeed.feed.data.local.FeedCursorStore
import com.eric.contentfeed.feed.data.local.FeedLocalDataSource
import com.eric.contentfeed.feed.data.remote.ArticlePage
import com.eric.contentfeed.feed.data.remote.ArticleRemoteDataSource
import com.eric.contentfeed.feed.data.remote.RemoteResult
import com.eric.contentfeed.feed.domain.model.Article
import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.eric.contentfeed.feed.domain.model.SourceStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DefaultArticleRepositoryTest {
    private lateinit var localDataSource: FeedLocalDataSource
    private lateinit var remoteDataSource: ArticleRemoteDataSource
    private lateinit var cursorStore: FeedCursorStore
    private lateinit var freshnessGate: FreshnessGate
    private lateinit var repository: DefaultArticleRepository
    private val clock = EpochClock { 1_700_000_000_000L }

    @Before
    fun setUp() {
        localDataSource = mockk(relaxed = true)
        remoteDataSource = mockk()
        cursorStore = mockk(relaxed = true)
        freshnessGate = mockk(relaxed = true)
        repository = DefaultArticleRepository(localDataSource, remoteDataSource, cursorStore, freshnessGate, clock)
    }

    @Test
    fun cachedArticlesAreEmittedWithoutWaitingOnNetwork() =
        runTest {
            val cached = cachedArticles(1..3)
            every { localDataSource.observeArticles() } returns flowOf(cached)

            assertEquals(cached, repository.observeArticles().first())
            coVerify(exactly = 0) { remoteDataSource.fetchPage(any(), any()) }
        }

    @Test
    fun freshCacheSkipsRefetch() =
        runTest {
            coEvery { freshnessGate.isStale(FeedPolicy.ARTICLES_FRESHNESS_KEY, FeedPolicy.ARTICLES_TTL) } returns false

            val outcome = repository.refreshTop(bypassFreshness = false)

            assertEquals(RefreshOutcome.Skipped, outcome)
            coVerify(exactly = 0) { remoteDataSource.fetchPage(any(), any()) }
        }

    @Test
    fun staleCacheRefetchesAndMarksFetched() =
        runTest {
            coEvery { freshnessGate.isStale(any(), any()) } returns true
            coEvery { remoteDataSource.fetchPage(0, FeedPolicy.ARTICLE_PAGE_SIZE) } returns
                RemoteResult.Loaded(ArticlePage(emptyList(), isLastPage = true))
            every { localDataSource.observeArticles() } returns flowOf(emptyList())
            every { localDataSource.observePlacements() } returns flowOf(emptyList())

            val outcome = repository.refreshTop(bypassFreshness = false)

            assertEquals(RefreshOutcome.Succeeded, outcome)
            coVerify { freshnessGate.markFetched(FeedPolicy.ARTICLES_FRESHNESS_KEY) }
        }

    @Test
    fun manualBypassFetchesRegardlessOfFreshness() =
        runTest {
            coEvery { freshnessGate.isStale(any(), any()) } returns false
            coEvery { remoteDataSource.fetchPage(0, FeedPolicy.ARTICLE_PAGE_SIZE) } returns
                RemoteResult.Loaded(ArticlePage(emptyList(), isLastPage = true))
            every { localDataSource.observeArticles() } returns flowOf(emptyList())
            every { localDataSource.observePlacements() } returns flowOf(emptyList())

            val outcome = repository.refreshTop(bypassFreshness = true)

            assertEquals(RefreshOutcome.Succeeded, outcome)
            coVerify { remoteDataSource.fetchPage(0, FeedPolicy.ARTICLE_PAGE_SIZE) }
        }

    @Test
    fun newlyDiscoveredArticlesAdvanceTheCursorAndAssignPlacementsWithoutDisturbingExistingRows() =
        runTest {
            // 5 articles (6..10) already cached; the top page now also returns 5 new
            // ones (1..5) prepended ahead of them.
            coEvery { freshnessGate.isStale(any(), any()) } returns true
            coEvery { remoteDataSource.fetchPage(0, FeedPolicy.ARTICLE_PAGE_SIZE) } returns
                RemoteResult.Loaded(ArticlePage(articles(1..10), isLastPage = false))
            every { localDataSource.observeArticles() } returnsMany
                listOf(flowOf(cachedArticles(6..10)), flowOf(cachedArticles(1..10)))
            every { localDataSource.observePlacements() } returns flowOf(emptyList())
            coEvery { localDataSource.nextPlacementPoolIndex(any()) } returns 0
            coEvery { localDataSource.nextPlacementAssignmentSequence() } returns 0L
            coEvery { cursorStore.readNextOffset() } returns 0

            val outcome = repository.refreshTop(bypassFreshness = false)

            assertEquals(RefreshOutcome.Succeeded, outcome)
            // upsertArticles never deletes — existing rows 6..10 are only touched by
            // the update-in-place upsert, never removed, so nothing already on screen
            // moves or disappears.
            coVerify { localDataSource.upsertArticles(articles(1..10), clock.nowEpochMillis()) }
            // 5 new ids (1..5) discovered against the pre-upsert cache -> cursor
            // advances by 5, not derived from any row count.
            coVerify { cursorStore.writeNextOffset(5) }
            coVerify { localDataSource.insertPlacement(match { it.anchorArticleId == 5 }) }
            coVerify { localDataSource.insertPlacement(match { it.anchorArticleId == 10 }) }
        }

    @Test
    fun isLastPageClearsWhenALaterRefreshDiscoversTheStreamHasGrownPastIt() =
        runTest {
            // First refresh: the whole (small) stream fits in one page -> isLastPage
            // = true. Second refresh: the stream has since grown past a full page of
            // brand-new articles -> isLastPage = false. A bug that only ever assigns
            // `true` (never `false`) would leave the flag stuck, permanently
            // short-circuiting loadNextPage() below.
            coEvery { freshnessGate.isStale(any(), any()) } returns true
            coEvery { remoteDataSource.fetchPage(0, FeedPolicy.ARTICLE_PAGE_SIZE) } returnsMany
                listOf(
                    RemoteResult.Loaded(ArticlePage(articles(1..10), isLastPage = true)),
                    RemoteResult.Loaded(ArticlePage(articles(100..119), isLastPage = false)),
                )
            every { localDataSource.observeArticles() } returnsMany
                listOf(
                    flowOf(emptyList()),
                    flowOf(cachedArticles(1..10)),
                    flowOf(cachedArticles(1..10)),
                    flowOf(cachedArticles(1..10) + cachedArticles(100..119)),
                )
            every { localDataSource.observePlacements() } returns flowOf(emptyList())

            repository.refreshTop(bypassFreshness = false)
            assertEquals(true, repository.isLastPage.value)

            repository.refreshTop(bypassFreshness = false)
            assertEquals(false, repository.isLastPage.value)

            repository.loadNextPage()

            // 2 top refreshes + 1 append fetch only happens if loadNextPage() actually
            // reached the network instead of returning early on a stale `true`.
            coVerify(exactly = 3) { remoteDataSource.fetchPage(any(), any()) }
        }

    @Test
    fun failedTopRefreshDoesNotMarkFetchedOrTouchTheCache() =
        runTest {
            coEvery { freshnessGate.isStale(any(), any()) } returns true
            coEvery { remoteDataSource.fetchPage(0, FeedPolicy.ARTICLE_PAGE_SIZE) } returns
                RemoteResult.Failure(RemoteFailure.Http(500))

            val outcome = repository.refreshTop(bypassFreshness = false)

            assertEquals(RefreshOutcome.Failed(RemoteFailure.Http(500)), outcome)
            assertEquals(SourceStatus.Failed(RemoteFailure.Http(500)), repository.refreshStatus.value)
            coVerify(exactly = 0) { freshnessGate.markFetched(any()) }
            coVerify(exactly = 0) { localDataSource.upsertArticles(any(), any()) }
        }

    @Test
    fun loadNextPageAppendsAndSetsLastPageFromTheResponse() =
        runTest {
            coEvery { cursorStore.readNextOffset() } returns 20
            coEvery { remoteDataSource.fetchPage(20, FeedPolicy.ARTICLE_PAGE_SIZE) } returns
                RemoteResult.Loaded(ArticlePage(articles(21..25), isLastPage = true))
            every { localDataSource.observeArticles() } returns flowOf(cachedArticles(1..25))
            every { localDataSource.observePlacements() } returns flowOf(emptyList())

            repository.loadNextPage()

            assertEquals(SourceStatus.Ready, repository.appendStatus.value)
            assertEquals(true, repository.isLastPage.value)
            coVerify { cursorStore.writeNextOffset(25) }
        }

    @Test
    fun loadNextPageIsANoOpOnceTheLastPageIsReached() =
        runTest {
            coEvery { cursorStore.readNextOffset() } returns 20
            coEvery { remoteDataSource.fetchPage(20, any()) } returns
                RemoteResult.Loaded(ArticlePage(emptyList(), isLastPage = true))
            every { localDataSource.observeArticles() } returns flowOf(emptyList())
            every { localDataSource.observePlacements() } returns flowOf(emptyList())

            repository.loadNextPage()
            repository.loadNextPage()

            coVerify(exactly = 1) { remoteDataSource.fetchPage(any(), any()) }
        }

    @Test
    fun failedPageAppendLeavesCursorAndCacheUntouchedForRetry() =
        runTest {
            coEvery { cursorStore.readNextOffset() } returns 20
            coEvery { remoteDataSource.fetchPage(20, any()) } returns
                RemoteResult.Failure(RemoteFailure.NetworkUnavailable)

            repository.loadNextPage()

            assertEquals(SourceStatus.Failed(RemoteFailure.NetworkUnavailable), repository.appendStatus.value)
            coVerify(exactly = 0) { cursorStore.writeNextOffset(any()) }
            coVerify(exactly = 0) { localDataSource.upsertArticles(any(), any()) }
        }

    @Test
    fun pruneStaleUnsavedArticlesDeletesPlacementsOrphanedBelowTheOldestSurvivor() =
        runTest {
            every { localDataSource.observeArticles() } returns flowOf(cachedArticles(1..5))

            repository.pruneStaleUnsavedArticles()

            coVerify { localDataSource.pruneUnsavedArticles(any()) }
            coVerify { localDataSource.deleteOrphanedPlacementsBelow(-5L, 5) }
        }

    @Test
    fun pruneStaleUnsavedArticlesDeletesAllPlacementsWhenNoArticlesSurvive() =
        runTest {
            every { localDataSource.observeArticles() } returns flowOf(emptyList())

            repository.pruneStaleUnsavedArticles()

            coVerify { localDataSource.deleteAllPlacements() }
            coVerify(exactly = 0) { localDataSource.deleteOrphanedPlacementsBelow(any(), any()) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun concurrentRefreshAndAppendDoNotLoseEachOthersCursorAdvance() =
        runTest {
            // A real fake, not a mock: the bug this guards against is an absolute
            // cursor write computed from a value read before the network round trip,
            // which mockk's relaxed stubbing can't reproduce — only a real mutable
            // `offset` can show one write clobbering the other.
            val fakeCursorStore = FakeCursorStore()
            val gatedRemote = GatedAppendRemoteDataSource()
            coEvery { freshnessGate.isStale(any(), any()) } returns true
            every { localDataSource.observeArticles() } returns flowOf(emptyList())
            every { localDataSource.observePlacements() } returns flowOf(emptyList())

            val racyRepository =
                DefaultArticleRepository(localDataSource, gatedRemote, fakeCursorStore, freshnessGate, clock)

            // The append reads the cursor and issues its fetch, then blocks on the
            // gate before it can commit anything — exactly the window in which a
            // concurrent refresh's own cursor advance must not be lost.
            val appendJob = launch { racyRepository.loadNextPage() }
            runCurrent()

            racyRepository.refreshTop(bypassFreshness = false)

            gatedRemote.appendGate.complete(Unit)
            advanceUntilIdle()
            appendJob.join()

            // Refresh discovered 5 new articles, append fetched 5 more — both
            // contributions must land regardless of interleaving. An absolute
            // (non-relative) append write would instead clobber the refresh's commit
            // and leave this at 5.
            assertEquals(10, fakeCursorStore.offset)
        }

    private class FakeCursorStore : FeedCursorStore {
        var offset: Int = 0

        override suspend fun readNextOffset(): Int = offset

        override suspend fun writeNextOffset(offset: Int) {
            this.offset = offset
        }
    }

    private inner class GatedAppendRemoteDataSource : ArticleRemoteDataSource {
        val appendGate = CompletableDeferred<Unit>()
        private var calls = 0

        override suspend fun fetchPage(
            offset: Int,
            limit: Int,
        ): RemoteResult<ArticlePage> {
            calls++
            return if (calls == 1) {
                appendGate.await()
                RemoteResult.Loaded(ArticlePage(articles(6..10), isLastPage = false))
            } else {
                RemoteResult.Loaded(ArticlePage(articles(1..5), isLastPage = false))
            }
        }
    }

    private fun articles(ids: IntRange): List<Article> = ids.map(::article)

    private fun article(id: Int) =
        Article(
            id = id,
            title = "Article $id",
            source = "Source",
            authors = emptyList(),
            summary = null,
            imageUrl = null,
            articleUrl = "https://example.com/$id",
            publishedAtEpochMillis = -id.toLong(),
        )

    private fun cachedArticles(ids: IntRange): List<CachedArticle> = ids.map(::article).map(::toCachedArticle)

    private fun toCachedArticle(article: Article) =
        CachedArticle(
            article = article,
            isSaved = false,
            savedAtEpochMillis = null,
            localImagePath = null,
            pendingUnsaveAtEpochMillis = null,
        )
}
