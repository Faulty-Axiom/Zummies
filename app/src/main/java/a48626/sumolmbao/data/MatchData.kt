package a48626.sumolmbao.data

data class RikishiMatchesResponse(
    val kimariteLosses: Map<String, Int>,
    val kimariteWins: Map<String, Int>,
    val matches: List<RikishiMatch>,
    val opponentWins: Int,
    val rikishiWins: Int,
    val total: Int
)

data class RikishiMatch(
    val bashoId: String,
    val division: String,
    val day: Int,
    val matchNo: Int,
    val eastId: Int,
    val eastShikona: String,
    val eastRank: String,
    val westId: Int,
    val westShikona: String,
    val westRank: String,
    val kimarite: String,
    val winnerId: Int,
    val winnerEn: String,
    val winnerJp: String
)