package a48626.sumolmbao.third_fragment

import a48626.sumolmbao.R
import a48626.sumolmbao.SumoDBScraper
import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.RikishiId
import a48626.sumolmbao.data.RikishiStats
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import a48626.sumolmbao.database.RikishiDatabase
import a48626.sumolmbao.database.RikishiDao
import a48626.sumolmbao.database.toRikishiDetails
import a48626.sumolmbao.retrofit.RetrofitInstance
import android.os.Parcelable
import android.util.Log
import android.widget.ProgressBar
import kotlinx.coroutines.*

class ThirdFragment : Fragment() {

    private lateinit var rikishiDao: RikishiDao
    private lateinit var rikishiAdapter: RikishiAdapter
    private lateinit var rikishiRecyclerView: RecyclerView
    private lateinit var searchEditText: EditText

    private var allRikishi = mutableListOf<RikishiDetails>()
    private val filteredRikishi = mutableListOf<RikishiDetails>()

    private var searchJob: Job? = null
    private val searchDebounceTime = 100L // milliseconds

    // Remove lateinit and use nullable with safe access
    private var rikishiDetailRecyclerView: RecyclerView? = null
    private var rikishiDetailAdapter: RikishiDetailAdapter? = null
    private var detailContainer: ViewGroup? = null

