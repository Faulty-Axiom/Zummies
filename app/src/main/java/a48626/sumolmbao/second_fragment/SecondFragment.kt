package a48626.sumolmbao

import a48626.sumolmbao.data.Banzuke
import a48626.sumolmbao.data.Rikishi
import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.RankChange
import a48626.sumolmbao.data.TournamentResultDisplayData
import a48626.sumolmbao.data.RikishiMatchesListResponse
import a48626.sumolmbao.data.RikishiMatch
import a48626.sumolmbao.database.RikishiDatabase
import a48626.sumolmbao.database.toRikishiDetails
import a48626.sumolmbao.favourites.FavouritesAdapter
import a48626.sumolmbao.favourites.FavouritesManager
import a48626.sumolmbao.retrofit.RetrofitInstance
import a48626.sumolmbao.second_fragment.DivisionFilterAdapter
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import scrapeSumoTournaments
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

sealed class DisplayItem : Serializable {
    data class RikishiDisplayItem(val rikishiDetails: RikishiDetails) : DisplayItem()
    // --- FIX: Add the missing 'count' parameter to match the implementation ---
    data class DivisionSeparatorItem(val divisionName: String, val count: Int) : DisplayItem()
    override fun equals(other: Any?): Boolean { if (this === other) return true; if (javaClass != other?.javaClass) return false; other as DisplayItem; return when (this) { is RikishiDisplayItem -> other is RikishiDisplayItem && this.rikishiDetails.id == other.rikishiDetails.id; is DivisionSeparatorItem -> other is DivisionSeparatorItem && this.divisionName == other.divisionName } }
    override fun hashCode(): Int { return when (this) { is RikishiDisplayItem -> rikishiDetails.id.hashCode(); is DivisionSeparatorItem -> divisionName.hashCode() } }
}

class SecondFragment : Fragment() {

    // Use the by viewModels() delegate to get a reference to the ViewModel
    private val viewModel: SecondFragmentViewModel by viewModels()

    private lateinit var favouritesRecyclerView: RecyclerView
    private lateinit var favouritesAdapter: FavouritesAdapter
    private var lastTournamentBanzukeMap: Map<Int, Rikishi>? = null

    // This state is now managed by the ViewModel, but we keep a local reference for convenience
    private val divisionExpandedState = mutableMapOf<String, Boolean>()

    private lateinit var showSeparatedDivisionToggle: Switch
    private lateinit var divisionFilterButton: TextView
    private lateinit var divisionFilterRecyclerView: RecyclerView
    private lateinit var divisionFilterAdapter: DivisionFilterAdapter
    private lateinit var overlay: View

    private var isShowSeparatedDivisionEnabled: Boolean = false
    private var selectedDivisionFilter: String = "All"
    private var isSpoilerModeEnabled: Boolean = false

