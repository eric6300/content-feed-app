package com.eric.contentfeed.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleDaoTest {
    private lateinit var database: ContentFeedDatabase
    private lateinit var dao: ArticleDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ContentFeedDatabase::class.java).build()
        dao = database.articleDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeArticlesOrdersPublishedDateDescendingThenIdAscending() =
        runTest {
            dao.upsertRemoteArticles(
                listOf(
                    article(id = 1, publishedAt = 100),
                    article(id = 2, publishedAt = 200),
                    article(id = 3, publishedAt = 100),
                ),
            )

            assertEquals(listOf(2, 1, 3), dao.observeArticles().first().map { it.id })
        }

    @Test
    fun nullPublishedAtRoundTripsAsDateUnknownSentinel() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1, publishedAt = null)))

            assertNull(dao.observeArticle(1).first()!!.publishedAtEpochMillis)
        }

    @Test
    fun remoteUpsertPreservesSavedStateAndLocalImage() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1, publishedAt = 100)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 300, localImagePath = "saved/image.jpg")

            dao.upsertRemoteArticles(
                listOf(article(id = 1, publishedAt = 400, title = "Updated title")),
            )

            val result = dao.observeArticle(1).first()!!
            assertEquals("Updated title", result.title)
            assertTrue(result.isSaved)
            assertEquals(300L, result.savedAtEpochMillis)
            assertEquals("saved/image.jpg", result.localImagePath)
            assertEquals(400L, result.fetchedAtEpochMillis)
        }

    @Test
    fun upsertIsIdempotentAndDoesNotDuplicateRows() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1)))
            dao.upsertRemoteArticles(listOf(article(id = 1)))

            assertEquals(1, dao.observeArticles().first().size)
        }

    @Test
    fun savedListOrdersByMostRecentlySavedFirst() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1), article(id = 2), article(id = 3)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 100, localImagePath = null)
            dao.saveArticle(articleId = 2, savedAtEpochMillis = 300, localImagePath = null)
            dao.saveArticle(articleId = 3, savedAtEpochMillis = 200, localImagePath = null)

            assertEquals(listOf(2, 3, 1), dao.observeSavedArticles().first().map { it.id })
        }

    @Test
    fun retentionDeletesOnlyExpiredUnsavedArticles() =
        runTest {
            dao.upsertRemoteArticles(
                listOf(
                    article(id = 1, fetchedAt = 10),
                    article(id = 2, fetchedAt = 10),
                    article(id = 3, fetchedAt = 100),
                ),
            )
            dao.saveArticle(articleId = 2, savedAtEpochMillis = 20, localImagePath = null)

            assertEquals(1, dao.deleteUnsavedOlderThan(cutoffEpochMillis = 50))
            assertEquals(listOf(2, 3), dao.observeArticles().first().map { it.id })
            assertEquals(null, dao.observeArticle(1).first())
        }

    @Test
    fun retentionNeverDeletesAnArticleWithAPendingUnsave() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1, fetchedAt = 10)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 20, localImagePath = "saved/image.jpg")
            dao.markPendingUnsave(articleId = 1, deadlineEpochMillis = 40)

            assertEquals(0, dao.deleteUnsavedOlderThan(cutoffEpochMillis = 1_000))
            assertTrue(dao.observeArticle(1).first()!!.isSaved)
        }

    @Test
    fun feedOrDetailUnsaveIsImmediateWithNoPendingState() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 20, localImagePath = "saved/image.jpg")

            dao.unsaveImmediate(1)

            val result = dao.observeArticle(1).first()!!
            assertTrue(!result.isSaved)
            assertNull(result.savedAtEpochMillis)
            assertNull(result.localImagePath)
            assertNull(result.pendingUnsaveAtEpochMillis)
            assertEquals(emptyList<Int>(), dao.observeSavedArticles().first().map { it.id })
        }

    @Test
    fun savedListRemovalHidesTheArticleImmediatelyWithoutDeletingItsData() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 20, localImagePath = "saved/image.jpg")

            assertEquals(1, dao.markPendingUnsave(articleId = 1, deadlineEpochMillis = 100))

            assertEquals(emptyList<Int>(), dao.observeSavedArticles().first().map { it.id })
            val result = dao.observeArticle(1).first()!!
            assertTrue(result.isSaved)
            assertEquals("saved/image.jpg", result.localImagePath)
        }

    @Test
    fun undoRestoresAPendingRemovalBeforeTheDeadline() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 20, localImagePath = "saved/image.jpg")
            dao.markPendingUnsave(articleId = 1, deadlineEpochMillis = 100)

            assertEquals(1, dao.undoPendingUnsave(1, nowEpochMillis = 50))

            assertEquals(listOf(1), dao.observeSavedArticles().first().map { it.id })
        }

    @Test
    fun undoAfterFinalizationHasNoEffect() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 20, localImagePath = "saved/image.jpg")
            dao.markPendingUnsave(articleId = 1, deadlineEpochMillis = 100)
            dao.finalizeExpiredPendingUnsaves(nowEpochMillis = 100)

            assertEquals(0, dao.undoPendingUnsave(1, nowEpochMillis = 100))
        }

    @Test
    fun undoAfterItsOwnDeadlineHasPassedHasNoEffectEvenBeforeFinalizeRuns() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 20, localImagePath = "saved/image.jpg")
            dao.markPendingUnsave(articleId = 1, deadlineEpochMillis = 100)

            // Deadline has passed, but finalizeExpiredPendingUnsaves hasn't run yet —
            // undo must still refuse, not race finalize to decide the outcome.
            assertEquals(0, dao.undoPendingUnsave(1, nowEpochMillis = 150))
            assertTrue(dao.observeArticle(1).first()!!.pendingUnsaveAtEpochMillis != null)
        }

    @Test
    fun finalizeOnlyRemovesExpiredPendingUnsavesAndReturnsThemForImageCleanup() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1), article(id = 2)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 10, localImagePath = "saved/1.jpg")
            dao.saveArticle(articleId = 2, savedAtEpochMillis = 10, localImagePath = "saved/2.jpg")
            dao.markPendingUnsave(articleId = 1, deadlineEpochMillis = 100)
            dao.markPendingUnsave(articleId = 2, deadlineEpochMillis = 200)

            val finalized = dao.finalizeExpiredPendingUnsaves(nowEpochMillis = 100)

            assertEquals(listOf(1), finalized.map { it.id })
            assertEquals("saved/1.jpg", finalized.single().localImagePath)
            assertTrue(!dao.observeArticle(1).first()!!.isSaved)
            assertTrue(dao.observeArticle(2).first()!!.isSaved)
        }

    @Test
    fun saveArticleReturnsZeroWhenTheArticleIsNotInTheLocalCache() =
        runTest {
            assertEquals(0, dao.saveArticle(articleId = 999, savedAtEpochMillis = 20, localImagePath = null))
        }

    @Test
    fun saveArticleReturnsOneWhenItUpdatesAnExistingRow() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1)))

            assertEquals(1, dao.saveArticle(articleId = 1, savedAtEpochMillis = 20, localImagePath = null))
        }

    @Test
    fun attachLocalImagePathChangesOnlyTheImageAndPreservesSavedListOrder() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1), article(id = 2)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 100, localImagePath = null)
            dao.saveArticle(articleId = 2, savedAtEpochMillis = 200, localImagePath = null)

            assertEquals(1, dao.attachLocalImagePath(1, "saved/1.img"))

            assertEquals(listOf(2, 1), dao.observeSavedArticles().first().map { it.id })
            val attached = dao.observeArticle(1).first()!!
            assertEquals("saved/1.img", attached.localImagePath)
            assertEquals(100L, attached.savedAtEpochMillis)
            assertTrue(attached.isSaved)
        }

    @Test
    fun attachLocalImagePathIsRejectedAfterTheArticleIsNoLongerSaved() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 100, localImagePath = null)
            dao.unsaveImmediate(1)

            assertEquals(0, dao.attachLocalImagePath(1, "saved/1.img"))
            assertNull(dao.observeArticle(1).first()!!.localImagePath)
        }

    @Test
    fun attachLocalImagePathStillWorksWhileRemovalIsPending() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 100, localImagePath = null)
            dao.markPendingUnsave(articleId = 1, deadlineEpochMillis = 200)

            assertEquals(1, dao.attachLocalImagePath(1, "saved/1.img"))
            assertEquals("saved/1.img", dao.observeArticle(1).first()!!.localImagePath)
        }

    @Test
    fun finalizeAllPendingUnsavesClearsEveryPendingRowButLeavesRowsInTheCache() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1), article(id = 2), article(id = 3)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 100, localImagePath = "saved/1.img")
            dao.saveArticle(articleId = 2, savedAtEpochMillis = 200, localImagePath = "saved/2.img")
            dao.saveArticle(articleId = 3, savedAtEpochMillis = 300, localImagePath = null)
            dao.markPendingUnsave(articleId = 1, deadlineEpochMillis = Long.MAX_VALUE)
            dao.markPendingUnsave(articleId = 2, deadlineEpochMillis = 1)

            val finalized = dao.finalizeAllPendingUnsaves()

            assertEquals(listOf(1, 2), finalized.map { it.id }.sorted())
            assertEquals(listOf(1, 2, 3), dao.observeArticles().first().map { it.id })
            assertTrue(!dao.observeArticle(1).first()!!.isSaved)
            assertTrue(!dao.observeArticle(2).first()!!.isSaved)
            assertTrue(dao.observeArticle(3).first()!!.isSaved)
            assertEquals(
                emptyList<Int>(),
                dao
                    .observeSavedArticles()
                    .first()
                    .map { it.id }
                    .filter { it != 3 },
            )
        }

    @Test
    fun finalizedRowsBecomeEligibleForUnsavedRetentionPrune() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1, fetchedAt = 10)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 20, localImagePath = "saved/1.img")
            dao.markPendingUnsave(articleId = 1, deadlineEpochMillis = Long.MAX_VALUE)

            dao.finalizeAllPendingUnsaves()

            assertEquals(1, dao.deleteUnsavedOlderThan(cutoffEpochMillis = 100))
            assertNull(dao.observeArticle(1).first())
        }

    @Test
    fun savedStateIsVisibleToFeedAndSavedQueriesFromTheSameRoomRow() =
        runTest {
            dao.upsertRemoteArticles(listOf(article(id = 1)))
            dao.saveArticle(articleId = 1, savedAtEpochMillis = 100, localImagePath = null)

            assertTrue(
                dao
                    .observeArticles()
                    .first()
                    .single()
                    .isSaved,
            )
            assertEquals(listOf(1), dao.observeSavedArticles().first().map { it.id })
        }

    private fun article(
        id: Int,
        publishedAt: Long? = 100,
        fetchedAt: Long = publishedAt ?: 0,
        title: String = "Article $id",
    ) = ArticleEntity(
        id = id,
        title = title,
        source = "Source",
        authorsJson = "[]",
        summary = null,
        imageUrl = null,
        articleUrl = "https://example.com/$id",
        publishedAtEpochMillis = publishedAt,
        fetchedAtEpochMillis = fetchedAt,
    )
}
