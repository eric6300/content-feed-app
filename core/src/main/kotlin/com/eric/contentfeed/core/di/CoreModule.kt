package com.eric.contentfeed.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.eric.contentfeed.core.BuildConfig
import com.eric.contentfeed.core.connectivity.AndroidConnectivityObserver
import com.eric.contentfeed.core.connectivity.ConnectivityObserver
import com.eric.contentfeed.core.database.ContentFeedDatabase
import com.eric.contentfeed.core.freshness.DataStoreFreshnessGate
import com.eric.contentfeed.core.freshness.EpochClock
import com.eric.contentfeed.core.freshness.FreshnessGate
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

// Shared by FreshnessGate's per-source timestamps and the feed's pagination cursor —
// named for the feature, not the first key type it held. A corrupted preferences file
// (e.g. truncated by a kill during write) falls back to empty rather than throwing —
// every freshness key reads as never-fetched (safe: only forces a refetch) and the
// cursor reads as unset (safe: only re-requests the first page).
private val Context.feedPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "feed_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

val coreModule =
    module {
        single<ContentFeedDatabase> {
            Room
                .databaseBuilder(
                    androidContext(),
                    ContentFeedDatabase::class.java,
                    "content_feed.db",
                ).build()
        }
        single { get<ContentFeedDatabase>().articleDao() }
        single { get<ContentFeedDatabase>().weatherDao() }
        single { get<ContentFeedDatabase>().feedPlacementDao() }
        single<DataStore<Preferences>> { androidContext().feedPreferencesDataStore }
        single<EpochClock> { EpochClock { System.currentTimeMillis() } }
        single<FreshnessGate> { DataStoreFreshnessGate(get(), get()) }
        single<ConnectivityObserver> { AndroidConnectivityObserver(androidContext()) }
        single {
            OkHttpClient
                .Builder()
                .apply {
                    if (BuildConfig.DEBUG) {
                        addInterceptor(
                            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
                        )
                    }
                }.build()
        }
        single { Moshi.Builder().build() }
    }
