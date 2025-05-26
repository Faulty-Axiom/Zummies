package a48626.sumolmbao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException

object SumoDBScraper {
    suspend fun getHighestRank(sumodbId: Int): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://sumodb.sumogames.de/Rikishi.aspx?r=$sumodbId"
            val doc = Jsoup.connect(url).get()

            // Find the table cell with "Highest Rank" text
            val cells = doc.select("td:containsOwn(Highest Rank)")
            if (cells.isNotEmpty()) {
                // The next sibling td contains the value
                val nextCell = cells.first()?.nextElementSibling()
                return@withContext nextCell?.text()?.trim()
            }
            null
        } catch (e: IOException) {
            null
        }
    }
}