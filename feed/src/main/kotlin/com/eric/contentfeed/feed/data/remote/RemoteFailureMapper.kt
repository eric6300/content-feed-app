package com.eric.contentfeed.feed.data.remote

import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.skydoves.sandwich.ApiResponse
import retrofit2.Response
import java.io.IOException

/** Converts a sandwich failure into the domain-safe [RemoteFailure] vocabulary. Uses
 * the raw [Response] payload rather than sandwich's `statusCode` extension, which
 * collapses any code outside its own enum to `Unknown` and would lose the real number. */
internal fun ApiResponse.Failure<*>.toRemoteFailure(): RemoteFailure =
    when (this) {
        is ApiResponse.Failure.Error ->
            (payload as? Response<*>)
                ?.let { RemoteFailure.Http(it.code()) }
                ?: RemoteFailure.Unknown
        is ApiResponse.Failure.Exception ->
            if (throwable is IOException) {
                RemoteFailure.NetworkUnavailable
            } else {
                RemoteFailure.Unknown
            }
    }
