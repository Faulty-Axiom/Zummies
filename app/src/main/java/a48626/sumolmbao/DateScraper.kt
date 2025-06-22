import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

data class Tournament(
    val name: String,
    val firstDay: Date,
    val yyyymm: String
)

suspend fun scrapeSumoTournaments(): Triple<Tournament?, Tournament?, Tournament?> = withContext(Dispatchers.IO) {
    try {
        val url = "https://www.sumo.or.jp/EnTicket/year_schedule/"
        val doc = Jsoup.connect(url).get()
        val today = Calendar.getInstance().time
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val sectionBlocks = doc.select("div.mdSection1")
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)

        var previous: Tournament? = null
        var current: Tournament? = null
        var next: Tournament? = null

        for (section in sectionBlocks) {
            val h3 = section.selectFirst("h3.mdTtl6") ?: continue
            if (!h3.text().contains(currentYear.toString())) continue

            val table = section.selectFirst("table") ?: continue
            val rows = table.select("tbody tr")

            for (row in rows) {
                val cols = row.select("td")
                if (cols.size < 5) continue

                val name = cols[0].text()
                val dayCell = cols[4].html()
                val parts = dayCell.split("<br>.<br>")

                if (parts.size != 2) continue

                val firstDay = dateFormat.parse(parts[0].replace("<br>", "").trim()) ?: continue
                val lastDay = dateFormat.parse(parts[1].replace("<br>", "").trim()) ?: continue

                val tournament = Tournament(name, firstDay, SimpleDateFormat("yyyyMM").format(firstDay))

                if (today.before(firstDay)) {
                    next = tournament
                    break
                }

                if (!today.after(lastDay)) {
                    current = tournament
                }

                previous = tournament
            }
            break // only one year block needed
        }

        Triple(previous, current, next)

    } catch (e: Exception) {
        e.printStackTrace()
        Triple(null, null, null)
    }
}
