package a48626.sumolmbao

import a48626.sumolmbao.data.Banzuke
import a48626.sumolmbao.data.Rikishi
import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.RankChange
import a48626.sumolmbao.data.TournamentResultDisplayData
import a48626.sumolmbao.data.RikishiMatchesListResponse // Correct import for list response
import a48626.sumolmbao.data.RikishiMatch // Import RikishiMatch
import a48626.sumolmbao.database.RikishiDatabase
import a48626.sumolmbao.database.toRikishiDetails
import a48626.sumolmbao.favourites.FavouritesAdapter
import a48626.sumolmbao.favourites.FavouritesManager
import a48626.sumolmbao.retrofit.RetrofitInstance
import a48626.sumolmbao.second_fragment.DivisionFilterAdapter
import a48626.sumolmbao.second_fragment.RikishiBoutsAdapter // Import RikishiBoutsAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine // For converting callback to suspend
import kotlin.coroutines.resume // For converting callback to suspend
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import scrapeSumoTournaments // Import the scrapeSumoTournaments function
import Tournament // Import the Tournament data class

sealed class DisplayItem : Serializable {
    data class RikishiDisplayItem(val rikishiDetails: RikishiDetails) : DisplayItem()
    data class DivisionSeparatorItem(val divisionName: String) : DisplayItem()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DisplayItem

        return when (this) {
            is RikishiDisplayItem -> other is RikishiDisplayItem && this.rikishiDetails.id == other.rikishiDetails.id
            is DivisionSeparatorItem -> other is DivisionSeparatorItem && this.divisionName == other.divisionName
        }
    }

    override fun hashCode(): Int {
        return when (this) {
            is RikishiDisplayItem -> rikishiDetails.id.hashCode()
            is DivisionSeparatorItem -> divisionName.hashCode()
        }
    }
}

class SecondFragment : Fragment() {

    private lateinit var favouritesRecyclerView: RecyclerView
    private lateinit var favouritesAdapter: FavouritesAdapter
    private var allRikishiList: List<RikishiDetails> = emptyList()
    private var sortedFavouriteRikishi: List<RikishiDetails> = emptyList()
    private var lastTournamentBanzukeMap: Map<Int, Rikishi>? = null

    private val cachedTournamentHistory = mutableMapOf<Int, List<TournamentResultDisplayData>>()
    private val gridVisibilityState = mutableMapOf<Int, Boolean>()
    private val cachedRikishiBouts = mutableMapOf<Int, List<RikishiMatch>>()
    private val boutsVisibilityState = mutableMapOf<Int, Boolean>()

    private val divisionExpandedState = mutableMapOf<String, Boolean>()

    private lateinit var showSeparatedDivisionToggle: Switch
    private lateinit var divisionFilterButton: TextView
    private lateinit var divisionFilterRecyclerView: RecyclerView
    private lateinit var divisionFilterAdapter: DivisionFilterAdapter
    private lateinit var overlay: View

    private var isShowSeparatedDivisionEnabled: Boolean = false
    private var selectedDivisionFilter: String = "All"
    private var isSpoilerModeEnabled: Boolean = false

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

        showSeparatedDivisionToggle = view.findViewById(R.id.showSeparatedDivisionToggle)
        divisionFilterButton = view.findViewById(R.id.divisionFilterButton)
        divisionFilterRecyclerView = view.findViewById(R.id.divisionFilterRecyclerView)
        divisionFilterRecyclerView.layoutManager = LinearLayoutManager(context)
        overlay = view.findViewById(R.id.overlay)

        favouritesAdapter = FavouritesAdapter(
            emptyList(),
            null,
            { rikishi -> handleRikishiItemClick(rikishi) },
            cachedTournamentHistory,
            gridVisibilityState,
            onDivisionHeaderClick = { divisionName -> toggleDivisionVisibility(divisionName) }
        )
        favouritesRecyclerView.adapter = favouritesAdapter

        divisionFilterAdapter = DivisionFilterAdapter(
            emptyList(),
            onDivisionSelected = { divisionName ->
                selectedDivisionFilter = divisionName
                FavouritesManager.saveSelectedDivisionFilter(requireContext(), divisionName)
                divisionFilterButton.text = divisionName
                hideDivisionFilterRecyclerView()
                updateFavouritesDisplay()
            }
        )
        divisionFilterRecyclerView.adapter = divisionFilterAdapter

