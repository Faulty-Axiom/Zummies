package a48626.sumolmbao.database
import a48626.sumolmbao.data.RikishiDetails

// Extension function to map a RikishiDetails object to a RikishiEntity for Room storage
fun RikishiDetails.toEntity(timestamp: Long): RikishiEntity {
    return RikishiEntity(
        originalId = this.id.toString(),
        sumodbId = this.sumodbId,
        nskId = this.nskId,
        shikonaEn = this.shikonaEn,
        shikonaJp = this.shikonaJp,
        currentRank = this.currentRank ?: "Retired",
        rank = this.currentRank,
        rankValue = parseRankValue(this.currentRank),
        heya = this.heya,
        birthDate = this.birthDate,
        shusshin = this.shusshin,
        height = this.height ?: 0.0,
        weight = this.weight ?: 0.0,
        debut = this.debut,
        wins = 0,
        losses = 0,
        absences = 0,
        timestamp = timestamp
    )
}

// Extension function to map a RikishiEntity back to a RikishiDetails object
fun RikishiEntity.toRikishiDetails(): RikishiDetails {
    return RikishiDetails(
        id = this.originalId.toIntOrNull() ?: 0,
        sumodbId = this.sumodbId ?: 0,
        nskId = this.nskId ?: 0,
        shikonaEn = this.shikonaEn ?: "",
        shikonaJp = this.shikonaJp ?: "",
        currentRank = this.currentRank ?: this.rank ?: "",
        heya = this.heya ?: "",
        birthDate = this.birthDate ?: "",
        shusshin = this.shusshin ?: "",
        height = this.height ?: 0.0,
        weight = this.weight ?: 0.0,
        debut = this.debut ?: "",
        updatedAt = "" // Not stored in entity
    )
}

private fun parseRankValue(rank: String?): Int {
    if (rank.isNullOrEmpty()) return 1000 // Default value for unknown ranks

    return when {
        rank.contains("Yokozuna", ignoreCase = true) -> 100
        rank.contains("Ozeki", ignoreCase = true) -> 200
        rank.contains("Sekiwake", ignoreCase = true) -> 300
        rank.contains("Komusubi", ignoreCase = true) -> 400
        rank.contains("Maegashira", ignoreCase = true) -> {
            try {
                val number = rank.split(" ")[1].toIntOrNull() ?: 0
                500 + (16 - number.coerceIn(1..16))
            } catch (e: Exception) {
                500
            }
        }
        rank.contains("Juryo", ignoreCase = true) -> {
            try {
                val number = rank.split(" ")[1].toIntOrNull() ?: 0
                600 + (14 - number.coerceIn(1..14))
            } catch (e: Exception) {
                600
            }
        }
        else -> 1000
    }
}