    private var allRikishiMap: Map<Int, RikishiDetails> = emptyMap()
    private lateinit var currentStateSignature: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("ViewModelLifecycle", "Fragment onCreateView")
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        // The 'hidden' parameter is true if the fragment is now hidden, false if it is now visible.
        if (!hidden && isResumed) {
            Log.d("StateCheck", "onHiddenChanged: Fragment is now VISIBLE.")

            val currentStateSignature = generateCurrentStateSignature()
            Log.d("StateCheck", "Current State Signature: $currentStateSignature")
            Log.d("StateCheck", "Last Loaded Signature in ViewModel: ${viewModel.lastLoadedStateSignature}")

            // Reload only if the state has actually changed.
            if (currentStateSignature != viewModel.lastLoadedStateSignature) {
                Log.i("StateCheck", "State has changed! Reloading favourites...")
                lifecycleScope.launch {
                    loadAndDisplayFavourites(currentStateSignature)
                }
            } else {
                Log.i("StateCheck", "State is the same. No reload needed.")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ViewModelLifecycle", "Fragment onViewCreated. ViewModel instance: ${viewModel.hashCode()}")

        favouritesRecyclerView = view.findViewById(R.id.favourites_recycler_view)
        favouritesRecyclerView.layoutManager = LinearLayoutManager(context)
        showSeparatedDivisionToggle = view.findViewById(R.id.showSeparatedDivisionToggle)
        divisionFilterButton = view.findViewById(R.id.divisionFilterButton)
        divisionFilterRecyclerView = view.findViewById(R.id.divisionFilterRecyclerView)
        divisionFilterRecyclerView.layoutManager = LinearLayoutManager(context)
        overlay = view.findViewById(R.id.overlay)

        viewModel.boutsVisibilityState.putAll(FavouritesManager.loadBoutsVisibilityState(requireContext()))
        viewModel.videoVisibilityState.putAll(FavouritesManager.loadVideoVisibilityState(requireContext()))
        Log.d("FragmentState", "Loaded Bouts Visibility State: ${viewModel.boutsVisibilityState}")
        Log.d("FragmentState", "Loaded Video Visibility State: ${viewModel.videoVisibilityState}")

        currentStateSignature = generateCurrentStateSignature()

        lifecycleScope.launch {
            val rikishiDao = RikishiDatabase.getDatabase(requireContext()).rikishiDao()
            allRikishiMap = withContext(Dispatchers.IO) {
                rikishiDao.getAllRikishi().mapNotNull { it.toRikishiDetails() }.associateBy { it.id }
            }
            Log.d("SecondFragment", "Initialized allRikishiMap with ${allRikishiMap.size} entries.")

            initializeAdapter()
            setupFilterControls()

            if (viewModel.lastLoadedStateSignature == null) {
                val initialStateSignature = generateCurrentStateSignature()
                Log.i("StateCheck", "Initial load triggered from onViewCreated.")
                loadAndDisplayFavourites(initialStateSignature)
            }
        }
    }

    private fun initializeAdapter() {
        // --- FIX: All constructor parameters are now correctly passed ---
        favouritesAdapter = FavouritesAdapter(
            favouriteRikishiList = emptyList(),
            allRikishiMap = allRikishiMap, // Use the correct map
            onCheckScoreClick = { rikishi -> handleRikishiItemClick(rikishi) },
            cachedTournamentHistory = viewModel.cachedTournamentHistory,
            cachedRikishiBouts = viewModel.cachedRikishiBouts,
            gridVisibilityState = viewModel.gridVisibilityState,
            boutsVisibilityState = viewModel.boutsVisibilityState,
            videoVisibilityState = viewModel.videoVisibilityState, // Pass the video state
            onDivisionHeaderClick = { divisionName -> toggleDivisionVisibility(divisionName) },
            onRikishiNameClick = { rikishi ->
                (activity as? MainActivity)?.navigateToThirdFragment(rikishi)
            },
            kimariteMap = kimariteTranslations // Pass the translations map
        )
        favouritesRecyclerView.adapter = favouritesAdapter
    }

    private fun setupFilterControls() {
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
        divisionFilterButton.setOnClickListener { toggleDivisionFilterRecyclerView() }
        overlay.setOnClickListener { hideDivisionFilterRecyclerView() }
    }

    override fun onResume() {
        super.onResume()
        Log.d("StateCheck", "onResume triggered.")
        isSpoilerModeEnabled = FavouritesManager.loadSpoilerModePreference(requireContext())

        // --- This is the smart reload logic you designed ---
        val currentStateSignature = generateCurrentStateSignature()
        Log.d("StateCheck", "Current State Signature: $currentStateSignature")
        Log.d("StateCheck", "Last Loaded Signature in ViewModel: ${viewModel.lastLoadedStateSignature}")

        // Reload only if the state has actually changed or has never been loaded.
        if (currentStateSignature != viewModel.lastLoadedStateSignature) {
            Log.i("StateCheck", "State has changed! Reloading favourites...")
            lifecycleScope.launch {
                loadAndDisplayFavourites(currentStateSignature)
            }
        } else {
            Log.i("StateCheck", "State is the same. No reload needed.")
        }
    }

    private fun generateCurrentStateSignature(): String {
        val favouritesIds = FavouritesManager.getFavouriteRikishiIds(requireContext()).sorted().joinToString(",")
        val isSegmented = FavouritesManager.loadSegmentedTopDivisionPreference(requireContext())
        return "Favs:$favouritesIds|Segmented:$isSegmented"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("divisionExpandedState", divisionExpandedState as Serializable)
        outState.putBoolean("isShowSeparatedDivisionEnabled", isShowSeparatedDivisionEnabled)
        outState.putString("selectedDivisionFilter", selectedDivisionFilter)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            (it.getSerializable("divisionExpandedState") as? Map<String, Boolean>)?.let { map -> divisionExpandedState.putAll(map) }
            isShowSeparatedDivisionEnabled = it.getBoolean("isShowSeparatedDivisionEnabled", false)
            selectedDivisionFilter = it.getString("selectedDivisionFilter", "All") ?: "All"
            showSeparatedDivisionToggle.isChecked = isShowSeparatedDivisionEnabled
            divisionFilterButton.text = selectedDivisionFilter
            updateFilterButtonState(isShowSeparatedDivisionEnabled)
        }
    }

