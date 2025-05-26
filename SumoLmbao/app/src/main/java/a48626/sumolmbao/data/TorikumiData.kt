package a48626.sumolmbao.data

data class Rikishis(
    val limit: Int,
    val skip: Int,
    val total: Int,
    val records: List<RikishiDetails>?
)

data class RikishiDetails(
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
    val updatedAt: String,
    val measurements: List<Measurement>? = null,
    val ranks: List<RankHistory>? = null,
    val shikonas: List<ShikonaHistory>? = null,
    val intai: Boolean? = null
)

data class Measurement(
    val date: String,
    val height: Int,
    val weight: Int
)

data class RankHistory(
    val date: String,
    val rank: String
)