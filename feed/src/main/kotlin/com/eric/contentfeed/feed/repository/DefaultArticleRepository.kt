package com.eric.contentfeed.feed.repository

import com.eric.contentfeed.core.freshness.EpochClock
import com.eric.contentfeed.core.freshness.FreshnessGate
import com.eric.contentfeed.feed.data.local.FeedCursorStore
import com.eric.contentfeed.feed.data.local.FeedLocalDataSource
import com.eric.contentfeed.feed.data.remote.ArticlePage
import com.eric.contentfeed.feed.data.remote.ArticleRemoteDataSource
import com.eric.contentfeed.feed.data.remote.RemoteResult
import com.eric.contentfeed.feed.domain.composition.ServiceCardPlacementAssigner
import com.eric.contentfeed.feed.domain.model.CachedArticle
import com.eric.contentfeed.feed.domain.model.FeedPlacement
import com.eric.contentfeed.feed.domain.model.SERVICE_CARD_CONTENT_TYPE
import com.eric.contentfeed.feed.domain.model.SourceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultArticleRepository(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: ArticleRemoteDataSource,
    private val cursorStore: FeedCursorStore,
    freshnessGate: FreshnessGate,
    private val clock: EpochClock,
) : ArticleRepository {
    private val refresher = SourceRefresher(freshnessGate)

    private val mutableRefreshStatus = MutableStateFlow<SourceStatus>(SourceStatus.Idle)
    override val refreshStatus: StateFlow<SourceStatus> = mutableRefreshStatus.asStateFlow()

    private val mutableAppendStatus = MutableStateFlow<SourceStatus>(SourceStatus.Idle)
    override val appendStatus: StateFlow<SourceStatus> = mutableAppendStatus.asStateFlow()

    private val mutableIsLastPage = MutableStateFlow(false)
    override val isLastPage: StateFlow<Boolean> = mutableIsLastPage.asStateFlow()

    // Guards every local read-modify-write against the placement counters and the
    // pagination cursor — refreshTop (reconnect) and loadNextPage (scroll) can be
    // invoked concurrently on this same singleton, and neither is safe to interleave
    // with the other's local mutation. Network fetches happen outside the lock so a
    // stalled fetch on one path never blocks the other.
    private val mutationMutex = Mutex()

    override fun observeArticles(): Flow<List<CachedArticle>> = localDataSource.observeArticles()

    override fun observePlacements(): Flow<List<FeedPlacement>> = localDataSource.observePlacements()

    override suspend fun refreshTop(bypassFreshness: Boolean): RefreshOutcome {
        mutableRefreshStatus.value = SourceStatus.Loading
        val outcome =
            refresher.refreshIfNeeded(
                key = FeedPolicy.ARTICLES_FRESHNESS_KEY,
                ttl = FeedPolicy.ARTICLES_TTL,
                bypassFreshness = bypassFreshness,
                fetch = { remoteDataSource.fetchPage(offset = 0, limit = FeedPolicy.ARTICLE_PAGE_SIZE) },
                persist = ::persistTopPage,
            )
        mutableRefreshStatus.value =
            when (outcome) {
                RefreshOutcome.Skipped, RefreshOutcome.Succeeded -> SourceStatus.Ready
                is RefreshOutcome.Failed -> SourceStatus.Failed(outcome.cause)
            }
        return outcome
    }

    /** Discovers new articles by comparing stable ids against the cache — never by
     * comparing counts or deriving an offset from Room, which would break as soon as
     * retention pruning shrinks the cache independent of pagination. */
    private suspend fun persistTopPage(page: ArticlePage) =
        mutationMutex.withLock {
            val existingIds = localDataSource.observeArticles().first().mapTo(HashSet()) { it.article.id }
            localDataSource.upsertArticles(page.articles, clock.nowEpochMillis())
            assignPlacements()

            val newArticleCount = page.articles.count { it.id !in existingIds }
            cursorStore.writeNextOffset(cursorStore.readNextOffset() + newArticleCount)
            // Unconditional, not just on true: the stream can un-exhaust itself (grows
            // past the last-seen boundary between refreshes), and a stale true here
            // would permanently stick loadNextPage() as a no-op.
            mutableIsLastPage.value = page.isLastPage
        }

    override suspend fun loadNextPage() {
        if (mutableIsLastPage.value) return
        mutableAppendStatus.value = SourceStatus.Loading
        // Read outside the lock only to pick an offset for the network request — the
        // authoritative read-modify-write on commit happens inside the lock below, so
        // a concurrent refreshTop can't be silently overwritten by a stale offset.
        val offset = cursorStore.readNextOffset()
        when (val result = remoteDataSource.fetchPage(offset = offset, limit = FeedPolicy.ARTICLE_PAGE_SIZE)) {
            is RemoteResult.Loaded -> {
                val page = result.value
                mutationMutex.withLock {
                    localDataSource.upsertArticles(page.articles, clock.nowEpochMillis())
                    assignPlacements()
                    // Relative, not offset + page.articles.size: offset was read
                    // before the network call and the lock, so a concurrent
                    // refreshTop's cursor advance in between must not be clobbered.
                    cursorStore.writeNextOffset(cursorStore.readNextOffset() + page.articles.size)
                    mutableIsLastPage.value = page.isLastPage
                }
                mutableAppendStatus.value = SourceStatus.Ready
            }
            is RemoteResult.Failure -> {
                // Cursor and cache are untouched, so the articles already on screen
                // stay visible and a retry re-requests the same offset.
                mutableAppendStatus.value = SourceStatus.Failed(result.cause)
            }
        }
    }

    private suspend fun assignPlacements() {
        val articles = localDataSource.observeArticles().first().map { it.article }
        val existingPlacements = localDataSource.observePlacements().first()
        val newPlacements =
            ServiceCardPlacementAssigner.assign(
                articles = articles,
                existingPlacements = existingPlacements,
                contentType = SERVICE_CARD_CONTENT_TYPE,
                interval = FeedPolicy.SERVICE_CARD_INTERVAL,
                nextPoolIndex = localDataSource.nextPlacementPoolIndex(SERVICE_CARD_CONTENT_TYPE),
                nextAssignmentSequence = localDataSource.nextPlacementAssignmentSequence(),
            )
        newPlacements.forEach { localDataSource.insertPlacement(it) }
    }

    override suspend fun pruneStaleUnsavedArticles() {
        mutationMutex.withLock {
            val cutoff = clock.nowEpochMillis() - FeedPolicy.UNSAVED_ARTICLE_RETENTION.inWholeMilliseconds
            localDataSource.pruneUnsavedArticles(cutoff)

            // Sorted publishedAt DESC, id ASC (Room's own ordering), so the last
            // surviving entry is the oldest one left — the boundary below which any
            // placement is now orphaned (see FeedPlacementDao.deleteOrphanedBelow).
            val survivors = localDataSource.observeArticles().first()
            val oldest = survivors.lastOrNull()?.article
            if (oldest == null) {
                localDataSource.deleteAllPlacements()
            } else {
                localDataSource.deleteOrphanedPlacementsBelow(oldest.publishedAtEpochMillis, oldest.id)
            }
        }
    }
}
