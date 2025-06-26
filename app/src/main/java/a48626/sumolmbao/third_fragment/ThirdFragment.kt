package a48626.sumolmbao.third_fragment

import a48626.sumolmbao.R
import a48626.sumolmbao.SumoDBScraper
import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.RikishiId
import a48626.sumolmbao.data.RikishiStats
import a48626.sumolmbao.database.RikishiDao
import a48626.sumolmbao.database.RikishiDatabase
import a48626.sumolmbao.database.toRikishiDetails
import a48626.sumolmbao.favourites.FavouritesManager
import a48626.sumolmbao.retrofit.RetrofitInstance
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.internal.ViewUtils.hideKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThirdFragment : Fragment() {

    private lateinit var rikishiDao: RikishiDao
    private lateinit var rikishiAdapter: RikishiAdapter
    private lateinit var rikishiRecyclerView: RecyclerView
    private lateinit var searchEditText: EditText

    private var allRikishi = mutableListOf<RikishiDetails>()
    private val filteredRikishi = mutableListOf<RikishiDetails>()

    private var searchJob: Job? = null
    private val searchDebounceTime = 100L // milliseconds

    private var rikishiDetailRecyclerView: RecyclerView? = null
    private var rikishiDetailAdapter: RikishiDetailAdapter? = null
    private var detailContainer: ViewGroup? = null

    private lateinit var favouriteButtonDetail: ImageView

    private var selectedRikishi: RikishiDetails? = null
    private var currentRikishiDetails: RikishiId? = null
    private var currentStats: RikishiStats? = null
    private var currentHighestRank: String? = null

    var rikishiToDisplay: RikishiDetails? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_third, container, false)
        detailContainer = view.findViewById(R.id.detailContainer)
        // The RecyclerView is now part of the XML, so we find it by ID
        rikishiDetailRecyclerView = view.findViewById(R.id.rikishiDetailRecyclerView)
        rikishiDetailRecyclerView?.layoutManager = LinearLayoutManager(context)
        detailContainer?.visibility = View.GONE
        return view
    }

    @SuppressLint("CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ThirdFragment", "onViewCreated called.")

        rikishiDao = RikishiDatabase.getDatabase(requireContext()).rikishiDao()
        searchEditText = view.findViewById(R.id.searchEditText)
        rikishiRecyclerView = view.findViewById(R.id.rikishiRecyclerView)
        favouriteButtonDetail = view.findViewById(R.id.favourite_button_top_bar)

        setupRecyclerView()

        rikishiToDisplay?.let { rikishi ->
            Log.d("ThirdFragment", "New rikishi to display: ${rikishi.shikonaEn}")
            // Use the existing functions to show the details and update the search bar
            showRikishiDetails(rikishi, fetchData = true)
            searchEditText.setText(rikishi.shikonaEn?.replace("#", ""))
            // Clear the variable so it's not used again accidentally
            rikishiToDisplay = null
        }

        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                rikishiRecyclerView.visibility = View.VISIBLE
            }
        }

        view.findViewById<LinearLayout>(R.id.main_content).setOnClickListener {
            clearSearchFocus()
        }

        setupSearchListener()
        loadCachedRikishi()

        // This is the main logic to handle receiving data from the SecondFragment
        arguments?.getSerializable("selectedRikishi")?.let { rikishi ->
            if (rikishi is RikishiDetails) {
                Log.d("ThirdFragment", "Received Rikishi from arguments: ${rikishi.shikonaEn}")
                searchEditText.setText(rikishi.shikonaEn?.replace("#", ""))
                // This is the crucial call to start fetching and displaying data
                showRikishiDetails(rikishi, fetchData = true)
            } else {
                Log.e("ThirdFragment", "Argument 'selectedRikishi' is not of type RikishiDetails.")
            }
        } ?: run {
            Log.d("ThirdFragment", "No 'selectedRikishi' argument found.")
            // This handles restoring the view if the fragment was recreated
            if (selectedRikishi != null && currentRikishiDetails != null) {
                Log.d("ThirdFragment", "Restoring state for a previously selected Rikishi.")
                showRikishiDetails(selectedRikishi!!, false)
            }
        }
    }

    private fun setupRecyclerView() {
        rikishiRecyclerView.layoutManager = LinearLayoutManager(context)
        rikishiAdapter = RikishiAdapter(filteredRikishi, rikishiRecyclerView) { rikishi ->
            showRikishiDetails(rikishi)
            clearSearchFocus()
        }
        rikishiRecyclerView.adapter = rikishiAdapter
        rikishiRecyclerView.visibility = View.GONE
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

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            hideSearchAndKeyboard()
        }
    }

    private fun hideSearchAndKeyboard() {
        rikishiRecyclerView.visibility = View.GONE

        // Hide the keyboard
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)

        searchEditText.clearFocus()
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
                allRikishi = withContext(Dispatchers.IO) {
                    rikishiDao.getAllRikishi().mapNotNull { it.toRikishiDetails() }.toMutableList()
                }
                withContext(Dispatchers.Main) { applyFilter("") }
            } catch (e: Exception) {
                Log.e("ThirdFragment", "Error loading cached rikishi", e)
            }
        }
    }

    private fun applyFilter(query: String) {
        val results = if (query.isEmpty()) {
            allRikishi.take(20)
        } else {
            val lowercaseQuery = query.lowercase()
            allRikishi.filter {
                it.shikonaEn?.lowercase()?.startsWith(lowercaseQuery) == true
            }.sortedBy { it.shikonaEn }.take(20)
        }
        rikishiAdapter.updateData(results)
        if (query.isNotEmpty()) {
            rikishiRecyclerView.scrollToPosition(0)
        }
    }

    private fun showRikishiDetails(rikishi: RikishiDetails, fetchData: Boolean = true) {
        Log.d("ThirdFragment", "showRikishiDetails called for ${rikishi.shikonaEn}. Fetch data: $fetchData")
        selectedRikishi = rikishi
        val container = detailContainer ?: return

        rikishiRecyclerView.visibility = View.GONE
        container.visibility = View.VISIBLE
        favouriteButtonDetail.visibility = View.VISIBLE

        val englishNameTextView: TextView = view?.findViewById(R.id.rikishiEnglishName) ?: return
        val japaneseNameTextView: TextView = view?.findViewById(R.id.rikishiJapaneseName) ?: return
        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)

        englishNameTextView.text = rikishi.shikonaEn?.replace("#", "") ?: "Unknown"
        japaneseNameTextView.text = rikishi.shikonaJp ?: ""

        favouriteButtonDetail.isSelected = FavouritesManager.isFavourite(requireContext(), rikishi.id)
        favouriteButtonDetail.setOnClickListener {
            val isNowFavourite = FavouritesManager.toggleFavourite(requireContext(), rikishi.id)
            it.isSelected = isNowFavourite
        }

        if (!fetchData && currentRikishiDetails != null) {
            Log.d("ThirdFragment", "Restoring details from memory, not fetching.")
            progressBar?.visibility = View.GONE
            rikishiDetailAdapter = RikishiDetailAdapter(currentRikishiDetails!!, currentStats, currentHighestRank)
            rikishiDetailRecyclerView?.adapter = rikishiDetailAdapter
            return
        }

        progressBar?.visibility = View.VISIBLE
        rikishiDetailRecyclerView?.adapter = null

        lifecycleScope.launch {
            try {
                Log.d("ThirdFragment", "Fetching details for ID: ${rikishi.id}")
                val (detailedRikishi, stats, highestRank) = withContext(Dispatchers.IO) {
                    val detailedRikishi = RetrofitInstance.api.getRikishiById(rikishi.id, shikonas = true)
                    Log.d("ThirdFragment", "API getRikishiById successful.")
                    val stats = try {
                        RetrofitInstance.api.getRikishiStats(rikishi.id).also {
                            Log.d("ThirdFragment", "API getRikishiStats successful.")
                        }
                    } catch (e: Exception) {
                        Log.e("ThirdFragment", "API getRikishiStats failed", e)
                        null
                    }
                    val highestRank = try {
                        SumoDBScraper.getHighestRank(rikishi.sumodbId).also {
                            Log.d("ThirdFragment", "Scraping for highest rank successful.")
                        }
                    } catch (e: Exception) {
                        Log.e("ThirdFragment", "Scraping for highest rank failed", e)
                        null
                    }
                    Triple(detailedRikishi, stats, highestRank)
                }

                withContext(Dispatchers.Main) {
                    Log.d("ThirdFragment", "Updating UI with fetched data.")
                    currentRikishiDetails = detailedRikishi
                    currentStats = stats
                    currentHighestRank = highestRank
                    progressBar?.visibility = View.GONE
                    rikishiDetailAdapter = RikishiDetailAdapter(detailedRikishi, stats, highestRank)
                    rikishiDetailRecyclerView?.adapter = rikishiDetailAdapter
                }
            } catch (e: Exception) {
                Log.e("ThirdFragment", "Failed to load details for rikishi ID ${rikishi.id}", e)
                withContext(Dispatchers.Main) {
                    progressBar?.visibility = View.GONE
                    val fallbackRikishi = createFallbackRikishi(rikishi)
                    rikishiDetailAdapter = RikishiDetailAdapter(fallbackRikishi)
                    rikishiDetailRecyclerView?.adapter = rikishiDetailAdapter
                }
            }
        }
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
            selectedRikishi?.let { rikishi ->
                searchEditText.setText(rikishi.shikonaEn?.replace("#", ""))
            }
        }
    }

    private fun createFallbackRikishi(base: RikishiDetails): RikishiId {
        return RikishiId(
            id = base.id, sumodbId = base.sumodbId, nskId = base.nskId,
            shikonaEn = base.shikonaEn ?: "Unknown", shikonaJp = base.shikonaJp ?: "不明",
            currentRank = base.currentRank ?: "Rank unknown", heya = base.heya ?: "Unknown stable",
            birthDate = base.birthDate ?: "", shusshin = base.shusshin ?: "Unknown origin",
            height = base.height ?: 0.0, weight = base.weight ?: 0.0,
            debut = base.debut ?: "", intai = if (base.intai == true) "" else null,
            measurements = null, ranks = null
        )
    }
}