    private var selectedRikishi: RikishiDetails? = null
    private var currentRikishiDetails: RikishiId? = null
    private var currentStats: RikishiStats? = null
    private var currentHighestRank: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_third, container, false)

        // Initialize detail container
        detailContainer = view.findViewById(R.id.detailContainer)

        // Initialize and add detail RecyclerView
        rikishiDetailRecyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(context)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        detailContainer?.addView(rikishiDetailRecyclerView)
        detailContainer?.visibility = View.GONE

        return view
    }

    @SuppressLint("CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        rikishiDao = RikishiDatabase.getDatabase(requireContext()).rikishiDao()
        searchEditText = view.findViewById(R.id.searchEditText)
        rikishiRecyclerView = view.findViewById(R.id.rikishiRecyclerView)

        setupRecyclerView()

        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (searchEditText.text.isNotEmpty()) {
                    searchEditText.text.clear()
                }
                rikishiRecyclerView.visibility = View.VISIBLE
            }
        }

        view.findViewById<LinearLayout>(R.id.main_content).setOnClickListener {
            clearSearchFocus()
        }

        detailContainer?.isClickable = false

        setupSearchListener()
        loadCachedRikishi()

        if (selectedRikishi != null && currentRikishiDetails != null) {
            showRikishiDetails(selectedRikishi!!, false) // Pass false to skip API call
        }
    }

    private fun setupRecyclerView() {
        rikishiRecyclerView.layoutManager = LinearLayoutManager(context)

        rikishiAdapter = RikishiAdapter(filteredRikishi, rikishiRecyclerView) { rikishi ->
            showRikishiDetails(rikishi)
        }

        rikishiRecyclerView.adapter = rikishiAdapter
        rikishiRecyclerView.visibility = View.GONE
        rikishiRecyclerView.setHasFixedSize(true)
    }

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    s?.toString()?.let { query ->
                        delay(searchDebounceTime)
                        applyFilter(query.trim())
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun clearSearchFocus() {
        searchEditText.clearFocus()
        rikishiRecyclerView.visibility = View.GONE
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }

    private fun loadCachedRikishi() {
        lifecycleScope.launch {
            try {
                val (count, rikishiEntities) = withContext(Dispatchers.IO) {
                    Pair(rikishiDao.getCount(), rikishiDao.getAllRikishi())
                }

                if (count > 0) {
                    allRikishi = rikishiEntities.mapNotNull {
                        try {
                            it.toRikishiDetails()
                        } catch (e: Exception) {
                            null
                        }
                    }.toMutableList()

                    withContext(Dispatchers.Main) {
                        applyFilter("")
                    }
                }
            } catch (e: Exception) {
                // Log error if needed
            }
        }
    }

    private fun applyFilter(query: String) {
        val results = if (query.isEmpty()) {
            allRikishi.take(20) // Show first 20 when no query
        } else {
            val lowercaseQuery = query.lowercase()
            allRikishi.filter {
                // Only show names that start with the query (case insensitive)
                it.shikonaEn?.lowercase()?.startsWith(lowercaseQuery) == true
            }.sortedBy { it.shikonaEn } // Optional: sort alphabetically
                .take(20) // Limit to 20 results
        }

        rikishiAdapter.updateData(results)

        if (query.isNotEmpty()) {
            rikishiRecyclerView.scrollToPosition(0)
        }
    }

    private fun showRikishiDetails(rikishi: RikishiDetails, fetchData: Boolean = true) {
        selectedRikishi = rikishi
        val recyclerView = rikishiDetailRecyclerView ?: return
        val container = detailContainer ?: return
        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)

        rikishiRecyclerView.visibility = View.GONE
        searchEditText.clearFocus()
        container.visibility = View.VISIBLE

        val englishNameTextView: TextView = view?.findViewById(R.id.rikishiEnglishName) ?: return
        val japaneseNameTextView: TextView = view?.findViewById(R.id.rikishiJapaneseName) ?: return

        englishNameTextView.text = rikishi.shikonaEn?.replace("#", "") ?: "Unknown"
        japaneseNameTextView.text = rikishi.shikonaJp ?: ""

        if (!fetchData && currentRikishiDetails != null) {
            // Use cached data
            progressBar?.visibility = View.GONE
            rikishiDetailAdapter = RikishiDetailAdapter(
                currentRikishiDetails!!,
                currentStats,
                currentHighestRank
            )
            recyclerView.adapter = rikishiDetailAdapter
            return
        }

        progressBar?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Fetch fresh data
                val (detailedRikishi, stats, highestRank) = withContext(Dispatchers.IO) {
                    val detailedRikishi = RetrofitInstance.api.getRikishiById(
                        rikishi.id,
                        shikonas = true
                    )
                    val stats = try {
                        RetrofitInstance.api.getRikishiStats(rikishi.id)
                    } catch (e: Exception) {
                        null
                    }
                    val highestRank = try {
                        SumoDBScraper.getHighestRank(rikishi.sumodbId)
                    } catch (e: Exception) {
                        null
                    }
                    Triple(detailedRikishi, stats, highestRank)
                }

                withContext(Dispatchers.Main) {
                    // Update cached data
                    currentRikishiDetails = detailedRikishi
                    currentStats = stats
                    currentHighestRank = highestRank

                    progressBar?.visibility = View.GONE
                    rikishiDetailAdapter = RikishiDetailAdapter(
                        detailedRikishi,
                        stats,
                        highestRank
                    )
                    recyclerView.adapter = rikishiDetailAdapter
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Failed to load details", e)
                withContext(Dispatchers.Main) {
                    progressBar?.visibility = View.GONE
                    val fallbackRikishi = createFallbackRikishi(rikishi)
                    rikishiDetailAdapter = RikishiDetailAdapter(fallbackRikishi)
                    recyclerView.adapter = rikishiDetailAdapter
                }
            }
        }

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("selectedRikishi", selectedRikishi as Parcelable?)
        outState.putParcelable("currentRikishiDetails", currentRikishiDetails as Parcelable?)
        outState.putParcelable("currentStats", currentStats as Parcelable?)
        outState.putString("currentHighestRank", currentHighestRank)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            selectedRikishi = it.getParcelable("selectedRikishi")
            currentRikishiDetails = it.getParcelable("currentRikishiDetails")
            currentStats = it.getParcelable("currentStats")
            currentHighestRank = it.getString("currentHighestRank")

            // Update search field if we have a selected rikishi
            selectedRikishi?.let { rikishi ->
                searchEditText.setText(rikishi.shikonaEn?.replace("#", ""))
            }
        }
    }

    private fun createLoadingRikishi(base: RikishiDetails): RikishiId {
        return RikishiId(
            id = base.id,
            sumodbId = base.sumodbId,
            nskId = base.nskId,
            shikonaEn = "Loading...",
            shikonaJp = "読み込み中...",
            currentRank = "Loading rank...",
            heya = base.heya ?: "Unknown stable",
            birthDate = "",
            shusshin = "",
            height = 0.0,
            weight = 0.0,
            debut = "",
            intai = null,
            measurements = null,
            ranks = null,
        )
    }

    private fun createFallbackRikishi(base: RikishiDetails): RikishiId {
        return RikishiId(
            id = base.id,
            sumodbId = base.sumodbId,
            nskId = base.nskId,
            shikonaEn = base.shikonaEn ?: "Unknown",
            shikonaJp = base.shikonaJp ?: "不明",
            currentRank = base.currentRank ?: "Rank unknown",
            heya = base.heya ?: "Unknown stable",
            birthDate = base.birthDate ?: "",
            shusshin = base.shusshin ?: "Unknown origin",
            height = base.height ?: 0.0,
            weight = base.weight ?: 0.0,
            debut = base.debut ?: "",
            intai = if (base.intai == true) "" else null,
            measurements = null,
            ranks = null,
        )
    }

    private fun updateRikishiDetails(detailedRikishi: RikishiId) {
        val recyclerView = rikishiDetailRecyclerView ?: return
        val englishNameTextView: TextView = view?.findViewById(R.id.rikishiEnglishName) ?: return
        val japaneseNameTextView: TextView = view?.findViewById(R.id.rikishiJapaneseName) ?: return

        // Update names
        englishNameTextView.text = detailedRikishi.shikonaEn?.replace("#", "") ?: "Unknown"
        japaneseNameTextView.text = detailedRikishi.shikonaJp ?: ""

        // Update adapter with new data
        rikishiDetailAdapter = RikishiDetailAdapter(detailedRikishi)
        recyclerView.adapter = rikishiDetailAdapter
    }
}