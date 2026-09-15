package com.eric.contentfeed.feed.data.remote

import com.eric.contentfeed.feed.domain.model.RemoteFailure
import com.skydoves.sandwich.ApiResponse

sealed interface RemoteResult<out T> {
    data class Loaded<out T>(
        val value: T,
    ) : RemoteResult<T>

    data class Failure(
        val cause: RemoteFailure,
    ) : RemoteResult<Nothing>
}

/** sandwich substitutes `kotlin.Unit` for the body of a 2xx that carries no content
 * (Retrofit hands it a null body for 204/205), so the declared DTO type is not
 * guaranteed at runtime — an unchecked `data` access throws instead of failing cleanly. */
internal inline fun <reified T : Any> ApiResponse.Success<*>.bodyAs(): T? = data as? T
