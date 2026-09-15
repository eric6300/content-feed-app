package com.eric.contentfeed.feed.data.remote.spaceflightnews

import com.eric.contentfeed.feed.data.remote.ArticlePageResult
import com.eric.contentfeed.feed.data.remote.SPACEFLIGHT_NEWS_ARTICLES_PATH
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.skydoves.sandwich.ApiResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SpaceflightNewsRemoteDataSourceTest {
    private lateinit var api: SpaceflightNewsApi
    private lateinit var dataSource: SpaceflightNewsRemoteDataSource

    @Before
    fun setUp() {
        api = mockk()
        dataSource = SpaceflightNewsRemoteDataSource(api)
    }

    @Test
    fun aNonNullNextMeansItIsNotTheLastPage() =
        runTest {
            coEvery { api.getArticles(any(), any(), any()) } returns
                ApiResponse.Success(response(next = "https://example.com/next"))

            val result = dataSource.fetchPage(offset = 0, limit = 10) as ArticlePageResult.Loaded

            assertFalse(result.isLastPage)
        }

    @Test
    fun aNullNextMeansItIsTheLastPage() =
        runTest {
            coEvery { api.getArticles(any(), any(), any()) } returns
                ApiResponse.Success(response(next = null, results = listOf(article(1))))

            val result = dataSource.fetchPage(offset = 0, limit = 10) as ArticlePageResult.Loaded

            assertTrue(result.isLastPage)
        }

    @Test
    fun emptyResultsWithNullNextIsBothAnEmptyPageAndTheLastPage() =
        runTest {
            coEvery { api.getArticles(any(), any(), any()) } returns
                ApiResponse.Success(response(next = null, results = emptyList()))

            val result = dataSource.fetchPage(offset = 0, limit = 10) as ArticlePageResult.Loaded

            assertEquals(emptyList<Any>(), result.articles)
            assertTrue(result.isLastPage)
        }

    @Test
    fun resultCountBelowLimitWithNonNullNextIsStillNotTheLastPage() =
        runTest {
            coEvery { api.getArticles(any(), any(), any()) } returns
                ApiResponse.Success(response(next = "https://example.com/next", results = listOf(article(1))))

            val result = dataSource.fetchPage(offset = 0, limit = 10) as ArticlePageResult.Loaded

            assertFalse(result.isLastPage)
        }

    @Test
    fun httpFailureMapsToFailureResult() =
        runTest {
            coEvery { api.getArticles(any(), any(), any()) } returns
                ApiResponse.Failure.Exception(IOException())

            val result = dataSource.fetchPage(offset = 0, limit = 10) as ArticlePageResult.Failure

            assertEquals(RemoteFailure.NetworkUnavailable, result.cause)
        }

    @Test
    fun offsetAndLimitReachTheApiVerbatim() =
        runTest {
            coEvery { api.getArticles(any(), any(), any()) } returns ApiResponse.Success(response())

            dataSource.fetchPage(offset = 30, limit = 15)

            coVerify { api.getArticles(url = SPACEFLIGHT_NEWS_ARTICLES_PATH, offset = 30, limit = 15) }
        }

    private fun response(
        next: String? = "https://example.com/next",
        results: List<ArticleDto> = listOf(article(1)),
    ) = ArticleListResponseDto(next = next, results = results)

    private fun article(id: Int) =
        ArticleDto(
            id = id,
            title = "Title $id",
            url = "https://example.com/$id",
            newsSite = "Source",
            imageUrl = null,
            summary = null,
            publishedAt = null,
            authors = null,
        )
}
