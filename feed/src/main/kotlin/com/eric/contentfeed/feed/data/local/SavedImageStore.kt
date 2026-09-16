package com.eric.contentfeed.feed.data.local

/** Owns app-internal copies of article images that must remain readable offline. */
interface SavedImageStore {
    /** Returns the copied file path, or null when the image is not in Coil's disk cache. */
    suspend fun copyFromCache(
        articleId: Int,
        imageUrl: String?,
    ): String?

    /** Deletes the deterministic saved-image file for [articleId], if present. */
    suspend fun delete(articleId: Int)
}