    private suspend fun loadAndDisplayFavourites(newStateSignature: String) {
        val rikishiDao = RikishiDatabase.getDatabase(requireContext()).rikishiDao()
        viewModel.sortedFavouriteRikishi = withContext(Dispatchers.IO) {
            rikishiDao.getAllRikishi()
                .mapNotNull { it.toRikishiDetails() }
                .filter { FavouritesManager.getFavouriteRikishiIds(requireContext()).contains(it.id.toString()) }
                .sortedWith(getRikishiComparator())
        }

        // After successfully loading, update the signature in the ViewModel
        viewModel.lastLoadedStateSignature = newStateSignature
        Log.d("StateCheck", "Data reloaded. Updated ViewModel signature to: ${viewModel.lastLoadedStateSignature}")

        lastTournamentBanzukeMap = fetchLastTournamentBanzukeSuspend()
        divisionFilterAdapter.updateData(buildFilterDivisionList(FavouritesManager.loadSegmentedTopDivisionPreference(requireContext())))
        updateFavouritesDisplay()
    }

    private fun updateFavouritesDisplay() {
        if (!::favouritesAdapter.isInitialized) return

        val segmentedTopDivisionEnabled = FavouritesManager.loadSegmentedTopDivisionPreference(requireContext())
        Log.d("SettingsBug", "updateFavouritesDisplay is using 'segmentedTopDivisionEnabled' with value: $segmentedTopDivisionEnabled")

        favouritesAdapter.updateData(
            newFavourites = buildDisplayItems(
                segmentedTopDivisionEnabled,
                isShowSeparatedDivisionEnabled,
                selectedDivisionFilter
            ),
            newAllRikishiMap = allRikishiMap,
            newCachedTournamentHistory = viewModel.cachedTournamentHistory,
            newCachedRikishiBouts = viewModel.cachedRikishiBouts,
            newGridVisibilityState = viewModel.gridVisibilityState,
            newBoutsVisibilityState = viewModel.boutsVisibilityState,
            newVideoVisibilityState = viewModel.videoVisibilityState
        )
    }

