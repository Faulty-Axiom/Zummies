package a48626.sumolmbao.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rikishi_table")
data class RikishiEntity(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,

    @ColumnInfo(name = "original_id", index = true)
    val originalId: String,

    @ColumnInfo(name = "sumodb_id")
    val sumodbId: Int? = null,

    @ColumnInfo(name = "nsk_id")
    val nskId: Int? = null,

    @ColumnInfo(name = "shikona_en")
    val shikonaEn: String?,

    @ColumnInfo(name = "shikona_jp")
    val shikonaJp: String? = null,

    @ColumnInfo(name = "current_rank")
    val currentRank: String? = null,

    @ColumnInfo(name = "rank")
    val rank: String?,

    @ColumnInfo(name = "rank_value")
    val rankValue: Int,

    @ColumnInfo(name = "heya")
    val heya: String? = null,

    @ColumnInfo(name = "birth_date")
    val birthDate: String? = null,

    @ColumnInfo(name = "shusshin")
    val shusshin: String? = null,

    @ColumnInfo(name = "height")
    val height: Double? = null,

    @ColumnInfo(name = "weight")
    val weight: Double? = null,

    @ColumnInfo(name = "debut")
    val debut: String? = null,

    @ColumnInfo(name = "wins")
    val wins: Int,

    @ColumnInfo(name = "losses")
    val losses: Int,

    @ColumnInfo(name = "absences")
    val absences: Int,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
) {
    constructor() : this(
        originalId = "unknown_${System.currentTimeMillis()}",
        shikonaEn = null,
        rank = null,
        rankValue = 0,
        wins = 0,
        losses = 0,
        absences = 0,
        timestamp = 0L
    )
}