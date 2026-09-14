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
    fun poolIndexCyclingIsCountedIndependentlyPerContentType() =
        runTest {
            placementDao.insertIfAbsent(
                placement(anchorArticleId = 10, contentType = SERVICE_CARD, poolIndex = 0, assignmentSequence = 0),
            )
            placementDao.insertIfAbsent(
                placement(anchorArticleId = 20, contentType = SERVICE_CARD, poolIndex = 1, assignmentSequence = 1),
            )
            placementDao.insertIfAbsent(
                placement(anchorArticleId = 30, contentType = VIDEO_CARD, poolIndex = 0, assignmentSequence = 2),
            )

            assertEquals(2, placementDao.countPlacementsByContentType(SERVICE_CARD))
            assertEquals(1, placementDao.countPlacementsByContentType(VIDEO_CARD))
            assertEquals(3, placementDao.countPlacements())
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
