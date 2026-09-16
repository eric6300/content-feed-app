package com.eric.contentfeed.feed.repository

import com.eric.contentfeed.core.freshness.EpochClock
import com.eric.contentfeed.feed.data.local.FakeSavedImageStore
import com.eric.contentfeed.feed.data.local.FeedLocalDataSource
import com.eric.contentfeed.feed.domain.model.Article
import com.eric.contentfeed.feed.domain.model.CachedArticle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSavedArticleRepositoryTest {
    private lateinit var localDataSource: FeedLocalDataSource
    private lateinit var imageStore: FakeSavedImageStore
    private lateinit var repository: DefaultSavedArticleRepository
    private val clock = EpochClock { NOW }

    @Before
    fun setUp() {
        localDataSource = mockk(relaxed = true)
        imageStore = FakeSavedImageStore("files/saved_images/1.img")
        repository = DefaultSavedArticleRepository(localDataSource, imageStore, clock)
        every { localDataSource.observeArticle(1) } returns flowOf(cachedArticle(1))
        every { localDataSource.observeArticle(2) } returns flowOf(cachedArticle(2))
    }

    @Test
    fun saveWritesTheSavedRowBeforeStartingImageWorkAndAttachesOnlyThePath() =
        runTest {
            val events = mutableListOf<String>()
            coEvery { localDataSource.saveArticle(1, NOW, null) } answers {
                events += "save"
                true
            }
            imageStore.onCopy = { _, _ ->
                events += "copy"
                imageStore.copiedPath
            }
            coEvery { localDataSource.attachLocalImagePath(1, imageStore.copiedPath!!) } answers {
                events += "attach"
                true
            }

            repository.saveArticle(1)

            assertEquals(listOf("save", "copy", "attach"), events)
            coVerify(exactly = 1) { localDataSource.saveArticle(1, NOW, null) }
            coVerify(exactly = 1) { localDataSource.attachLocalImagePath(1, imageStore.copiedPath!!) }
        }

    @Test
    fun saveSucceedsWhenTheImageIsNotCached() =
        runTest {
            coEvery { localDataSource.saveArticle(1, NOW, null) } returns true
            imageStore.copiedPath = null

            repository.saveArticle(1)

            assertTrue(imageStore.copyCalls.single() == (1 to "https://example.com/1.jpg"))
            coVerify(exactly = 0) { localDataSource.attachLocalImagePath(any(), any()) }
            assertEquals(emptyList<Int>(), imageStore.deleteCalls)
        }

    @Test
    fun saveDoesNotAskTheNetworkForAnImage() =
        runTest {
            coEvery { localDataSource.saveArticle(1, NOW, null) } returns true

            repository.saveArticle(1)

            assertEquals(1, imageStore.copyCalls.size)
        }

    @Test
    fun aCopyThatLandsAfterImmediateUnsaveIsDiscardedAndDeleted() =
        runTest {
            val copyStarted = CompletableDeferred<Unit>()
            val releaseCopy = CompletableDeferred<Unit>()
            coEvery { localDataSource.saveArticle(1, NOW, null) } returns true
            imageStore.onCopy = { _, _ ->
                copyStarted.complete(Unit)
                releaseCopy.await()
                imageStore.copiedPath
            }
            coEvery { localDataSource.attachLocalImagePath(1, imageStore.copiedPath!!) } returns false

            val saveJob = launch { repository.saveArticle(1) }
            copyStarted.await()
            val unsaveJob = launch { repository.unsaveArticleImmediately(1) }
            runCurrent()
            releaseCopy.complete(Unit)
            saveJob.join()
            unsaveJob.join()

            assertEquals(listOf(1, 1), imageStore.deleteCalls)
            coVerify(exactly = 1) { localDataSource.unsaveArticleImmediately(1) }
        }

    @Test
    fun aCopyLandingDuringPendingRemovalAttachesSoUndoCanRestoreIt() =
        runTest {
            val copyStarted = CompletableDeferred<Unit>()
            val releaseCopy = CompletableDeferred<Unit>()
            coEvery { localDataSource.saveArticle(1, NOW, null) } returns true
            coEvery {
                localDataSource.markPendingUnsave(
                    1,
                    NOW + FeedPolicy.PERSISTED_UNDO_WINDOW.inWholeMilliseconds,
                )
            } returns
                true
            coEvery { localDataSource.attachLocalImagePath(1, imageStore.copiedPath!!) } returns true
            imageStore.onCopy = { _, _ ->
                copyStarted.complete(Unit)
                releaseCopy.await()
                imageStore.copiedPath
            }

            val saveJob = launch { repository.saveArticle(1) }
            copyStarted.await()
            assertTrue(repository.removeFromSavedList(1))
            releaseCopy.complete(Unit)
            saveJob.join()

            coVerify(exactly = 1) { localDataSource.attachLocalImagePath(1, imageStore.copiedPath!!) }
            assertEquals(emptyList<Int>(), imageStore.deleteCalls)
        }

    @Test
    fun immediateUnsaveClearsTheRowThenDeletesItsImage() =
        runTest {
            repository.unsaveArticleImmediately(1)

            coVerify { localDataSource.unsaveArticleImmediately(1) }
            assertEquals(listOf(1), imageStore.deleteCalls)
        }

    @Test
    fun savedListRemovalDoesNotTouchTheImageFile() =
        runTest {
            coEvery { localDataSource.markPendingUnsave(1, NOW + 6_000L) } returns true

            assertTrue(repository.removeFromSavedList(1))

            assertEquals(emptyList<Int>(), imageStore.deleteCalls)
            assertEquals(emptyList<Int>(), imageStore.copyCalls)
        }

    @Test
    fun thePersistedUndoDeadlineIncludesTheGraceSecond() =
        runTest {
            coEvery { localDataSource.markPendingUnsave(1, NOW + 6_000L) } returns true

            repository.removeFromSavedList(1)

            coVerify { localDataSource.markPendingUnsave(1, NOW + 6_000L) }
        }

    @Test
    fun undoDoesNotTouchTheImageFile() =
        runTest {
            coEvery { localDataSource.undoUnsave(1, NOW) } returns true

            assertTrue(repository.undoRemoval(1))

            assertEquals(emptyList<Int>(), imageStore.deleteCalls)
            assertEquals(emptyList<Int>(), imageStore.copyCalls)
        }

    @Test
    fun expiredFinalizationDeletesOnlyReturnedRowsImages() =
        runTest {
            every { localDataSource.observeArticle(2) } returns flowOf(cachedArticle(2))
            coEvery { localDataSource.finalizeExpiredPendingUnsaves(NOW) } returns
                listOf(cachedArticle(1), cachedArticle(2))

            repository.finalizeExpiredRemovals()

            assertEquals(listOf(1, 2), imageStore.deleteCalls)
        }

    @Test
    fun restartFinalizationDeletesAllReturnedRowsRegardlessOfTheirDeadlines() =
        runTest {
            coEvery { localDataSource.finalizeAllPendingUnsaves() } returns
                listOf(cachedArticle(1), cachedArticle(2))

            repository.finalizeAllPendingRemovals()

            assertEquals(listOf(1, 2), imageStore.deleteCalls)
            coVerify(exactly = 1) { localDataSource.finalizeAllPendingUnsaves() }
        }

    private fun cachedArticle(id: Int) =
        CachedArticle(
            article =
                Article(
                    id = id,
                    title = "Article $id",
                    source = "Source",
                    authors = emptyList(),
                    summary = "Summary",
                    imageUrl = "https://example.com/$id.jpg",
                    articleUrl = "https://example.com/articles/$id",
                    publishedAtEpochMillis = 1_000L,
                ),
            isSaved = true,
            savedAtEpochMillis = NOW,
            localImagePath = "files/saved_images/$id.img",
            pendingUnsaveAtEpochMillis = null,
        )

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