    private fun handleRikishiItemClick(rikishi: RikishiDetails) {
        val itemPosition = favouritesAdapter.favouriteRikishiList.indexOfFirst {
            it is DisplayItem.RikishiDisplayItem && it.rikishiDetails.id == rikishi.id
        }
        if (itemPosition == RecyclerView.NO_POSITION) return

        // --- ADDED LOGGING HERE ---
        val currentState = viewModel.videoVisibilityState.getOrDefault(rikishi.id, false)
        Log.d("StateDebug", "handleRikishiItemClick for ${rikishi.shikonaEn}. Current VideoVisible state is: $currentState. Toggling now.")

        viewModel.toggleVideoVisibility(rikishi.id)
        FavouritesManager.saveVideoVisibilityState(requireContext(), viewModel.videoVisibilityState)

        val areDetailsVisible = viewModel.gridVisibilityState.getOrDefault(rikishi.id, false)
        if (areDetailsVisible) {
            viewModel.gridVisibilityState[rikishi.id] = false
            viewModel.boutsVisibilityState[rikishi.id] = false
            favouritesAdapter.notifyItemChanged(itemPosition)
        } else {
            val historyInCache = viewModel.cachedTournamentHistory.containsKey(rikishi.id)
            val boutsInCache = viewModel.cachedRikishiBouts.containsKey(rikishi.id)
            Log.d("SecondFragmentCache", "Click for ${rikishi.shikonaEn}: History in ViewModel? $historyInCache, Bouts in ViewModel? $boutsInCache")

            if (historyInCache && boutsInCache) {
                Log.d("SecondFragmentCache", "-> INSTANT PATH: Data is cached in ViewModel. Showing immediately.")
                viewModel.gridVisibilityState[rikishi.id] = true
                viewModel.boutsVisibilityState[rikishi.id] = true
                favouritesAdapter.notifyItemChanged(itemPosition)
            } else {
                Log.d("SecondFragmentCache", "-> FETCH PATH: Data missing from ViewModel. Starting network calls.")
                lifecycleScope.launch {
                    val deferredTasks = mutableListOf(
                        async(Dispatchers.IO) { if (!historyInCache) checkWrestlerScoreAndHistoryInPreviousTournament(rikishi) },
                        async(Dispatchers.IO) { if (!boutsInCache) fetchAndDisplayRikishiBouts(rikishi) }
                    )
                    deferredTasks.awaitAll()
                    withContext(Dispatchers.Main) {
                        viewModel.gridVisibilityState[rikishi.id] = true
                        viewModel.boutsVisibilityState[rikishi.id] = true
                        favouritesAdapter.notifyItemChanged(itemPosition)
                    }
                }
            }
        }
    }

    private suspend fun fetchAndDisplayRikishiBouts(rikishi: RikishiDetails) {
        val (previousTournament, currentTournament, _) = withContext(Dispatchers.IO) { scrapeSumoTournaments() }
        val targetBashoId = when {
            currentTournament != null && Calendar.getInstance().time.after(currentTournament.firstDay) -> currentTournament.yyyymm
            previousTournament != null -> previousTournament.yyyymm
            else -> null
        }
        if (targetBashoId == null) return
        try {
            val rikishiMatchesResponse = RetrofitInstance.api.getRikishiMatches(rikishi.id)
            val allMatches = rikishiMatchesResponse.matches ?: emptyList()
            val isMakuuchiOrJuryo = getDivisionForDisplay(rikishi, false) in listOf("Makuuchi", "Juryo")
            val boutsLimit = if (isMakuuchiOrJuryo) 15 else 7

            val filteredBouts = allMatches
                .filter { it.bashoId == targetBashoId }
                .sortedByDescending { it.day }
                .take(boutsLimit)

            viewModel.cachedRikishiBouts[rikishi.id] = filteredBouts
        } catch (e: Exception) {
            Log.e("SecondFragmentCache", "Error fetching bouts for ${rikishi.shikonaEn}", e)
        }
    }