        isShowSeparatedDivisionEnabled = FavouritesManager.loadShowSeparatedDivisionPreference(requireContext())
        showSeparatedDivisionToggle.isChecked = isShowSeparatedDivisionEnabled
        selectedDivisionFilter = FavouritesManager.loadSelectedDivisionFilter(requireContext())
        divisionFilterButton.text = selectedDivisionFilter
        updateFilterButtonState(isShowSeparatedDivisionEnabled)

        showSeparatedDivisionToggle.setOnCheckedChangeListener { _, isChecked ->
            isShowSeparatedDivisionEnabled = isChecked
            FavouritesManager.saveShowSeparatedDivisionPreference(requireContext(), isChecked)
            updateFilterButtonState(isChecked)

            if (!isChecked) {
                selectedDivisionFilter = "All"
                FavouritesManager.saveSelectedDivisionFilter(requireContext(), "All")
                divisionFilterButton.text = "All"
                hideDivisionFilterRecyclerView()
            }

            updateFavouritesDisplay()
        }

        divisionFilterButton.setOnClickListener {
            toggleDivisionFilterRecyclerView()
        }

        overlay.setOnClickListener {
            hideDivisionFilterRecyclerView()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            loadAndDisplayFavourites()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("cachedTournamentHistory", cachedTournamentHistory as Serializable)
        outState.putSerializable("gridVisibilityState", gridVisibilityState as Serializable)
        outState.putSerializable("divisionExpandedState", divisionExpandedState as Serializable)
        outState.putBoolean("isShowSeparatedDivisionEnabled", isShowSeparatedDivisionEnabled)
        outState.putString("selectedDivisionFilter", selectedDivisionFilter)
        outState.putSerializable("cachedRikishiBouts", cachedRikishiBouts as Serializable)
        outState.putSerializable("boutsVisibilityState", boutsVisibilityState as Serializable)

        Log.d("SecondFragment", "Saving state: cachedTournamentHistory size = ${cachedTournamentHistory.size}, gridVisibilityState size = ${gridVisibilityState.size}, divisionExpandedState size = ${divisionExpandedState.size}")
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
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
            val savedDivisionExpandedState = it.getSerializable("divisionExpandedState") as? Map<String, Boolean>
            if (savedDivisionExpandedState != null) {
                divisionExpandedState.clear()
                divisionExpandedState.putAll(savedDivisionExpandedState)
            }
            val savedBouts = it.getSerializable("cachedRikishiBouts") as? Map<Int, List<RikishiMatch>>
            if (savedBouts != null) {
                cachedRikishiBouts.clear()
                cachedRikishiBouts.putAll(savedBouts)
            }
            val savedBoutsVisibility = it.getSerializable("boutsVisibilityState") as? Map<Int, Boolean>
            if (savedBoutsVisibility != null) {
                boutsVisibilityState.clear()
                boutsVisibilityState.putAll(savedBoutsVisibility)
            }

            isShowSeparatedDivisionEnabled = it.getBoolean("isShowSeparatedDivisionEnabled", false)
            selectedDivisionFilter = it.getString("selectedDivisionFilter", "All") ?: "All"

            showSeparatedDivisionToggle.isChecked = isShowSeparatedDivisionEnabled
            divisionFilterButton.text = selectedDivisionFilter
            updateFilterButtonState(isShowSeparatedDivisionEnabled)

            Log.d("SecondFragment", "Restored state: cachedTournamentHistory size = ${cachedTournamentHistory.size}, gridVisibilityState size = ${gridVisibilityState.size}, divisionExpandedState size = ${divisionExpandedState.size}")
        }
    }

    private suspend fun loadAndDisplayFavourites() {
        val currentFavouriteIds = FavouritesManager.getFavouriteRikishiIds(requireContext())
        val currentFavouritesHash = currentFavouriteIds.sorted().joinToString(",")

        val lastSavedFavouritesHash = FavouritesManager.loadFavouritesHash(requireContext())

        if (currentFavouritesHash == lastSavedFavouritesHash && FavouritesManager.loadSortedFavouritesCache(requireContext()) != null) {
            sortedFavouriteRikishi = FavouritesManager.loadSortedFavouritesCache(requireContext())!!
            Log.d("SecondFragment", "Loaded sorted favourites from cache. Size: ${sortedFavouriteRikishi.size}")
        } else {
            Log.d("SecondFragment", "Cache invalid or empty, rebuilding sorted favourites.")
            val rikishiDao = RikishiDatabase.getDatabase(requireContext()).rikishiDao()

            val fetchedRikishiDetails = withContext(Dispatchers.IO) {
                val allRikishiEntities = rikishiDao.getAllRikishi()
                allRikishiEntities.map { it.toRikishiDetails() }
            }

            val favouriteRikishi = fetchedRikishiDetails.filter { rikishi ->
                currentFavouriteIds.contains(rikishi.id.toString())
            }

            sortedFavouriteRikishi = favouriteRikishi.sortedWith(Comparator { r1, r2 ->
                val isR1Retired = r1.currentRank?.contains("Retired", ignoreCase = true) == true || r1.intai == true
                val isR2Retired = r2.currentRank?.contains("Retired", ignoreCase = true) == true || r2.intai == true

                when {
                    isR1Retired && !isR2Retired -> 1
                    !isR1Retired && isR2Retired -> -1
                    isR1Retired && isR2Retired -> r1.shikonaEn.compareTo(r2.shikonaEn, ignoreCase = true)
                    else -> {
                        val rankValue1 = parseRankValue(r1.currentRank)
                        val rankValue2 = parseRankValue(r2.currentRank)

                        val rankComparison = rankValue1.compareTo(rankValue2)
                        if (rankComparison == 0) {
                            val side1 = getRankSide(r1.currentRank)
                            val side2 = getRankSide(r2.currentRank)
                            val sideValue1 = if (side1 == "East") 0 else 1
                            val sideValue2 = if (side2 == "East") 0 else 1
                            sideValue1.compareTo(sideValue2)
                        } else {
                            rankComparison
                        }
                    }
                }
            })
            FavouritesManager.saveSortedFavouritesCache(requireContext(), sortedFavouriteRikishi)
            FavouritesManager.saveFavouritesHash(requireContext(), currentFavouritesHash)
        }

        divisionFilterAdapter.updateData(buildFilterDivisionList(FavouritesManager.loadSegmentedTopDivisionPreference(requireContext())))
        divisionFilterAdapter.updateSelectedDivision(selectedDivisionFilter)

        val currentFilterDivisions = buildFilterDivisionList(FavouritesManager.loadSegmentedTopDivisionPreference(requireContext()))
        if (divisionExpandedState.isEmpty() || !divisionExpandedState.keys.containsAll(currentFilterDivisions)) {
            divisionExpandedState.clear()
            currentFilterDivisions.forEach { divisionExpandedState[it] = true }
        }

        lastTournamentBanzukeMap = fetchLastTournamentBanzukeSuspend()

        updateFavouritesDisplay()
    }

    private fun updateFavouritesDisplay() {
        favouritesAdapter.updateData(buildDisplayItems(
            FavouritesManager.loadSegmentedTopDivisionPreference(requireContext()),
            isShowSeparatedDivisionEnabled,
            selectedDivisionFilter
        ), lastTournamentBanzukeMap, cachedTournamentHistory, gridVisibilityState)
    }

    private fun buildDisplayItems(
        segmentedTopDivisionEnabled: Boolean,
        showSeparatedDivisionEnabled: Boolean,
        filterDivision: String
    ): List<DisplayItem> {
        val displayItems = mutableListOf<DisplayItem>()
        var currentDivisionName: String? = null

        val filteredRikishi = if (filterDivision == "All") {
            sortedFavouriteRikishi
        } else {
            sortedFavouriteRikishi.filter { rikishi ->
                getDivisionForDisplay(rikishi, segmentedTopDivisionEnabled) == filterDivision
            }
        }

        if (!showSeparatedDivisionEnabled) {
            filteredRikishi.forEach { displayItems.add(DisplayItem.RikishiDisplayItem(it)) }
            return displayItems
        }

        for (rikishi in filteredRikishi) {
            val division = getDivisionForDisplay(rikishi, segmentedTopDivisionEnabled)

            if (filterDivision == "All") {
                if (division != currentDivisionName) {
                    displayItems.add(DisplayItem.DivisionSeparatorItem(division))
                    currentDivisionName = division
                }
            }

            if (divisionExpandedState[division] == true || filterDivision != "All") {
                displayItems.add(DisplayItem.RikishiDisplayItem(rikishi))
            }
        }
        return displayItems
    }

    private fun buildFilterDivisionList(segmentedTopDivisionEnabled: Boolean): List<String> {
        val divisions = mutableSetOf<String>()
        divisions.add("All")

        val activeDivisions = mutableSetOf<String>()
        for (rikishi in sortedFavouriteRikishi) {
            val division = getDivisionForDisplay(rikishi, segmentedTopDivisionEnabled)
            activeDivisions.add(division)
        }

        val orderedDivisions = mutableListOf<String>()
        if (segmentedTopDivisionEnabled) {
            orderedDivisions.addAll(listOf("Yokozuna", "Ozeki", "Sekiwake", "Komusubi", "Maegashira"))
        } else {
            orderedDivisions.add("Makuuchi")
        }
        orderedDivisions.addAll(listOf("Juryo", "Makushita", "Sandanme", "Jonidan", "Jonokuchi", "Retired"))


        val finalDivisions = mutableListOf<String>()
        finalDivisions.add("All")
        for (div in orderedDivisions) {
            if (activeDivisions.contains(div)) {
                finalDivisions.add(div)
            }
        }

        return finalDivisions
    }


    private fun toggleDivisionVisibility(divisionName: String) {
        val isExpanded = divisionExpandedState[divisionName] ?: true
        divisionExpandedState[divisionName] = !isExpanded
        updateFavouritesDisplay()
    }

    private fun updateFilterButtonState(isEnabled: Boolean) {
        if (isEnabled) {
            divisionFilterButton.alpha = 1.0f
            divisionFilterButton.isClickable = true
            divisionFilterButton.setOnClickListener { toggleDivisionFilterRecyclerView() }
        } else {
            divisionFilterButton.alpha = 0.5f
            divisionFilterButton.isClickable = false
            divisionFilterButton.setOnClickListener(null)
            hideDivisionFilterRecyclerView()
        }
    }

    private fun toggleDivisionFilterRecyclerView() {
        if (divisionFilterRecyclerView.visibility == View.VISIBLE) {
            hideDivisionFilterRecyclerView()
        } else {
            showDivisionFilterRecyclerView()
        }
    }

    private fun showDivisionFilterRecyclerView() {
        divisionFilterRecyclerView.visibility = View.VISIBLE
        overlay.visibility = View.VISIBLE
        divisionFilterRecyclerView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_down))
    }

    private fun hideDivisionFilterRecyclerView() {
        divisionFilterRecyclerView.visibility = View.GONE
        divisionFilterRecyclerView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_up))
        overlay.visibility = View.GONE
    }


    private suspend fun fetchLastTournamentBanzukeSuspend(): Map<Int, Rikishi>? {
        return suspendCancellableCoroutine { continuation ->
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
                            continuation.resume(banzukeMap)
                        } else {
                            Log.e("SecondFragment", "Error fetching banzuke (last tournament): ${response.code()}")
                            continuation.resume(null)
                        }
                    }

                    override fun onFailure(call: Call<Banzuke>, t: Throwable) {
                        Log.e("SecondFragment", "Network error fetching banzuke (last tournament): ${t.message}")
                        continuation.resume(null)
                    }
                })
            } else {
                continuation.resume(null)
            }
        }
    }


    private fun handleRikishiItemClick(rikishi: RikishiDetails) {
        val itemPosition = favouritesAdapter.favouriteRikishiList.indexOfFirst { displayItem ->
            displayItem is DisplayItem.RikishiDisplayItem && displayItem.rikishiDetails.id == rikishi.id
        }

        if (itemPosition == RecyclerView.NO_POSITION) {
            Log.e("SecondFragment", "Rikishi not found in adapter list for click: ${rikishi.shikonaEn}")
            return
        }

        val viewHolder = favouritesRecyclerView.findViewHolderForAdapterPosition(itemPosition) as? FavouritesAdapter.RikishiViewHolder

        val gridContainer = viewHolder?.itemView?.findViewById<LinearLayout>(R.id.tournamentGridContainer)
        val tournamentGridView = viewHolder?.itemView?.findViewById<RecyclerView>(R.id.tournamentGridView)
        val rikishiBoutsRecyclerView = viewHolder?.itemView?.findViewById<RecyclerView>(R.id.rikishiBoutsRecyclerView)


        val isGridCurrentlyVisible = gridVisibilityState[rikishi.id] == true
        val areBoutsCurrentlyVisible = boutsVisibilityState[rikishi.id] == true


        if (isGridCurrentlyVisible || areBoutsCurrentlyVisible) {
            // Hide both past results and bouts
            gridContainer?.visibility = View.GONE
            rikishiBoutsRecyclerView?.visibility = View.GONE
            gridVisibilityState[rikishi.id] = false
            boutsVisibilityState[rikishi.id] = false
        } else {
            // Show past results
            if (cachedTournamentHistory.containsKey(rikishi.id)) {
                gridContainer?.visibility = View.VISIBLE
                tournamentGridView?.adapter = a48626.sumolmbao.second_fragment.TournamentResultAdapter(cachedTournamentHistory[rikishi.id] ?: emptyList())
                tournamentGridView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                gridVisibilityState[rikishi.id] = true
            } else {
                lifecycleScope.launch {
                    checkWrestlerScoreAndHistoryInPreviousTournament(rikishi, gridContainer, tournamentGridView)
                }
            }

            // Show bouts
            if (cachedRikishiBouts.containsKey(rikishi.id)) {
                rikishiBoutsRecyclerView?.visibility = View.VISIBLE
                val rikishiBoutsAdapter = RikishiBoutsAdapter(cachedRikishiBouts[rikishi.id] ?: emptyList(), rikishi.id, isSpoilerModeEnabled)
                rikishiBoutsRecyclerView?.adapter = rikishiBoutsAdapter
                rikishiBoutsRecyclerView?.layoutManager = LinearLayoutManager(context)
                boutsVisibilityState[rikishi.id] = true
            } else {
                lifecycleScope.launch {
                    fetchAndDisplayRikishiBouts(rikishi, rikishiBoutsRecyclerView)
                }
            }
        }
    }

    private suspend fun fetchAndDisplayRikishiBouts(rikishi: RikishiDetails, rikishiBoutsRecyclerView: RecyclerView?) {
        if (rikishiBoutsRecyclerView == null) return

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Fetching bouts for ${rikishi.shikonaEn}...", Toast.LENGTH_SHORT).show()
            rikishiBoutsRecyclerView.visibility = View.GONE
        }

        val (previousTournament, currentTournament, _) = withContext(Dispatchers.IO) {
            scrapeSumoTournaments()
        }

        val targetBashoId: String? = when {
            currentTournament != null && Calendar.getInstance().time.after(currentTournament.firstDay) -> {
                currentTournament.yyyymm
            }
            previousTournament != null -> {
                previousTournament.yyyymm
            }
            else -> {
                null
            }
        }

        if (targetBashoId == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Could not determine relevant tournament for bouts.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val rikishiMatchesResponse: RikishiMatchesListResponse? = try {
            RetrofitInstance.api.getRikishiMatches(rikishi.id)
        } catch (e: Exception) {
            Log.e("SecondFragment", "Error fetching rikishi matches for bouts: ${e.message}", e)
            null
        }

        val allMatches = rikishiMatchesResponse?.matches ?: emptyList()
        Log.d("SecondFragment", "Fetched ${allMatches.size} total matches for bouts for ${rikishi.shikonaEn} in $targetBashoId")

        val isMakuuchiOrJuryo = getDivisionForDisplay(rikishi, false) == "Makuuchi" || getDivisionForDisplay(rikishi, false) == "Juryo"
        val boutsLimit = if (isMakuuchiOrJuryo) 15 else 7

        val filteredBouts = allMatches
            .filter { it.bashoId == targetBashoId }
            .sortedBy { it.day }
            .takeLast(boutsLimit)

        withContext(Dispatchers.Main) {
            if (filteredBouts.isNotEmpty()) {
                cachedRikishiBouts[rikishi.id] = filteredBouts
                val rikishiBoutsAdapter = RikishiBoutsAdapter(filteredBouts, rikishi.id, isSpoilerModeEnabled)
                rikishiBoutsRecyclerView.adapter = rikishiBoutsAdapter
                rikishiBoutsRecyclerView.layoutManager = LinearLayoutManager(context)
                rikishiBoutsRecyclerView.visibility = View.VISIBLE
                boutsVisibilityState[rikishi.id] = true
            } else {
                rikishiBoutsRecyclerView.visibility = View.GONE
                boutsVisibilityState[rikishi.id] = false
                Toast.makeText(context, "No bouts found for ${rikishi.shikonaEn} in $targetBashoId.", Toast.LENGTH_LONG).show()
            }
        }
    }


    private suspend fun checkWrestlerScoreAndHistoryInPreviousTournament(
        rikishi: RikishiDetails,
        gridContainer: LinearLayout?,
        tournamentGridView: RecyclerView?
    ) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Searching for ${rikishi.shikonaEn}'s history...", Toast.LENGTH_SHORT).show()
            gridContainer?.visibility = View.GONE
        }

        val rikishiMatchesResponse: RikishiMatchesListResponse? = try {
            RetrofitInstance.api.getRikishiMatches(rikishi.id)
        } catch (e: Exception) {
            Log.e("SecondFragment", "Network error fetching matches for ${rikishi.shikonaEn}: ${e.message}")
            null
        }

        val allMatches = rikishiMatchesResponse?.matches ?: emptyList()
        Log.d("SecondFragment", "Fetched ${allMatches.size} total matches for ${rikishi.shikonaEn}. Sample: ${allMatches.take(3)}")

        val allRankChanges: List<RankChange> = try {
            RetrofitInstance.api.getRanks(rikishi.id)
        } catch (e: Exception) {
            Log.e("SecondFragment", "Network error fetching ranks for ${rikishi.shikonaEn}: ${e.message}")
            emptyList()
        }

        val filteredRankChanges = allRankChanges
            .distinctBy { it.bashoId }
            .sortedByDescending { it.bashoId }
            .take(6)

        Log.d("SecondFragment", "Parsed rank changes for ${rikishi.shikonaEn}: $filteredRankChanges")

        if (filteredRankChanges.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No historical rank data found for ${rikishi.shikonaEn}.", Toast.LENGTH_SHORT).show()
                gridVisibilityState[rikishi.id] = false
            }
            return
        }

        val tournamentResults = mutableListOf<TournamentResultDisplayData>()

        val actualRikishiDivision = getDivisionForDisplay(rikishi, FavouritesManager.loadSegmentedTopDivisionPreference(requireContext()))
        val totalTournamentDays = if (actualRikishiDivision == "Juryo" || actualRikishiDivision == "Makuuchi") 15 else 7


        for (rankChange in filteredRankChanges) {
            val bashoId = rankChange.bashoId
            val rank = shortenRank(rankChange.rank)
            val date = formatBashoIdToMonthYear(bashoId)

            var wins = 0
            var losses = 0
            var absences = 0

            val matchesInBasho = allMatches.filter { it.bashoId == bashoId }

            if (matchesInBasho.isNotEmpty()) {
                for (match in matchesInBasho) {
                    if (match.winnerId == rikishi.id) {
                        wins++
                    } else {
                        losses++
                    }
                }
                absences = totalTournamentDays - matchesInBasho.size
                if (absences < 0) absences = 0
            } else {
                absences = totalTournamentDays
                wins = 0
                losses = 0
            }

            val score = formatRecord(wins, losses, absences)
            tournamentResults.add(TournamentResultDisplayData(rank, score, date))
        }

        while (tournamentResults.size < 6) {
            tournamentResults.add(TournamentResultDisplayData("-", "-", "-"))
        }

        tournamentResults.reverse()

        withContext(Dispatchers.Main) {
            cachedTournamentHistory[rikishi.id] = tournamentResults

            if (tournamentGridView != null && gridContainer != null) {
                tournamentGridView.adapter = a48626.sumolmbao.second_fragment.TournamentResultAdapter(tournamentResults)
                tournamentGridView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                gridContainer.visibility = View.VISIBLE
                gridVisibilityState[rikishi.id] = true
            }
            Toast.makeText(context, "${rikishi.shikonaEn}'s tournament history loaded.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getDivisionForDisplay(rikishi: RikishiDetails, segmentedTopDivisionEnabled: Boolean): String {
        val isRetired = rikishi.currentRank?.contains("Retired", ignoreCase = true) == true || rikishi.intai == true
        if (isRetired) {
            return "Retired"
        }

        val rankString = rikishi.currentRank?.lowercase() ?: return "Unknown"

        return if (segmentedTopDivisionEnabled) {
            when {
                rankString.startsWith("yokozuna") -> "Yokozuna"
                rankString.startsWith("ozeki") -> "Ozeki"
                rankString.startsWith("sekiwake") -> "Sekiwake"
                rankString.startsWith("komusubi") -> "Komusubi"
                rankString.startsWith("maegashira") -> "Maegashira"
                else -> convertRankToDivision(rikishi.currentRank)
            }
        } else {
            convertRankToDivision(rikishi.currentRank)
        }
    }

    private fun convertRankToDivision(fullRank: String?): String {
        if (fullRank.isNullOrBlank()) return "Unknown"
        val lowerRank = fullRank.lowercase()

        return when {
            lowerRank.startsWith("yokozuna") -> "Makuuchi"
            lowerRank.startsWith("ozeki") -> "Makuuchi"
            lowerRank.startsWith("sekiwake") -> "Makuuchi"
            lowerRank.startsWith("komusubi") -> "Makuuchi"
            lowerRank.startsWith("maegashira") -> "Makuuchi"
            lowerRank.startsWith("juryo") -> "Juryo"
            lowerRank.startsWith("makushita") -> "Makushita"
            lowerRank.startsWith("sandanme") -> "Sandanme"
            lowerRank.startsWith("jonidan") -> "Jonidan"
            lowerRank.startsWith("jonokuchi") -> "Jonokuchi"
            else -> "Unknown"
        }
    }

    private fun getRankSide(rank: String?): String? {
        if (rank.isNullOrBlank()) return null
        return when {
            rank.contains("East", ignoreCase = true) -> "East"
            rank.contains("West", ignoreCase = true) -> "West"
            else -> null
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
        val regex = Regex("""(Yokozuna|Ozeki|Sekiwake|Komusubi|Maegashira|Juryo|Makushita|Sandanme|Jonidan|Jonokuchi|Banzuke-gai|Mae-zumo|Retired)\s*(\d*)\s*(East|West)?""", RegexOption.IGNORE_CASE)
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
                "banzuke-gai" -> "Bg"
                "mae-zumo" -> "Mz"
                "retired" -> "Ret."
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

    private fun parseRankValue(rank: String?): Int {
        if (rank.isNullOrEmpty()) return 10000

        val lowerRank = rank.lowercase()
        return when {
            lowerRank.contains("retired") -> 9000
            lowerRank.contains("yokozuna") -> 100
            lowerRank.contains("ozeki") -> 200
            lowerRank.contains("sekiwake") -> 300
            lowerRank.contains("komusubi") -> 400
            lowerRank.contains("maegashira") -> {
                try {
                    val number = lowerRank.split(" ")[1].toIntOrNull() ?: 0
                    500 + number
                } catch (e: Exception) {
                    599
                }
            }
            lowerRank.contains("juryo") -> {
                try {
                    val number = lowerRank.split(" ")[1].toIntOrNull() ?: 0
                    600 + number
                } catch (e: Exception) {
                    699
                }
            }
            lowerRank.contains("makushita") -> 700
            lowerRank.contains("sandanme") -> 800
            lowerRank.contains("jonidan") -> 900
            lowerRank.contains("jonokuchi") -> 1000
            lowerRank.contains("mae-zumo") -> 1100
            lowerRank.contains("banzuke-gai") -> 1200
            else -> 9999
        }
    }
}