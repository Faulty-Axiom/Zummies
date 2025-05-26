package a48626.sumolmbao.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface RikishiDao {
    @Query("SELECT * FROM rikishi_table")
    suspend fun getAllRikishi(): List<RikishiEntity>

    @Query("SELECT * FROM rikishi_table WHERE original_id = :id")
    suspend fun getRikishiById(id: String): RikishiEntity?

    @Query("SELECT * FROM rikishi_table WHERE shikona_en LIKE '%' || :query || '%'")
    suspend fun searchByShikona(query: String): List<RikishiEntity>

    @Query("SELECT * FROM rikishi_table WHERE heya = :heya")
    suspend fun getRikishiByHeya(heya: String): List<RikishiEntity>

    @Query("SELECT * FROM rikishi_table WHERE current_rank LIKE :rankFilter")
    suspend fun getRikishiByRank(rankFilter: String): List<RikishiEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rikishi: RikishiEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rikishi: List<RikishiEntity>)

    @Update
    suspend fun update(rikishi: RikishiEntity)

    @Query("DELETE FROM rikishi_table")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM rikishi_table")
    suspend fun getCount(): Int

    @Query("SELECT original_id FROM rikishi_table WHERE original_id IN (:ids)")
    suspend fun findExistingIds(ids: List<String>): List<String>

    @Query("SELECT * FROM rikishi_table ORDER BY rank_value ASC")
    suspend fun getAllRikishiOrderedByRank(): List<RikishiEntity>

    @Query("SELECT DISTINCT heya FROM rikishi_table WHERE heya IS NOT NULL ORDER BY heya ASC")
    suspend fun getAllHeyas(): List<String>

    @Transaction
    suspend fun upsertAll(rikishi: List<RikishiEntity>) {
        rikishi.forEach { rikishi ->
            if (findExistingIds(listOf(rikishi.originalId)).isNotEmpty()) {
                update(rikishi)
            } else {
                insert(rikishi)
            }
        }
    }

    @Transaction
    suspend fun insertAllInBatches(rikishi: List<RikishiEntity>) {
        rikishi.chunked(500).forEach { batch ->
            insertAll(batch)
        }
    }
}