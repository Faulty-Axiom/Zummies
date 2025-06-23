// app/src/main/java/a48626/sumolmbao/second_fragment/SecondFragment.kt
package a48626.sumolmbao

import a48626.sumolmbao.data.Banzuke
import a48626.sumolmbao.data.Rikishi
import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.RankChange
import a48626.sumolmbao.data.TournamentResultDisplayData
import a48626.sumolmbao.data.RikishiMatchesResponse
import a48626.sumolmbao.database.RikishiDatabase
import a48626.sumolmbao.database.toRikishiDetails
import a48626.sumolmbao.favourites.FavouritesAdapter
import a48626.sumolmbao.favourites.FavouritesManager
import a48626.sumolmbao.retrofit.RetrofitInstance
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import scrapeSumoTournaments
import Tournament
import a48626.sumolmbao.second_fragment.TournamentResultAdapter

class SecondFragment : Fragment() {

    private lateinit var favouritesRecyclerView: RecyclerView
    private lateinit var favouritesAdapter: FavouritesAdapter
    private var allRikishiList: List<RikishiDetails> = emptyList()
    private var lastTournamentBanzukeMap: Map<Int, Rikishi>? = null

    private val clickedRikishiScores = mutableMapOf<Int, String>()
    private val cachedTournamentHistory = mutableMapOf<Int, List<TournamentResultDisplayData>>()
    private val gridVisibilityState = mutableMapOf<Int, Boolean>()


    private val majorDivisions = listOf(
        "Yokozuna", "Ozeki", "Sekiwake", "Komusubi", "Maegashira",
        "Juryo", "Makushita", "Sandanme", "Jonidan", "Jonokuchi"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favouritesRecyclerView = view.findViewById(R.id.favourites_recycler_view)
        favouritesRecyclerView.layoutManager = LinearLayoutManager(context)

        favouritesAdapter = FavouritesAdapter(
            emptyList(),
            null,
            { rikishi, scoreTextView ->
                handleRikishiItemClick(rikishi, scoreTextView)
            },
            clickedRikishiScores,
            cachedTournamentHistory,
            gridVisibilityState
        )
        favouritesRecyclerView.adapter = favouritesAdapter
    }

