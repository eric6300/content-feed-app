package com.eric.contentfeed.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val SERVICE_CARD = "service_card"
private const val VIDEO_CARD = "video_card"

@RunWith(AndroidJUnit4::class)
class FeedPlacementDaoTest {
    private lateinit var database: ContentFeedDatabase
    private lateinit var articleDao: ArticleDao
    private lateinit var placementDao: FeedPlacementDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ContentFeedDatabase::class.java).build()
        articleDao = database.articleDao()
        placementDao = database.feedPlacementDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertIfAbsentKeepsTheFirstPlacementForAnAnchor() =
        runTest {
            articleDao.upsertRemoteArticles(listOf(article(id = 10)))

            placementDao.insertIfAbsent(placement(anchorArticleId = 10, poolIndex = 1, assignmentSequence = 0))
            placementDao.insertIfAbsent(placement(anchorArticleId = 10, poolIndex = 2, assignmentSequence = 1))

            assertEquals(1, placementDao.findByAnchorArticleId(10, SERVICE_CARD)!!.poolIndex)
            assertEquals(1, placementDao.observePlacements().first().size)
        }

    @Test
    fun differentContentTypesCanShareTheSameAnchorArticle() =
        runTest {
            articleDao.upsertRemoteArticles(listOf(article(id = 10)))

            placementDao.insertIfAbsent(
                placement(anchorArticleId = 10, contentType = SERVICE_CARD, poolIndex = 0, assignmentSequence = 0),
            )
            placementDao.insertIfAbsent(
                placement(anchorArticleId = 10, contentType = VIDEO_CARD, poolIndex = 0, assignmentSequence = 1),
            )

            assertEquals(2, placementDao.observePlacements().first().size)
            assertNotNull(placementDao.findByAnchorArticleId(10, SERVICE_CARD))
            assertNotNull(placementDao.findByAnchorArticleId(10, VIDEO_CARD))
        }

    @Test
    fun observePlacementsOrdersByGlobalAssignmentSequenceAcrossContentTypes() =
        runTest {
            placementDao.insertIfAbsent(
                placement(anchorArticleId = 30, contentType = VIDEO_CARD, poolIndex = 0, assignmentSequence = 2),
            )
            placementDao.insertIfAbsent(placement(anchorArticleId = 10, poolIndex = 0, assignmentSequence = 0))
            placementDao.insertIfAbsent(placement(anchorArticleId = 20, poolIndex = 1, assignmentSequence = 1))

            assertEquals(listOf(10, 20, 30), placementDao.observePlacements().first().map { it.anchorArticleId })
        }

    @Test
    fun nextPoolIndexIsCountedIndependentlyPerContentTypeAndSurvivesDeletion() =
        runTest {
            assertEquals(0, placementDao.nextPoolIndex(SERVICE_CARD))

            // Distinct anchor sort keys so deleteOrphanedBelow can remove exactly the
            // id=10 row below, leaving the higher pool index (1) as the sole survivor.
            placementDao.insertIfAbsent(
                placement(
                    anchorArticleId = 10,
                    anchorPublishedAtEpochMillis = 100,
                    contentType = SERVICE_CARD,
                    poolIndex = 0,
                    assignmentSequence = 0,
                ),
            )
            placementDao.insertIfAbsent(
                placement(
                    anchorArticleId = 20,
                    anchorPublishedAtEpochMillis = 300,
                    contentType = SERVICE_CARD,
                    poolIndex = 1,
                    assignmentSequence = 1,
                ),
            )
            placementDao.insertIfAbsent(
                placement(
                    anchorArticleId = 30,
                    anchorPublishedAtEpochMillis = 200,
                    contentType = VIDEO_CARD,
                    poolIndex = 0,
                    assignmentSequence = 2,
                ),
            )

            assertEquals(2, placementDao.nextPoolIndex(SERVICE_CARD))
            assertEquals(1, placementDao.nextPoolIndex(VIDEO_CARD))
            assertEquals(3, placementDao.countPlacements())

            // Removes only the id=10 row (pub=100, below the 200 cutoff); the
            // surviving SERVICE_CARD row keeps poolIndex=1. A COUNT-based cycle would
            // now regress to 1 (one row survives); MAX+1 correctly stays at 2.
            placementDao.deleteOrphanedBelow(oldestSurvivingPublishedAtEpochMillis = 200, oldestSurvivingArticleId = 30)
            assertEquals(2, placementDao.nextPoolIndex(SERVICE_CARD))
        }

    @Test
    fun nextAssignmentSequenceIsGlobalAcrossContentTypesAndSurvivesDeletion() =
        runTest {
            assertEquals(0L, placementDao.nextAssignmentSequence())

            placementDao.insertIfAbsent(
                placement(anchorArticleId = 10, contentType = SERVICE_CARD, poolIndex = 0, assignmentSequence = 0),
            )
            placementDao.insertIfAbsent(
                placement(anchorArticleId = 30, contentType = VIDEO_CARD, poolIndex = 0, assignmentSequence = 2),
            )

            assertEquals(3L, placementDao.nextAssignmentSequence())
        }

    @Test
    fun deleteOrphanedBelowRemovesPlacementsBelowTheOldestSurvivingArticle() =
        runTest {
            placementDao.insertIfAbsent(
                placement(
                    anchorArticleId = 10,
                    anchorPublishedAtEpochMillis = 300,
                    poolIndex = 0,
                    assignmentSequence = 0,
                ),
            )
            placementDao.insertIfAbsent(
                placement(
                    anchorArticleId = 20,
                    anchorPublishedAtEpochMillis = 200,
                    poolIndex = 1,
                    assignmentSequence = 1,
                ),
            )
            placementDao.insertIfAbsent(
                placement(
                    anchorArticleId = 30,
                    anchorPublishedAtEpochMillis = 100,
                    poolIndex = 2,
                    assignmentSequence = 2,
                ),
            )

            // Oldest surviving article now sorts at 200/20 — the 100/30 placement,
            // orphaned by a whole-session prune, must be removed; the others must not.
            val removed =
                placementDao.deleteOrphanedBelow(
                    oldestSurvivingPublishedAtEpochMillis = 200,
                    oldestSurvivingArticleId = 20,
                )

            assertEquals(1, removed)
            assertNotNull(placementDao.findByAnchorArticleId(10, SERVICE_CARD))
            assertNotNull(placementDao.findByAnchorArticleId(20, SERVICE_CARD))
            assertEquals(null, placementDao.findByAnchorArticleId(30, SERVICE_CARD))
        }

    @Test
    fun deleteOrphanedBelowTreatsANullAnchorTimestampAsOlderThanAnyNonNullValue() =
        runTest {
            placementDao.insertIfAbsent(
                placement(
                    anchorArticleId = 10,
                    anchorPublishedAtEpochMillis = 100,
                    poolIndex = 0,
                    assignmentSequence = 0,
                ),
            )
            placementDao.insertIfAbsent(
                placement(
                    anchorArticleId = 20,
                    anchorPublishedAtEpochMillis = null,
                    poolIndex = 1,
                    assignmentSequence = 1,
                ),
            )

            val removed =
                placementDao.deleteOrphanedBelow(
                    oldestSurvivingPublishedAtEpochMillis = 100,
                    oldestSurvivingArticleId = 10,
                )

            assertEquals(1, removed)
            assertNotNull(placementDao.findByAnchorArticleId(10, SERVICE_CARD))
            assertEquals(null, placementDao.findByAnchorArticleId(20, SERVICE_CARD))
        }

    @Test
    fun placementSurvivesItsAnchorArticleBeingPrunedFromTheCache() =
        runTest {
            articleDao.upsertRemoteArticles(listOf(article(id = 10, fetchedAt = 1)))
            placementDao.insertIfAbsent(
                placement(
                    anchorArticleId = 10,
                    anchorPublishedAtEpochMillis = 100,
                    poolIndex = 1,
                    assignmentSequence = 0,
                ),
            )

            articleDao.deleteUnsavedOlderThan(cutoffEpochMillis = 1_000)

            assertEquals(null, articleDao.observeArticle(10).first())
            val survivingPlacement = placementDao.findByAnchorArticleId(10, SERVICE_CARD)
            assertNotNull(survivingPlacement)
            assertEquals(100L, survivingPlacement!!.anchorPublishedAtEpochMillis)
            assertEquals(1, survivingPlacement.poolIndex)
        }

    private fun article(
        id: Int,
        fetchedAt: Long = 10,
    ) = ArticleEntity(
        id = id,
        title = "Article $id",
        source = "Source",
        authorsJson = "[]",
        summary = null,
        imageUrl = null,
        articleUrl = "https://example.com/$id",
        publishedAtEpochMillis = 100,
        fetchedAtEpochMillis = fetchedAt,
    )

    private fun placement(
        anchorArticleId: Int,
        anchorPublishedAtEpochMillis: Long? = 100,
        contentType: String = SERVICE_CARD,
        poolIndex: Int,
        assignmentSequence: Long,
    ) = FeedPlacementEntity(
        anchorArticleId = anchorArticleId,
        anchorPublishedAtEpochMillis = anchorPublishedAtEpochMillis,
        contentType = contentType,
        poolIndex = poolIndex,
        assignmentSequence = assignmentSequence,
    )
}
