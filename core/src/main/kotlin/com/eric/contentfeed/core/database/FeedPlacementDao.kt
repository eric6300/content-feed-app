package com.eric.contentfeed.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedPlacementDao {
    @Query(
        """
        SELECT * FROM feed_placements
        ORDER BY assignmentSequence ASC
        """,
    )
    fun observePlacements(): Flow<List<FeedPlacementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(placement: FeedPlacementEntity)

    @Query(
        """
        SELECT * FROM feed_placements
        WHERE anchorArticleId = :articleId
        LIMIT 1
        """,
    )
    suspend fun findByAnchorArticleId(articleId: Int): FeedPlacementEntity?

    @Query("SELECT COUNT(*) FROM feed_placements")
    suspend fun countPlacements(): Int

    @Query("SELECT COUNT(*) FROM feed_placements WHERE contentType = :contentType")
    suspend fun countPlacementsByContentType(contentType: String): Int
}
