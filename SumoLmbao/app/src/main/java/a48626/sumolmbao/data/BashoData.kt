package a48626.sumolmbao.data

data class Banzuke(
    val east: List<Rikishi>,
    val west: List<Rikishi>
)

data class BanzukeRow(
    val east: Rikishi?,
    val west: Rikishi?,
    val eastRank: String? = null,
    val westRank: String? = null,
    val isSanyakuRow: Boolean = false,
    val technique: String? = null,
    val winner: String? = null
)

data class Basho(
    val date: String,
    val startDate: String,
    val endDate: String,
    val yusho: List<Yusho>,
    val specialPrizes: List<SpecialPrize>,
    val banzuke: Banzuke
)

data class Yusho(
    val type: String,
    val rikishiId: Int,
    val shikonaEn: String,
    val shikonaJp: String
)

data class SpecialPrize(
    val type: String,
    val rikishiId: Int,
    val shikonaEn: String,
    val shikonaJp: String
)

data class Torikumi(
    val id: String,
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

data class TorikumiResponse(
    val torikumi: List<Torikumi>
)

data class Rikishi(
    val side: String,
    val rikishiID: Int,
    val shikonaEn: String,
    val shikonaJp: String,
    val rankValue: Int,
    val rank: String,
    val wins: Int,
    val losses: Int,
    val absences: Int
)

data class Record(
    val result: String,
    val opponentShikonaEn: String,
    val opponentShikonaJp: String,
    val opponentID: Int,
    val kimarite: String
)

data class RikishiResponse(
    val limit: Int,
    val skip: Int,
    val total: Int,
    //Permitir que records sejam null | Fazer tratamento na classe
    val records: List<Rikishi>?
)