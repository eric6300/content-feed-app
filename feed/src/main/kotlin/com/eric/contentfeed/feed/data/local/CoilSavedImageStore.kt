package com.eric.contentfeed.feed.data.local

import android.content.Context
import coil3.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/** Copies only entries already present in Coil's shared disk cache. The save path never
 * starts a network request and a cache miss is deliberately a successful no-image save. */
internal class CoilSavedImageStore(
    context: Context,
    private val imageLoader: ImageLoader,
) : SavedImageStore {
    private val savedImagesDirectory =
        context.filesDir.absolutePath
            .toPath()
            .resolve(SAVED_IMAGES_DIRECTORY)
    private val fileSystem = FileSystem.SYSTEM

    override suspend fun copyFromCache(
        articleId: Int,
        imageUrl: String?,
    ): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (imageUrl.isNullOrBlank()) return@runCatching null
                val snapshot = imageLoader.diskCache?.openSnapshot(imageUrl) ?: return@runCatching null
                snapshot.use {
                    fileSystem.createDirectories(savedImagesDirectory)
                    val temporaryPath = savedImagesDirectory.resolve("$articleId.img.tmp")
                    val finalPath = pathFor(articleId)
                    fileSystem.delete(temporaryPath, mustExist = false)
                    fileSystem.copy(it.data, temporaryPath)
                    fileSystem.delete(finalPath, mustExist = false)
                    fileSystem.atomicMove(temporaryPath, finalPath)
                    finalPath.toString()
                }
            }.getOrNull()
        }

    override suspend fun delete(articleId: Int) {
        withContext(Dispatchers.IO) {
            runCatching { fileSystem.delete(pathFor(articleId)) }
            runCatching { fileSystem.delete(savedImagesDirectory.resolve("$articleId.img.tmp")) }
        }
    }

    private fun pathFor(articleId: Int): Path = savedImagesDirectory.resolve("$articleId.img")

    private companion object {
        const val SAVED_IMAGES_DIRECTORY = "saved_images"
    }
}
