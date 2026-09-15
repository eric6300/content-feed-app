package com.eric.contentfeed.feed.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * The article pagination cursor: the offset the next page request should use.
 * Persisted independently of Room, never derived from the article row count — that
 * count drops on retention pruning and would otherwise re-request already-seen pages.
 */
interface FeedCursorStore {
    suspend fun readNextOffset(): Int

    suspend fun writeNextOffset(offset: Int)
}

class DataStoreFeedCursorStore(
    private val dataStore: DataStore<Preferences>,
) : FeedCursorStore {
    override suspend fun readNextOffset(): Int = dataStore.data.first()[NEXT_OFFSET_KEY] ?: 0

    override suspend fun writeNextOffset(offset: Int) {
        require(offset >= 0) { "Next offset must not be negative" }
        dataStore.edit { preferences -> preferences[NEXT_OFFSET_KEY] = offset }
    }

    private companion object {
        val NEXT_OFFSET_KEY = intPreferencesKey("feed.articles.nextOffset")
    }
}