    override fun onResume() {
        super.onResume()
        loadAndDisplayFavourites()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("clickedRikishiScores", clickedRikishiScores as Serializable)
        outState.putSerializable("cachedTournamentHistory", cachedTournamentHistory as Serializable)
        outState.putSerializable("gridVisibilityState", gridVisibilityState as Serializable)
        Log.d("SecondFragment", "Saving state: clickedRikishiScores size = ${clickedRikishiScores.size}, gridVisibilityState size = ${gridVisibilityState.size}")
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            val savedScores = it.getSerializable("clickedRikishiScores") as? Map<Int, String>
            if (savedScores != null) {
                clickedRikishiScores.clear()
                clickedRikishiScores.putAll(savedScores)
            }
            val savedHistory = it.getSerializable("cachedTournamentHistory") as? Map<Int, List<TournamentResultDisplayData>>
            if (savedHistory != null) {
                cachedTournamentHistory.clear()
                cachedTournamentHistory.putAll(savedHistory)
            }
            val savedVisibility = it.getSerializable("gridVisibilityState") as? Map<Int, Boolean>
            if (savedVisibility != null) {
                gridVisibilityState.clear()
                gridVisibilityState.putAll(savedVisibility)
            }
            Log.d("SecondFragment", "Restored state: clickedRikishiScores size = ${clickedRikishiScores.size}, gridVisibilityState size = ${gridVisibilityState.size}")
        }
    }

    private fun loadAndDisplayFavourites() {
        val favouriteIds = FavouritesManager.getFavouriteRikishiIds(requireContext())
        val rikishiDao = RikishiDatabase.getDatabase(requireContext()).rikishiDao()

        lifecycleScope.launch {
            val allRikishiEntities = rikishiDao.getAllRikishi()
            allRikishiList = allRikishiEntities.map { it.toRikishiDetails() }

            val favouriteRikishi = allRikishiList.filter { rikishi ->
                favouriteIds.contains(rikishi.id.toString())
            }

            fetchLastTournamentBanzuke { banzukeMap ->
                lastTournamentBanzukeMap = banzukeMap
                favouritesAdapter.updateData(favouriteRikishi, lastTournamentBanzukeMap, clickedRikishiScores, cachedTournamentHistory, gridVisibilityState)
            }
        }
    }

    private fun fetchLastTournamentBanzuke(callback: (Map<Int, Rikishi>?) -> Unit) {
        val sharedPreferences = requireContext().getSharedPreferences("UserSelections", Context.MODE_PRIVATE)
        val selectedYear = sharedPreferences.getInt("lastApiYear", -1).takeIf { it != -1 }
        val selectedMonth = sharedPreferences.getInt("lastApiMonth", -1).takeIf { it != -1 }
        val selectedDivision = sharedPreferences.getString("lastApiDivision", null)

        if (selectedYear != null && selectedMonth != null && !selectedDivision.isNullOrBlank() && selectedDivision != "Division") {
            val bashoId = String.format("%04d%02d", selectedYear, selectedMonth)
            RetrofitInstance.api.getBanzuke(bashoId, selectedDivision).enqueue(object : Callback<Banzuke> {
                override fun onResponse(call: Call<Banzuke>, response: Response<Banzuke>) {
                    if (response.isSuccessful) {
                        val banzuke = response.body()
                        val banzukeMap = mutableMapOf<Int, Rikishi>()
                        banzuke?.east?.forEach { rikishi -> banzukeMap[rikishi.rikishiID] = rikishi }
                        banzuke?.west?.forEach { rikishi -> banzukeMap[rikishi.rikishiID] = rikishi }
                        callback(banzukeMap)
                    } else {
                        Log.e("SecondFragment", "Error fetching banzuke (last tournament): ${response.code()}")
                        callback(null)
                    }
                }

                override fun onFailure(call: Call<Banzuke>, t: Throwable) {
                    Log.e("SecondFragment", "Network error fetching banzuke (last tournament): ${t.message}")
                    callback(null)
                }
            })
        } else {
            callback(null)
        }
    }

    private fun handleRikishiItemClick(rikishi: RikishiDetails, scoreTextView: TextView) {
        val viewHolder = favouritesRecyclerView.findViewHolderForAdapterPosition(
            favouritesAdapter.favouriteRikishiList.indexOf(rikishi)
        ) as? FavouritesAdapter.FavouriteViewHolder

        val gridContainer = viewHolder?.itemView?.findViewById<LinearLayout>(R.id.tournamentGridContainer)
        val tournamentGridView = viewHolder?.itemView?.findViewById<RecyclerView>(R.id.tournamentGridView)

        val isGridCurrentlyVisible = gridVisibilityState[rikishi.id] == true

        if (isGridCurrentlyVisible) {
            gridContainer?.visibility = View.GONE
            scoreTextView.visibility = View.GONE
            scoreTextView.text = ""
            clickedRikishiScores.remove(rikishi.id)
            gridVisibilityState[rikishi.id] = false
        } else if (cachedTournamentHistory.containsKey(rikishi.id) && clickedRikishiScores.containsKey(rikishi.id)) {
            scoreTextView.text = clickedRikishiScores[rikishi.id]
            scoreTextView.visibility = View.VISIBLE
            gridContainer?.visibility = View.VISIBLE
            tournamentGridView?.adapter = TournamentResultAdapter(cachedTournamentHistory[rikishi.id] ?: emptyList())
            tournamentGridView?.layoutManager = GridLayoutManager(context, 3)
            gridVisibilityState[rikishi.id] = true
        } else {
            lifecycleScope.launch {
                checkWrestlerScoreAndHistoryInPreviousTournament(rikishi, scoreTextView, gridContainer, tournamentGridView)
            }
        }
    }

    private suspend fun checkWrestlerScoreAndHistoryInPreviousTournament(
        rikishi: RikishiDetails,
        scoreTextView: TextView,
        gridContainer: LinearLayout?,
        tournamentGridView: RecyclerView?
    ) {
        withContext(Dispatchers.Main) {
            scoreTextView.text = "Searching..."
            scoreTextView.visibility = View.VISIBLE
            gridContainer?.visibility = View.GONE
        }

        val previousTournament = scrapeSumoTournaments().first

        if (previousTournament == null) {
            withContext(Dispatchers.Main) {
                scoreTextView.text = "Could not find previous tournament info."
                Toast.makeText(context, "Could not find previous tournament info.", Toast.LENGTH_SHORT).show()
                scoreTextView.visibility = View.GONE
                clickedRikishiScores.remove(rikishi.id)
                gridVisibilityState[rikishi.id] = false
            }
            return
        }

        // Fetch all matches for the rikishi from getRikishiMatches
        val rikishiMatchesResponse: RikishiMatchesResponse? = try {
            RetrofitInstance.api.getRikishiMatches(rikishiId = rikishi.id) // Corrected: Call suspend fun directly
        } catch (e: Exception) {
            Log.e("SecondFragment", "Network error fetching matches for ${rikishi.shikonaEn}: ${e.message}")
            null // Return null on exception
        }

        val allMatches = rikishiMatchesResponse?.matches ?: emptyList() // Access .matches property
        Log.d("SecondFragment", "Fetched ${allMatches.size} total matches for ${rikishi.shikonaEn}. Sample: ${allMatches.take(3)}")


        // 1. Get rank history for the wrestler
        val allRankChanges: List<RankChange> = try {
            RetrofitInstance.api.getRanks(rikishiId = rikishi.id) // Corrected: Call suspend fun directly
        } catch (e: Exception) {
            Log.e("SecondFragment", "Network error fetching ranks for ${rikishi.shikonaEn}: ${e.message}")
            emptyList()
        }

        val filteredRankChanges = allRankChanges // Corrected: Removed unnecessary safe call '?'
            .distinctBy { it.bashoId }
            .sortedByDescending { it.bashoId }
            .take(6)


        Log.d("SecondFragment", "Parsed rank changes for ${rikishi.shikonaEn}: $filteredRankChanges")


        if (filteredRankChanges.isEmpty()) {
            withContext(Dispatchers.Main) {
                scoreTextView.text = "No historical rank data found."
                Toast.makeText(context, "No historical rank data found for ${rikishi.shikonaEn}.", Toast.LENGTH_SHORT).show()
                scoreTextView.visibility = View.GONE
                clickedRikishiScores.remove(rikishi.id)
                gridVisibilityState[rikishi.id] = false
            }
            return
        }

        val tournamentResults = mutableListOf<TournamentResultDisplayData>()

        // Loop through the last 6 rank changes to find scores
        for (rankChange in filteredRankChanges) {
            val bashoId = rankChange.bashoId
            val rank = shortenRank(rankChange.rank)
            val date = formatBashoIdToMonthYear(bashoId)

            // Calculate wins and losses for this specific bashoId from allMatches
            var wins = 0
            var losses = 0
            var absences = 0

            val matchesInBasho = allMatches.filter { it.bashoId == bashoId }
            Log.d("SecondFragment", "Basho: $bashoId, Matches found: ${matchesInBasho.size}")

            if (matchesInBasho.isNotEmpty()) {
                for (match in matchesInBasho) {
                    Log.d("SecondFragment", "Match in $bashoId: winnerId=${match.winnerId}, rikishiId=${rikishi.id}, isWinner=${match.winnerId == rikishi.id}")
                    if (match.winnerId == rikishi.id) {
                        wins++
                    } else {
                        losses++
                    }
                }
            }

            val score = formatRecord(wins, losses, absences)
            tournamentResults.add(TournamentResultDisplayData(rank, score, date))
        }

        while (tournamentResults.size < 6) {
            tournamentResults.add(TournamentResultDisplayData("-", "-", "-"))
        }

        tournamentResults.reverse() // Reverse for oldest to newest for left-to-right grid fill

        withContext(Dispatchers.Main) {
            val initialScore = tournamentResults.lastOrNull()?.score ?: "N/A"
            scoreTextView.text = initialScore
            scoreTextView.visibility = View.VISIBLE
            clickedRikishiScores[rikishi.id] = initialScore

            cachedTournamentHistory[rikishi.id] = tournamentResults

            if (tournamentGridView != null && gridContainer != null) {
                tournamentGridView.adapter = TournamentResultAdapter(tournamentResults)
                tournamentGridView.layoutManager = GridLayoutManager(context, 3)
                gridContainer.visibility = View.VISIBLE
                gridVisibilityState[rikishi.id] = true
            }
            Toast.makeText(context, "${rikishi.shikonaEn}'s tournament history loaded.", Toast.LENGTH_LONG).show()
        }
    }

    /*
    private suspend fun fetchBanzukeForTournamentAndDivisionBlocking(bashoId: String, division: String): Map<Int, Rikishi>? = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.api.getBanzuke(bashoId, division).execute()
            if (response.isSuccessful) {
                val banzuke = response.body()
                val banzukeMap = mutableMapOf<Int, Rikishi>()
                banzuke?.east?.forEach { rikishi -> banzukeMap[rikishi.rikishiID] = rikishi }
                banzuke?.west?.forEach { rikishi -> banzukeMap[rikishi.rikishiID] = rikishi }
                banzukeMap
            } else {
                Log.e("SecondFragment", "Error fetching banzuke for $bashoId in $division: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("SecondFragment", "Network error fetching banzuke for $bashoId in $division: ${e.message}")
            null
        }
    }
    */

    private fun convertRankToDivision(fullRank: String?): String {
        if (fullRank.isNullOrBlank()) return "Unknown"
        return when {
            fullRank.startsWith("Yokozuna", true) -> "Yokozuna"
            fullRank.startsWith("Ozeki", true) -> "Ozeki"
            fullRank.startsWith("Sekiwake", true) -> "Sekiwake"
            fullRank.startsWith("Komusubi", true) -> "Komusubi"
            fullRank.startsWith("Maegashira", true) -> "Makuuchi"
            fullRank.startsWith("Juryo", true) -> "Juryo"
            fullRank.startsWith("Makushita", true) -> "Makushita"
            "Sandanme".equals(fullRank, true) -> "Sandanme"
            "Jonidan".equals(fullRank, true) -> "Jonidan"
            "Jonokuchi".equals(fullRank, true) -> "Jonokuchi"
            else -> "Unknown"
        }
    }

    private fun formatBashoIdToMonthYear(bashoId: String): String {
        if (bashoId.length != 6) return bashoId
        try {
            val year = bashoId.substring(0, 4)
            val monthNum = bashoId.substring(4, 6)
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.MONTH, monthNum.toInt() - 1)
            val monthName = SimpleDateFormat("MMM", Locale.ENGLISH).format(calendar.time)
            return "$monthName ${year.substring(2,4)}"
        } catch (e: Exception) {
            Log.e("DateFormatter", "Error formatting bashoId: $bashoId", e)
            return bashoId
        }
    }

    private fun shortenRank(fullRank: String?): String {
        if (fullRank.isNullOrBlank()) return ""
        val regex = Regex("""(Yokozuna|Ozeki|Sekiwake|Komusubi|Maegashira|Juryo|Makushita|Sandanme|Jonidan|Jonokuchi)\s*(\d*)\s*(East|West)?""", RegexOption.IGNORE_CASE)
        val match = regex.find(fullRank)

        return match?.let {
            val (name, number, side) = it.destructured
            val shortRank = when (name.lowercase()) {
                "yokozuna" -> "Y"
                "ozeki" -> "O"
                "sekiwake" -> "S"
                "komusubi" -> "K"
                "maegashira" -> "M"
                "juryo" -> "J"
                "makushita" -> "Ms"
                "sandanme" -> "Sd"
                "jonidan" -> "Jd"
                "jonokuchi" -> "Jk"
                else -> name.substring(0, Math.min(2, name.length))
            }
            if (number.isNotBlank()) {
                val shortSide = when (side.lowercase()) {
                    "east" -> "e"
                    "west" -> "w"
                    else -> ""
                }
                "$shortRank$number$shortSide"
            } else {
                shortRank
            }
        } ?: fullRank.split(" ").firstOrNull() ?: fullRank
    }

    private fun formatRecord(wins: Int?, losses: Int?, absences: Int?): String {
        val w = wins ?: 0
        val l = losses ?: 0
        val a = absences ?: 0
        return if (a > 0) "$w-$l-$a" else "$w-$l"
    }
}