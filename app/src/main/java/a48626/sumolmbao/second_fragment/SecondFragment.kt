// app/src/main/java/a48626/sumolmbao/second_fragment/SecondFragment.kt
package a48626.sumolmbao

import a48626.sumolmbao.data.Banzuke
import a48626.sumolmbao.data.Rikishi
import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.database.RikishiDatabase
import a48626.sumolmbao.database.toRikishiDetails
import a48626.sumolmbao.favourites.FavouritesAdapter
import a48626.sumolmbao.favourites.FavouritesManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

// Correct Retrofit imports
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Import the scrapeSumoTournaments function and Tournament data class
import scrapeSumoTournaments
import Tournament
import a48626.sumolmbao.retrofit.RetrofitInstance

class SecondFragment : Fragment() {

    private lateinit var favouritesRecyclerView: RecyclerView
    private lateinit var favouritesAdapter: FavouritesAdapter
    private var allRikishiList: List<RikishiDetails> = emptyList()
    private var lastTournamentBanzukeMap: Map<Int, Rikishi>? = null
    // In-memory cache for scores specifically fetched via "Check Score" button
    private val clickedRikishiScores = mutableMapOf<Int, String>()

    // Define all major divisions in order for rank checking
    private val majorDivisions = listOf(
        "Yokozuna", "Ozeki", "Sekiwake", "Komusubi", "Maegashira",
        "Juryo", "Makushita", "Sandanme", "Jonidan", "Jonokuchi"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favouritesRecyclerView = view.findViewById(R.id.favourites_recycler_view)
        favouritesRecyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize adapter with the click listener and the new cache map
        favouritesAdapter = FavouritesAdapter(
            emptyList(),
            null,
            { rikishi, scoreTextView ->
                // Toggle visibility of the score TextView
                if (scoreTextView.visibility == View.VISIBLE && clickedRikishiScores.containsKey(rikishi.id)) {
                    // If currently showing a cached score from explicit lookup, hide it and remove from cache
                    scoreTextView.visibility = View.GONE
                    scoreTextView.text = "" // Clear text when hiding
                    clickedRikishiScores.remove(rikishi.id)
                } else if (clickedRikishiScores.containsKey(rikishi.id)) {
                    // If a cached score from explicit lookup exists but is not visible, show it
                    scoreTextView.text = clickedRikishiScores[rikishi.id]
                    scoreTextView.visibility = View.VISIBLE
                } else {
                    // No cached score for explicit lookup, proceed to fetch
                    lifecycleScope.launch {
                        checkWrestlerScoreInPreviousTournament(rikishi, scoreTextView)
                    }
                }
            },
            clickedRikishiScores // Pass the mutable map to the adapter
        )
        favouritesRecyclerView.adapter = favouritesAdapter
    }

    override fun onResume() {
        super.onResume()
        // Load and display favourites every time the fragment is shown
        // This will now correctly use the restored clickedRikishiScores
        loadAndDisplayFavourites()
    }

