package com.eric.contentfeed.feed.data.remote

import com.eric.contentfeed.feed.data.remote.openmeteo.OpenMeteoApi
import com.eric.contentfeed.feed.data.remote.spaceflightnews.SpaceflightNewsApi
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.junit.Test

/** No I/O — `validateEagerly(true)` makes `create()` resolve the sandwich call adapter
 * and Moshi converter for every method signature immediately, so a missing `@Json`
 * mapping, an unsupported return type, or a converter mismatch fails here instead of
 * on-device. Stands in for the MockWebServer dependency this project doesn't have. */
class RetrofitApiContractTest {
    private val moshi = Moshi.Builder().build()
    private val client = OkHttpClient.Builder().build()

    @Test
    fun spaceflightNewsApiResolvesItsCallAdapterAndConverterForEveryMethod() {
        val retrofit =
            createRetrofit(client, moshi, SPACEFLIGHT_NEWS_BASE_URL)
                .newBuilder()
                .validateEagerly(true)
                .build()

        retrofit.create(SpaceflightNewsApi::class.java)
    }

    @Test
    fun openMeteoApiResolvesItsCallAdapterAndConverterForEveryMethod() {
        val retrofit =
            createRetrofit(client, moshi, OPEN_METEO_BASE_URL)
                .newBuilder()
                .validateEagerly(true)
                .build()

        retrofit.create(OpenMeteoApi::class.java)
    }
}