    private suspend fun checkWrestlerScoreAndHistoryInPreviousTournament(rikishi: RikishiDetails) {
        try {
            val allMatches = RetrofitInstance.api.getRikishiMatches(rikishi.id).matches ?: emptyList()
            val allRankChanges = RetrofitInstance.api.getRanks(rikishi.id)
            val filteredRankChanges = allRankChanges.distinctBy { it.bashoId }.sortedByDescending { it.bashoId }.take(6)
            if (filteredRankChanges.isEmpty()) return
            val tournamentResults = mutableListOf<TournamentResultDisplayData>()
            val actualRikishiDivision = getDivisionForDisplay(rikishi, FavouritesManager.loadSegmentedTopDivisionPreference(requireContext()))
            val totalTournamentDays = if (actualRikishiDivision in listOf("Juryo", "Makuuchi")) 15 else 7
            for (rankChange in filteredRankChanges) {
                val matchesInBasho = allMatches.filter { it.bashoId == rankChange.bashoId }
                var wins = 0; var losses = 0
                matchesInBasho.forEach { if (it.winnerId == rikishi.id) wins++ else losses++ }
                val absences = if (matchesInBasho.isNotEmpty()) totalTournamentDays - matchesInBasho.size else totalTournamentDays
                tournamentResults.add(TournamentResultDisplayData(shortenRank(rankChange.rank), formatRecord(wins, losses, absences.coerceAtLeast(0)), formatBashoIdToMonthYear(rankChange.bashoId)))
            }
            while (tournamentResults.size < 6) { tournamentResults.add(TournamentResultDisplayData("-", "-", "-")) }
            viewModel.cachedTournamentHistory[rikishi.id] = tournamentResults.reversed()
        } catch (e: Exception) {
            Log.e("SecondFragmentCache", "Error fetching history for ${rikishi.shikonaEn}", e)
        }
    }

    // Replace the existing buildDisplayItems method with this one
    private fun buildDisplayItems(segmentedTopDivisionEnabled: Boolean, showSeparatedDivisionEnabled: Boolean, filterDivision: String): List<DisplayItem> {
        val displayItems = mutableListOf<DisplayItem>()
        val rikishiList = viewModel.sortedFavouriteRikishi
        val filteredRikishi = if (filterDivision == "All") rikishiList else rikishiList.filter { getDivisionForDisplay(it, segmentedTopDivisionEnabled) == filterDivision }

        if (!showSeparatedDivisionEnabled) {
            filteredRikishi.forEach { rikishi -> displayItems.add(DisplayItem.RikishiDisplayItem(rikishi)) }
            return displayItems
        }

        var currentDivisionName: String? = null
        for (rikishi in filteredRikishi) {
            val division = getDivisionForDisplay(rikishi, segmentedTopDivisionEnabled)
            if (filterDivision == "All" && division != currentDivisionName) {
                // FIX: Calculate the count for the new division and pass it to the constructor
                val countForDivision = filteredRikishi.count { getDivisionForDisplay(it, segmentedTopDivisionEnabled) == division }
                displayItems.add(DisplayItem.DivisionSeparatorItem(division, countForDivision))
                currentDivisionName = division
            }
            if (divisionExpandedState.getOrDefault(division, true)) {
                displayItems.add(DisplayItem.RikishiDisplayItem(rikishi))
            }
        }
        return displayItems
    }

    private fun buildFilterDivisionList(segmentedTopDivisionEnabled: Boolean): List<String> {
        val activeDivisions = viewModel.sortedFavouriteRikishi.map { getDivisionForDisplay(it, segmentedTopDivisionEnabled) }.toSet()
        val orderedDivisions = if (segmentedTopDivisionEnabled) listOf("Yokozuna", "Ozeki", "Sekiwake", "Komusubi", "Maegashira", "Juryo", "Makushita", "Sandanme", "Jonidan", "Jonokuchi", "Retired") else listOf("Makuuchi", "Juryo", "Makushita", "Sandanme", "Jonidan", "Jonokuchi", "Retired")
        return listOf("All") + orderedDivisions.filter { it in activeDivisions }
    }