    // Save the state of clickedRikishiScores when the fragment is paused or destroyed
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Put the entire map as a Serializable object
        outState.putSerializable("clickedRikishiScores", clickedRikishiScores as Serializable)
        Log.d("SecondFragment", "Saving state: clickedRikishiScores size = ${clickedRikishiScores.size}")
    }

    // Restore the state of clickedRikishiScores when the fragment view is recreated
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            // Retrieve the map from the bundle
            val savedMap = it.getSerializable("clickedRikishiScores") as? Map<Int, String>
            if (savedMap != null) {
                clickedRikishiScores.clear()
                clickedRikishiScores.putAll(savedMap)
                Log.d("SecondFragment", "Restored state: clickedRikishiScores size = ${clickedRikishiScores.size}")
            }
        }
        // onResume will be called after this, which will update the adapter with the restored data.
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
                // Pass the new cache map along with others
                favouritesAdapter.updateData(favouriteRikishi, lastTournamentBanzukeMap, clickedRikishiScores)
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


    private suspend fun checkWrestlerScoreInPreviousTournament(rikishi: RikishiDetails, scoreTextView: TextView) {
        // Show a loading or "searching" message immediately on the UI thread
        withContext(Dispatchers.Main) {
            scoreTextView.text = "Searching..."
            scoreTextView.visibility = View.VISIBLE
        }

        val previousTournament = scrapeSumoTournaments().first

        if (previousTournament == null) {
            withContext(Dispatchers.Main) {
                scoreTextView.text = "Could not find previous tournament info."
                Toast.makeText(context, "Could not find previous tournament info.", Toast.LENGTH_SHORT).show()
                scoreTextView.visibility = View.GONE // Hide it or leave error
                clickedRikishiScores.remove(rikishi.id) // Ensure removed if error
            }
            return
        }

        val previousBashoId = previousTournament.yyyymm
        val currentRankDivision = convertRankToDivision(rikishi.currentRank)

        val divisionsToCheck = mutableListOf<String>()
        val makuuchiRanks = listOf("Yokozuna", "Ozeki", "Sekiwake", "Komusubi", "Maegashira")

        // Determine the initial division to check
        if (currentRankDivision.equals("Makuuchi", ignoreCase = true) || makuuchiRanks.any { rikishi.currentRank.startsWith(it, true) }) {
            divisionsToCheck.add("Makuuchi")
        } else {
            divisionsToCheck.add(currentRankDivision)
        }


        // Add fallback divisions based on rules
        when (currentRankDivision) {
            "Jonokuchi" -> if (!divisionsToCheck.contains("Jonidan")) divisionsToCheck.add("Jonidan")
            "Makuuchi" -> if (!divisionsToCheck.contains("Juryo")) divisionsToCheck.add("Juryo")
            // For other ranks, check above and below
            else -> {
                val currentIndex = majorDivisions.indexOf(currentRankDivision)
                if (currentIndex != -1) {
                    if (currentIndex > 0) { // Rank above
                        val rankAbove = majorDivisions[currentIndex - 1]
                        if (!divisionsToCheck.contains(rankAbove)) divisionsToCheck.add(rankAbove)
                    }
                    if (currentIndex < majorDivisions.size - 1) { // Rank below
                        val rankBelow = majorDivisions[currentIndex + 1]
                        if (!divisionsToCheck.contains(rankBelow)) divisionsToCheck.add(rankBelow)
                    }
                }
            }
        }

        // Remove duplicates and "Division" placeholder
        val uniqueDivisionsToCheck = divisionsToCheck.distinct().filter { it != "Division" }

        Log.d("SecondFragment", "Checking divisions for ${rikishi.shikonaEn}: $uniqueDivisionsToCheck")

        var foundRikishiInBanzuke: Rikishi? = null

        // Iterate through divisions and fetch banzuke synchronously within this coroutine
        for (division in uniqueDivisionsToCheck) {
            val banzukeMap = fetchBanzukeForTournamentAndDivisionBlocking(previousBashoId, division)
            foundRikishiInBanzuke = banzukeMap?.get(rikishi.id)
            if (foundRikishiInBanzuke != null) {
                break // Found the rikishi, stop searching further divisions
            }
        }

        // Update UI based on results on the main thread
        withContext(Dispatchers.Main) {
            if (foundRikishiInBanzuke != null) {
                val score = formatRecord(foundRikishiInBanzuke.wins, foundRikishiInBanzuke.losses, foundRikishiInBanzuke.absences)
                scoreTextView.text = score
                scoreTextView.visibility = View.VISIBLE
                // Store the found score in the in-memory cache
                clickedRikishiScores[rikishi.id] = score
                Toast.makeText(context, "${rikishi.shikonaEn}'s score in previous tournament: $score", Toast.LENGTH_LONG).show()
            } else {
                scoreTextView.text = "Score not found in previous tournament or division."
                scoreTextView.visibility = View.VISIBLE
                // Ensure this state is also persistent in the cache if it's the result
                clickedRikishiScores[rikishi.id] = scoreTextView.text.toString() // Store the "not found" message
                Toast.makeText(context, "${rikishi.shikonaEn}'s score not found in previous tournament or division.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // New suspend function for blocking API call within a coroutine
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

    // Helper function to convert full rank string to its main division
    private fun convertRankToDivision(fullRank: String?): String {
        if (fullRank.isNullOrBlank()) return "Unknown"
        return when {
            fullRank.startsWith("Yokozuna", true) -> "Yokozuna"
            fullRank.startsWith("Ozeki", true) -> "Ozeki"
            fullRank.startsWith("Sekiwake", true) -> "Sekiwake"
            fullRank.startsWith("Komusubi", true) -> "Komusubi"
            fullRank.startsWith("Maegashira", true) -> "Maegashira"
            fullRank.startsWith("Juryo", true) -> "Juryo"
            fullRank.startsWith("Makushita", true) -> "Makushita"
            fullRank.startsWith("Sandanme", true) -> "Sandanme"
            fullRank.startsWith("Jonidan", true) -> "Jonidan"
            fullRank.startsWith("Jonokuchi", true) -> "Jonokuchi"
            else -> "Unknown"
        }
    }

    private fun formatRecord(wins: Int?, losses: Int?, absences: Int?): String {
        val w = wins ?: 0
        val l = losses ?: 0
        val a = absences ?: 0
        return if (a > 0) "$w-$l-$a" else "$w-$l"
    }
}