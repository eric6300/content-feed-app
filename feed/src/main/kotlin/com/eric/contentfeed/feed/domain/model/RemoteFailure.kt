package com.eric.contentfeed.feed.domain.model

/** Domain-safe failure vocabulary. Retrofit/OkHttp/sandwich types never cross the
 * RemoteDataSource boundary; they collapse into one of these three. */
sealed interface RemoteFailure {
    data object NetworkUnavailable : RemoteFailure

    data class Http(
        val code: Int,
    ) : RemoteFailure

    data object Unknown : RemoteFailure
}
