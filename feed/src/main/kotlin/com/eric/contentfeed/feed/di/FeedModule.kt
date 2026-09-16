package com.eric.contentfeed.feed.di

import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.eric.contentfeed.feed.data.local.ArticleAuthorsJsonCodec
import com.eric.contentfeed.feed.data.local.BundledServiceCardCatalog
import com.eric.contentfeed.feed.data.local.CoilSavedImageStore
import com.eric.contentfeed.feed.data.local.DataStoreFeedCursorStore
import com.eric.contentfeed.feed.data.local.FeedCursorStore
import com.eric.contentfeed.feed.data.local.FeedLocalDataSource
import com.eric.contentfeed.feed.data.local.RoomFeedLocalDataSource
import com.eric.contentfeed.feed.data.local.SavedImageStore
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
import com.eric.contentfeed.feed.domain.usecase.FinalizePendingUnsavesUseCase
import com.eric.contentfeed.feed.domain.usecase.LoadNextArticlePageUseCase
import com.eric.contentfeed.feed.domain.usecase.ObserveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.ObserveFeedUseCase
import com.eric.contentfeed.feed.domain.usecase.ObserveSavedArticlesUseCase
import com.eric.contentfeed.feed.domain.usecase.RefreshFeedUseCase
import com.eric.contentfeed.feed.domain.usecase.SaveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.UndoUnsaveArticleUseCase
import com.eric.contentfeed.feed.domain.usecase.UnsaveArticleUseCase
import com.eric.contentfeed.feed.repository.ArticleRepository
import com.eric.contentfeed.feed.repository.DefaultArticleRepository
import com.eric.contentfeed.feed.repository.DefaultSavedArticleRepository
import com.eric.contentfeed.feed.repository.DefaultWeatherRepository
import com.eric.contentfeed.feed.repository.SavedArticleRepository
import com.eric.contentfeed.feed.repository.WeatherRepository
import okhttp3.OkHttpClient
import okio.Path.Companion.toPath
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
        single<FeedCursorStore> { DataStoreFeedCursorStore(get()) }
        single<ImageLoader> {
            val context = androidContext()
            val client = get<OkHttpClient>()
            ImageLoader
                .Builder(context)
                .components { add(OkHttpNetworkFetcherFactory(client)) }
                .diskCache(
                    DiskCache
                        .Builder()
                        .directory(
                            context.cacheDir
                                .resolve("image_cache")
                                .absolutePath
                                .toPath(),
                        ).build(),
                ).build()
        }
        single<SavedImageStore> { CoilSavedImageStore(androidContext(), get()) }

        single<SpaceflightNewsApi> {
            createRetrofit(get(), get(), SPACEFLIGHT_NEWS_BASE_URL).create(SpaceflightNewsApi::class.java)
        }
        single<OpenMeteoApi> {
            createRetrofit(get(), get(), OPEN_METEO_BASE_URL).create(OpenMeteoApi::class.java)
        }
        single<ArticleRemoteDataSource> { SpaceflightNewsRemoteDataSource(get()) }
        single<WeatherRemoteDataSource> { OpenMeteoRemoteDataSource(get()) }

        single<ArticleRepository> { DefaultArticleRepository(get(), get(), get(), get(), get()) }
        single<WeatherRepository> { DefaultWeatherRepository(get(), get(), get()) }
        single<SavedArticleRepository> { DefaultSavedArticleRepository(get(), get(), get()) }

        factory { ObserveFeedUseCase(get(), get(), get()) }
        factory { RefreshFeedUseCase(get(), get()) }
        factory { LoadNextArticlePageUseCase(get()) }
        factory { SaveArticleUseCase(get()) }
        factory { UnsaveArticleUseCase(get()) }
        factory { UndoUnsaveArticleUseCase(get()) }
        factory { FinalizePendingUnsavesUseCase(get()) }
        factory { ObserveSavedArticlesUseCase(get()) }
        factory { ObserveArticleUseCase(get()) }
    }
