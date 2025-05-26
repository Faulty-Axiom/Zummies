package a48626.sumolmbao.data

data class RikishiId(
    val id: Int,
    val sumodbId: Int,
    val nskId: Int,
    val shikonaEn: String,
    val shikonaJp: String,
    val currentRank: String,
    val heya: String,
    val birthDate: String,
    val shusshin: String,
    val height: Double,
    val weight: Double,
    val debut: String,
    val measurements: List<Measurement>? = null,
    val ranks: List<RankHistory>? = null,
    val shikonaHistory: List<ShikonaHistory>? = null,
    val intai: String? = null
)

data class ShikonaHistory(
    val id: String,
    val bashoId: String,
    val rikishiId: Int,
    val shikonaEn: String,
    val shikonaJp: String
)

data class RikishiStats(
    val absenceByDivision: Map<String, Int>,
    val basho: Int,
    val bashoByDivision: Map<String, Int>,
    val lossByDivision: Map<String, Int>,
    val sansho: Map<String, Int>,
    val totalAbsences: Int,
    val totalByDivision: Map<String, Int>,
    val totalLosses: Int,
    val totalMatches: Int,
    val totalWins: Int,
    val winsByDivision: Map<String, Int>,
    val yusho: Int,
    val yushoByDivision: Map<String, Int>
)