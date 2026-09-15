package com.eric.contentfeed.feed.data.remote.spaceflightnews

import com.eric.contentfeed.feed.data.remote.ArticlePage
import com.eric.contentfeed.feed.data.remote.RemoteResult
import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.skydoves.sandwich.ApiResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response
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
            coEvery { api.getArticles(any(), any()) } returns
                ApiResponse.Success(response(next = "https://example.com/next"))

            val result = dataSource.fetchPage(offset = 0, limit = 10)

            assertEquals(false, (result as RemoteResult.Loaded<ArticlePage>).value.isLastPage)
        }

    @Test
    fun aNullNextMeansItIsTheLastPage() =
        runTest {
            coEvery { api.getArticles(any(), any()) } returns
                ApiResponse.Success(response(next = null, results = listOf(article(1))))

            val result = dataSource.fetchPage(offset = 0, limit = 10)

            assertEquals(true, (result as RemoteResult.Loaded<ArticlePage>).value.isLastPage)
        }

    @Test
    fun emptyResultsWithNullNextIsBothAnEmptyPageAndTheLastPage() =
        runTest {
            coEvery { api.getArticles(any(), any()) } returns
                ApiResponse.Success(response(next = null, results = emptyList()))

            val result = dataSource.fetchPage(offset = 0, limit = 10)

            assertEquals(
                RemoteResult.Loaded(ArticlePage(articles = emptyList(), isLastPage = true)),
                result,
            )
        }

    @Test
    fun resultCountBelowLimitWithNonNullNextIsStillNotTheLastPage() =
        runTest {
            coEvery { api.getArticles(any(), any()) } returns
                ApiResponse.Success(response(next = "https://example.com/next", results = listOf(article(1))))

            val result = dataSource.fetchPage(offset = 0, limit = 10)

            assertEquals(false, (result as RemoteResult.Loaded<ArticlePage>).value.isLastPage)
        }

    @Test
    fun networkExceptionMapsToFailureResult() =
        runTest {
            coEvery { api.getArticles(any(), any()) } returns
                ApiResponse.Failure.Exception(IOException())

            val result = dataSource.fetchPage(offset = 0, limit = 10)

            assertEquals(RemoteResult.Failure(RemoteFailure.NetworkUnavailable), result)
        }

    @Test
    fun httpErrorMapsToFailureResultWithTheRealStatusCode() =
        runTest {
            coEvery { api.getArticles(any(), any()) } returns
                ApiResponse.Failure.Error(errorResponse(503))

            val result = dataSource.fetchPage(offset = 0, limit = 10)

            assertEquals(RemoteResult.Failure(RemoteFailure.Http(503)), result)
        }

    @Test
    fun aSuccessCarryingNoBodyMapsToUnknownFailure() =
        runTest {
            @Suppress("UNCHECKED_CAST")
            coEvery { api.getArticles(any(), any()) } returns
                ApiResponse.Success<Any>(Unit) as ApiResponse.Success<ArticleListResponseDto>

            val result = dataSource.fetchPage(offset = 0, limit = 10)

            assertEquals(RemoteResult.Failure(RemoteFailure.Unknown), result)
        }

    @Test
    fun offsetAndLimitReachTheApiVerbatim() =
        runTest {
            coEvery { api.getArticles(any(), any()) } returns ApiResponse.Success(response())

            dataSource.fetchPage(offset = 30, limit = 15)

            coVerify { api.getArticles(offset = 30, limit = 15) }
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

    private fun errorResponse(code: Int): Response<*> =
        Response.error<Any>(
            code,
            "".toResponseBody("application/json".toMediaType()),
        )
}
