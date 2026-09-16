package com.eric.contentfeed.feed.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Deterministic test double for saved-image copy/delete ordering. */
internal class FakeSavedImageStore(
    var copiedPath: String? = null,
) : SavedImageStore {
    val copyCalls = mutableListOf<Pair<Int, String?>>()
    val deleteCalls = mutableListOf<Int>()
    val events = mutableListOf<String>()
    val eventMutex = Mutex()
    var onCopy: (suspend (Int, String?) -> String?)? = null

    override suspend fun copyFromCache(
        articleId: Int,
        imageUrl: String?,
    ): String? {
        eventMutex.withLock {
            copyCalls += articleId to imageUrl
            events += "copy"
        }
        return onCopy?.invoke(articleId, imageUrl) ?: copiedPath
    }

    override suspend fun delete(articleId: Int) {
        eventMutex.withLock {
            deleteCalls += articleId
            events += "delete"
        }
    }
}
