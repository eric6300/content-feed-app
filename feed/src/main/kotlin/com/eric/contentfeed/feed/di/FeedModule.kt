package com.eric.contentfeed.feed.di

import com.eric.contentfeed.feed.data.local.ArticleAuthorsJsonCodec
import com.eric.contentfeed.feed.data.local.BundledServiceCardCatalog
import com.eric.contentfeed.feed.data.local.FeedLocalDataSource
import com.eric.contentfeed.feed.data.local.RoomFeedLocalDataSource
import com.eric.contentfeed.feed.data.local.ServiceCardCatalog
import com.eric.contentfeed.feed.data.local.WeatherCacheJsonCodec
import com.squareup.moshi.Moshi
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val feedModule =
    module {
        single { Moshi.Builder().build() }
        single { ArticleAuthorsJsonCodec(get()) }
        single { WeatherCacheJsonCodec(get()) }
        single<FeedLocalDataSource> {
            RoomFeedLocalDataSource(get(), get(), get(), get(), get())
        }
        single<ServiceCardCatalog> { BundledServiceCardCatalog(androidContext(), get()) }
    }
