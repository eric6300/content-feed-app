package com.eric.contentfeed.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.eric.contentfeed.core.database.ContentFeedDatabase
import com.eric.contentfeed.core.freshness.DataStoreFreshnessGate
import com.eric.contentfeed.core.freshness.EpochClock
import com.eric.contentfeed.core.freshness.FreshnessGate
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

// A corrupted preferences file (e.g. truncated by a kill during write) falls back to
// empty rather than throwing — every key reads as never-fetched, i.e. stale, which is
// the safe default: it only forces a refetch, it never breaks the cache-first read.
private val Context.freshnessDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "freshness",
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
        single<DataStore<Preferences>> { androidContext().freshnessDataStore }
        single<EpochClock> { EpochClock { System.currentTimeMillis() } }
        single<FreshnessGate> { DataStoreFreshnessGate(get(), get()) }
    }
