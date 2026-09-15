package com.eric.contentfeed.feed.di

import com.eric.contentfeed.feed.data.local.ArticleAuthorsJsonCodec
import com.eric.contentfeed.feed.data.local.BundledServiceCardCatalog
import com.eric.contentfeed.feed.data.local.FeedLocalDataSource
import com.eric.contentfeed.feed.data.local.RoomFeedLocalDataSource
import com.eric.contentfeed.feed.data.local.ServiceCardCatalog
import com.eric.contentfeed.feed.data.local.WeatherCacheJsonCodec
import com.eric.contentfeed.feed.data.remote.ArticleRemoteDataSource
import com.eric.contentfeed.feed.data.remote.OPEN_METEO_BASE_URL
import com.eric.contentfeed.feed.data.remote.SPACEFLIGHT_NEWS_BASE_URL
import com.eric.contentfeed.feed.data.remote.WeatherRemoteDataSource
import com.eric.contentfeed.feed.data.remote.createRetrofit
import com.eric.contentfeed.feed.data.remote.openmeteo.OpenMeteoApi
import com.eric.contentfeed.feed.data.remote.openmeteo.OpenMeteoRemoteDataSource
import com.eric.contentfeed.feed.data.remote.spaceflightnews.SpaceflightNewsApi
import com.eric.contentfeed.feed.data.remote.spaceflightnews.SpaceflightNewsRemoteDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val feedModule =
    module {
        single { ArticleAuthorsJsonCodec(get()) }
        single { WeatherCacheJsonCodec(get()) }
        single<FeedLocalDataSource> {
            RoomFeedLocalDataSource(get(), get(), get(), get(), get())
        }
        single<ServiceCardCatalog> { BundledServiceCardCatalog(androidContext(), get()) }

        single<SpaceflightNewsApi> {
            createRetrofit(get(), get(), SPACEFLIGHT_NEWS_BASE_URL).create(SpaceflightNewsApi::class.java)
        }
        single<OpenMeteoApi> {
            createRetrofit(get(), get(), OPEN_METEO_BASE_URL).create(OpenMeteoApi::class.java)
        }
        single<ArticleRemoteDataSource> { SpaceflightNewsRemoteDataSource(get()) }
        single<WeatherRemoteDataSource> { OpenMeteoRemoteDataSource(get()) }
    }
