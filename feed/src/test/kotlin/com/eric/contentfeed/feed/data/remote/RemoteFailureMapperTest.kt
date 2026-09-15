package com.eric.contentfeed.feed.data.remote

import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.skydoves.sandwich.ApiResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RemoteFailureMapperTest {
    @Test
    fun httpErrorResponseMapsToHttpWithTheRealStatusCode() {
        val failure = ApiResponse.Failure.Error(errorResponse(404))

        assertEquals(RemoteFailure.Http(404), failure.toRemoteFailure())
    }

    @Test
    fun anotherHttpStatusCodeIsPreservedExactly() {
        val failure = ApiResponse.Failure.Error(errorResponse(503))

        assertEquals(RemoteFailure.Http(503), failure.toRemoteFailure())
    }

    @Test
    fun aNonResponsePayloadMapsToUnknown() {
        val failure = ApiResponse.Failure.Error("not a response")

        assertEquals(RemoteFailure.Unknown, failure.toRemoteFailure())
    }

    @Test
    fun unknownHostExceptionMapsToNetworkUnavailable() {
        val failure = ApiResponse.Failure.Exception(UnknownHostException())

        assertEquals(RemoteFailure.NetworkUnavailable, failure.toRemoteFailure())
    }

    @Test
    fun socketTimeoutExceptionMapsToNetworkUnavailable() {
        val failure = ApiResponse.Failure.Exception(SocketTimeoutException())

        assertEquals(RemoteFailure.NetworkUnavailable, failure.toRemoteFailure())
    }

    @Test
    fun nonIoExceptionMapsToUnknown() {
        val failure = ApiResponse.Failure.Exception(IllegalStateException())

        assertEquals(RemoteFailure.Unknown, failure.toRemoteFailure())
    }

    private fun errorResponse(code: Int): Response<*> =
        Response.error<Any>(
            code,
            "".toResponseBody("application/json".toMediaType()),
        )
}