    private fun getRikishiComparator(): Comparator<RikishiDetails> {
        return compareBy<RikishiDetails> { rikishi ->
            val isRetired = rikishi.currentRank?.contains("Retired", ignoreCase = true) == true || rikishi.intai == true
            if (isRetired) 1 else 0
        }.thenBy { parseRankValue(it.currentRank) }
            .thenBy { getRankSide(it.currentRank) }
    }
    private fun toggleDivisionVisibility(divisionName: String) {
        divisionExpandedState[divisionName] = !(divisionExpandedState[divisionName] ?: true)
        updateFavouritesDisplay()
    }
    private fun updateFilterButtonState(isEnabled: Boolean) {
        divisionFilterButton.isClickable = isEnabled
        divisionFilterButton.alpha = if (isEnabled) 1.0f else 0.5f
    }
    private fun toggleDivisionFilterRecyclerView() {
        if (divisionFilterRecyclerView.visibility == View.VISIBLE) hideDivisionFilterRecyclerView() else showDivisionFilterRecyclerView()
    }
    private fun showDivisionFilterRecyclerView() {
        divisionFilterRecyclerView.visibility = View.VISIBLE
        overlay.visibility = View.VISIBLE
        divisionFilterRecyclerView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_down))
    }
    private fun hideDivisionFilterRecyclerView() {
        if (divisionFilterRecyclerView.visibility == View.GONE) return
        divisionFilterRecyclerView.visibility = View.GONE
        overlay.visibility = View.GONE
        divisionFilterRecyclerView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_up))
    }
    private suspend fun fetchLastTournamentBanzukeSuspend(): Map<Int, Rikishi>? {
        return suspendCancellableCoroutine { continuation ->
            val sharedPreferences = requireContext().getSharedPreferences("UserSelections", Context.MODE_PRIVATE)
            val year = sharedPreferences.getInt("lastApiYear", -1).takeIf { it != -1 }
            val month = sharedPreferences.getInt("lastApiMonth", -1).takeIf { it != -1 }
            val division = sharedPreferences.getString("lastApiDivision", null)
            if (year != null && month != null && !division.isNullOrBlank() && division != "Division") {
                RetrofitInstance.api.getBanzuke(String.format("%04d%02d", year, month), division).enqueue(object : Callback<Banzuke> {
                    override fun onResponse(call: Call<Banzuke>, response: Response<Banzuke>) {
                        continuation.resume(if (response.isSuccessful) response.body()?.let { b -> (b.east.orEmpty() + b.west.orEmpty()).associateBy { it.rikishiID } } else null)
                    }
                    override fun onFailure(call: Call<Banzuke>, t: Throwable) { continuation.resume(null) }
                })
            } else continuation.resume(null)
        }
    }
    private fun getDivisionForDisplay(rikishi: RikishiDetails, segmentedTopDivisionEnabled: Boolean): String {
        if (rikishi.currentRank?.contains("Retired", ignoreCase = true) == true || rikishi.intai == true) return "Retired"
        val rank = rikishi.currentRank ?: return "Unknown"
        return if (segmentedTopDivisionEnabled) {
            when {
                rank.startsWith("Yokozuna", true) -> "Yokozuna"
                rank.startsWith("Ozeki", true) -> "Ozeki"
                rank.startsWith("Sekiwake", true) -> "Sekiwake"
                rank.startsWith("Komusubi", true) -> "Komusubi"
                rank.startsWith("Maegashira", true) -> "Maegashira"
                else -> convertRankToDivision(rank)
            }
        } else convertRankToDivision(rank)
    }
    private fun convertRankToDivision(fullRank: String?): String {
        val lowerRank = fullRank?.lowercase() ?: return "Unknown"
        return when {
            lowerRank.startsWith("yokozuna") || lowerRank.startsWith("ozeki") || lowerRank.startsWith("sekiwake") || lowerRank.startsWith("komusubi") || lowerRank.startsWith("maegashira") -> "Makuuchi"
            lowerRank.startsWith("juryo") -> "Juryo"
            lowerRank.startsWith("makushita") -> "Makushita"
            lowerRank.startsWith("sandanme") -> "Sandanme"
            lowerRank.startsWith("jonidan") -> "Jonidan"
            lowerRank.startsWith("jonokuchi") -> "Jonokuchi"
            else -> "Unknown"
        }
    }
    private fun getRankSide(rank: String?): String? {
        return when {
            rank?.contains("East", true) == true -> "East"
            rank?.contains("West", true) == true -> "West"
            else -> null
        }
    }
    private fun formatBashoIdToMonthYear(bashoId: String): String {
        if (bashoId.length != 6) return bashoId
        return try {
            val month = SimpleDateFormat("MMM", Locale.ENGLISH).format(SimpleDateFormat("MM", Locale.ENGLISH).parse(bashoId.substring(4, 6))!!)
            "$month ${bashoId.substring(2, 4)}"
        } catch (e: Exception) { bashoId }
    }
    private fun shortenRank(fullRank: String?): String {
        if (fullRank.isNullOrBlank()) return "-"
        val r = fullRank.lowercase()
        val num = r.filter { it.isDigit() }
        val side = if (r.contains("east")) "e" else if (r.contains("west")) "w" else ""
        return when {
            r.startsWith("yokozuna") -> "Y$side"
            r.startsWith("ozeki") -> "O$side"
            r.startsWith("sekiwake") -> "S$side"
            r.startsWith("komusubi") -> "K$side"
            r.startsWith("maegashira") -> "M$num$side"
            r.startsWith("juryo") -> "J$num$side"
            r.startsWith("makushita") -> "Ms$num$side"
            r.startsWith("sandanme") -> "Sd$num$side"
            r.startsWith("jonidan") -> "Jd$num$side"
            r.startsWith("jonokuchi") -> "Jk$num$side"
            else -> "-"
        }
    }
    private fun formatRecord(wins: Int?, losses: Int?, absences: Int?): String {
        return if (absences != null && absences > 0) "${wins ?: 0}-${losses ?: 0}-${absences}" else "${wins ?: 0}-${losses ?: 0}"
    }
    private fun parseRankValue(rank: String?): Int {
        if (rank.isNullOrEmpty()) return 999
        val r = rank.lowercase()
        val num = r.filter { it.isDigit() }.toIntOrNull() ?: 99
        return when {
            r.contains("yokozuna") -> 1
            r.contains("ozeki") -> 2
            r.contains("sekiwake") -> 3
            r.contains("komusubi") -> 4
            r.contains("maegashira") -> 10 + num
            r.contains("juryo") -> 100 + num
            r.contains("makushita") -> 200 + num
            r.contains("sandanme") -> 300 + num
            r.contains("jonidan") -> 400 + num
            r.contains("jonokuchi") -> 500 + num
            else -> 999
        }
    }

    private val kimariteTranslations = mapOf(
        // --- Basic Techniques ---
        "Abisetaoshi" to "Backward Force Down",
        "Oshidashi" to "Frontal Push Out",
        "Oshitaoshi" to "Frontal Push Down",
        "Tsukidashi" to "Frontal Thrust Out",
        "Tsukitaoshi" to "Frontal Thrust Down",
        "Yorikiri" to "Frontal Force Out",
        "Yoritaoshi" to "Frontal Crush Out",

        // --- Leg Tripping Techniques ---
        "Ashitori" to "Leg Pick",
        "Chongake" to "Pulling Heel Hook",
        "Kawazugake" to "Hooking Backward Counter Throw",
        "Kekaeshi" to "Minor Inner Foot Sweep",
        "Ketaguri" to "Pulling Inside Ankle Sweep",
        "Kirikaeshi" to "Twisting Backward Knee Trip",
        "Komatasukui" to "Over Thigh Scooping Body Drop",
        "Kozumatori" to "Ankle Pick",
        "Mitokorozeme" to "Triple-Attack Force Out",
        "Nimaigeri" to "Ankle Kicking Twist Down",
        "Omata" to "Thigh Scooping Body Drop",
        "Sotogake" to "Outer Leg Trip",
        "Sotokomata" to "Outer Thigh Scooping Body Drop",
        "Susoharai" to "Rear Foot Sweep",
        "Susotori" to "Toe Pick",
        "Tsumatori" to "Rear Ankle Pick",
        "Uchigake" to "Inner Leg Trip",
        "Watashikomi" to "Thigh-Grabbing Push-Down",

        // --- Throwing Techniques ---
        "Ipponzeoi" to "One-Armed Shoulder Throw",
        "Kakenage" to "Hooking Inner Thigh Throw",
        "Koshinage" to "Hip Throw",
        "Kotenage" to "Armlock Throw",
        "Kubinage" to "Headlock Throw",
        "Nichonage" to "Body Drop Throw",
        "Shitatedashinage" to "Pulling Underarm Throw",
        "Shitatenage" to "Underarm Throw",
        "Sukuinage" to "Beltless Arm Throw",
        "Tsukaminage" to "Lifting Throw",
        "Uwatedashinage" to "Pulling Overarm Throw",
        "Uwatenage" to "Overarm Throw",
        "Yaguranage" to "Inner Thigh Throw",

        // --- Twist Down Techniques ---
        "Amiuchi" to "The Fisherman's Throw",
        "Gasshohineri" to "Clasping Head Twist Down",
        "Harimanage" to "Backward Belt Throw",
        "Kainahineri" to "Two-Handed Arm Twist Down",
        "Katasukashi" to "Under-Shoulder Swing Down",
        "Kotehineri" to "Arm Locking Twist Down",
        "Kubihineri" to "Head Twisting Throw",
        "Makiotoshi" to "Twist Down",
        "Osakate" to "Backward Twisting Overarm Throw",
        "Sabaori" to "Forward Crush Down",
        "Sakatottari" to "Arm Bar Throw Counter",
        "Shitatehineri" to "Twisting Underarm Throw",
        "Sotomuso" to "Outer Thigh Propping Twist Down",
        "Tokkurinage" to "Two-Handed Head Twist Down",
        "Tottari" to "Arm Bar Throw",
        "Tsukiotoshi" to "Thrust Down",
        "Uchimuso" to "Inner Thigh Propping Twist Down",
        "Uwatehineri" to "Twisting Overarm Throw",
        "Zubuneri" to "Head Pivot Throw",

        // --- Backwards Body Drop Techniques ---
        "Izori" to "Backward Body Drop",
        "Kakezori" to "Hooking Backwards Body Drop",
        "Shumokuzori" to "Bell-Hammer Backwards Body Drop",
        "Sototasukizori" to "Outer Reverse Backwards Body Drop",
        "Tasukizori" to "Reverse Backwards Body Drop",
        "Tsutaezori" to "Underarm Forward Body Drop",

        // --- Special Techniques ---
        "Hatakikomi" to "Slap Down",
        "Hikiotoshi" to "Hand Pull Down",
        "Hikkake" to "Arm Grabbing Force Out",
        "Kimedashi" to "Arm Barring Force Out",
        "Kimetaoshi" to "Arm Barring Force Down",
        "Okuridashi" to "Rear Push Out",
        "Okurigake" to "Rear Leg Trip",
        "Okurihikiotoshi" to "Rear Pull Down",
        "Okurinage" to "Rear Throw Down",
        "Okuritaoshi" to "Rear Push Down",
        "Okuritsuridashi" to "Rear Lift Out",
        "Okuritsuriotoshi" to "Rear Lifting Body Slam",
        "Sokubiotoshi" to "Head Chop Down",
        "Tsuridashi" to "Frontal Lift Out",
        "Tsuriotoshi" to "Frontal Lifting Body Slam",
        "Ushiromotare" to "Backward Lean Out",
        "Utchari" to "Backward Pivot Throw",
        "Waridashi" to "Upper-Arm Force Out",
        "Yobimodoshi" to "Pulling Body Slam",

        // --- Non-Techniques (Wins/Losses) ---
        "Fumidashi" to "Rear Step Out (Loss)",
        "Fusen" to "Default Win By Absence",
        "Hansoku" to "Foul (Disqualification)",
        "Isamiashi" to "Forward Step Out (Loss)",
        "Koshikudake" to "Inadvertent Collapse (Loss)",
        "Tsukihiza" to "Knee Touch Down (Loss)",
        "Tsukite" to "Hand Touch Down (Loss)"
    )
}