package a48626.sumolmbao.fourth_fragment

import a48626.sumolmbao.R
import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.RikishiMatchesResponse
import a48626.sumolmbao.database.RikishiDatabase
import a48626.sumolmbao.database.RikishiDao
import a48626.sumolmbao.database.toRikishiDetails
import a48626.sumolmbao.retrofit.RetrofitInstance
import a48626.sumolmbao.third_fragment.RikishiAdapter
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
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FourthFragment : Fragment() {

    private lateinit var rikishiDao: RikishiDao
    private lateinit var rikishi1Adapter: RikishiAdapter
    private lateinit var rikishi2Adapter: RikishiAdapter
    private lateinit var matchesAdapter: MatchAdapter

    private lateinit var rikishi1RecyclerView: RecyclerView
    private lateinit var rikishi2RecyclerView: RecyclerView
    private lateinit var matchesRecyclerView: RecyclerView

    private lateinit var searchRikishi1: EditText
    private lateinit var searchRikishi2: EditText

    private lateinit var progressBar: ProgressBar
    private lateinit var summaryContainer: ViewGroup
    private lateinit var kimariteSummaryText: TextView

    private var allRikishi = mutableListOf<RikishiDetails>()
    private val filteredRikishi1 = mutableListOf<RikishiDetails>()
    private val filteredRikishi2 = mutableListOf<RikishiDetails>()

    private var selectedRikishi1: RikishiDetails? = null
    private var selectedRikishi2: RikishiDetails? = null
    private var currentMatchesResponse: RikishiMatchesResponse? = null

    private var searchJob1: Job? = null
    private var searchJob2: Job? = null
    private val searchDebounceTime = 100L

    private var tooltipView: TextView? = null
    private lateinit var overlay_container: View


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fourth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        loadCachedRikishi()
        setupAdapters()
        setupSearchListeners()
        setupOutsideClickHandler(view)

        if (selectedRikishi1 != null && selectedRikishi2 != null && currentMatchesResponse != null) {
            displayMatchesResults(currentMatchesResponse!!)
        }

        searchRikishi1.setOnClickListener {
            overlay_container.visibility = View.GONE

        }
    }

    private fun initViews(view: View) {
        rikishiDao = RikishiDatabase.getDatabase(requireContext()).rikishiDao()

        searchRikishi1 = view.findViewById(R.id.searchRikishi1)
        searchRikishi2 = view.findViewById(R.id.searchRikishi2)
        overlay_container = view.findViewById(R.id.overlay_container)

        rikishi1RecyclerView = view.findViewById(R.id.rikishi1RecyclerView)
        rikishi2RecyclerView = view.findViewById(R.id.rikishi2RecyclerView)
        matchesRecyclerView = view.findViewById(R.id.matchesRecyclerView)

        progressBar = view.findViewById(R.id.progressBar)
        summaryContainer = view.findViewById(R.id.summaryContainer)
        kimariteSummaryText = view.findViewById(R.id.kimariteSummaryText)
    }

    private fun setupAdapters() {
        rikishi1Adapter = RikishiAdapter(filteredRikishi1, rikishi1RecyclerView) { rikishi ->
            selectRikishi(1, rikishi)
            clearSearchFocusAndHideKeyboards()
        }

        rikishi2Adapter = RikishiAdapter(filteredRikishi2, rikishi2RecyclerView) { rikishi ->
            selectRikishi(2, rikishi)
            clearSearchFocusAndHideKeyboards()

        }

        matchesAdapter = MatchAdapter(emptyList()).apply {
            onKimariteClickListener = { kimarite, anchorView ->
                val translation = kimariteTranslations[kimarite] ?: "No translation available"
                showTooltip(anchorView, translation)
                Log.d("KIMARITE", "Showing tooltip for: $kimarite")
            }
        }

        rikishi1RecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = rikishi1Adapter
            visibility = View.GONE
        }

        rikishi2RecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = rikishi2Adapter
            visibility = View.GONE
        }

        matchesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = matchesAdapter
            visibility = View.GONE
        }
    }

    private fun setupOutsideClickHandler(view: View) {
        view.findViewById<FrameLayout>(R.id.overlay_container)?.setOnClickListener {
            clearSearchFocusAndHideKeyboards()
            hideTooltip()
        }
    }

    private fun clearSearchFocusAndHideKeyboards() {
        searchRikishi1.clearFocus()
        searchRikishi2.clearFocus()
        hideSearchResults(1)
        hideSearchResults(2)

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchRikishi1.windowToken, 0)
        imm.hideSoftInputFromWindow(searchRikishi2.windowToken, 0)
    }

    private fun setupSearchListeners() {
        searchRikishi1.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showSearchResults(1)
                hideTooltip()
                applyFilter(1, searchRikishi1.text.toString())
            } else {
                hideSearchResults(1)
            }
        }

        searchRikishi2.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showSearchResults(2)
                hideTooltip()
                applyFilter(2, searchRikishi2.text.toString())
            } else {
                hideSearchResults(2)
            }
        }

        searchRikishi1.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchJob1?.cancel()
                searchJob1 = lifecycleScope.launch {
                    delay(searchDebounceTime)
                    s?.toString()?.let { query ->
                        applyFilter(1, query.trim())
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        searchRikishi2.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchJob2?.cancel()
                searchJob2 = lifecycleScope.launch {
                    delay(searchDebounceTime)
                    s?.toString()?.let { query ->
                        applyFilter(2, query.trim())
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun showSearchResults(searchType: Int) {
        when (searchType) {
            1 -> {
                rikishi1RecyclerView.visibility = View.VISIBLE
                rikishi2RecyclerView.visibility = View.GONE
                view?.findViewById<FrameLayout>(R.id.overlay_container)?.visibility = View.VISIBLE
            }
            2 -> {
                rikishi2RecyclerView.visibility = View.VISIBLE
                rikishi1RecyclerView.visibility = View.GONE
                view?.findViewById<FrameLayout>(R.id.overlay_container)?.visibility = View.VISIBLE
            }
        }
    }

    private fun hideSearchResults(searchType: Int) {
        when (searchType) {
            1 -> rikishi1RecyclerView.visibility = View.GONE
            2 -> rikishi2RecyclerView.visibility = View.GONE
        }

        if (rikishi1RecyclerView.visibility != View.VISIBLE &&
            rikishi2RecyclerView.visibility != View.VISIBLE) {
            view?.findViewById<FrameLayout>(R.id.overlay_container)?.visibility = View.GONE
        }
    }

    private fun loadCachedRikishi() {
        lifecycleScope.launch {
            try {
                allRikishi = withContext(Dispatchers.IO) {
                    rikishiDao.getAllRikishi().map { it.toRikishiDetails() }.toMutableList()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun applyFilter(searchType: Int, query: String) {
        val results = if (query.isEmpty()) {
            allRikishi.take(20)
        } else {
            val lowercaseQuery = query.lowercase()
            allRikishi.filter {
                it.shikonaEn?.lowercase()?.startsWith(lowercaseQuery) == true
            }.take(20)
        }

        when (searchType) {
            1 -> {
                filteredRikishi1.apply {
                    clear()
                    addAll(results)
                }
                rikishi1Adapter.notifyDataSetChanged()
                rikishi1RecyclerView.scrollToPosition(0)
            }
            2 -> {
                filteredRikishi2.apply {
                    clear()
                    addAll(results)
                }
                rikishi2Adapter.notifyDataSetChanged()
                rikishi2RecyclerView.scrollToPosition(0)
            }
        }
    }

    private fun selectRikishi(searchType: Int, rikishi: RikishiDetails)
    {
        when (searchType)
        {
            1 -> {
                selectedRikishi1 = rikishi
                searchRikishi1.setText(rikishi.shikonaEn?.replace("#", ""))
                hideSearchResults(1)
            }
            2 -> {
                selectedRikishi2 = rikishi
                searchRikishi2.setText(rikishi.shikonaEn?.replace("#", ""))
                hideSearchResults(2)
            }
        }

        if (selectedRikishi1 != null && selectedRikishi2 != null && currentMatchesResponse == null) {
            fetchHeadToHeadMatches()
        } else if (selectedRikishi1 != null && selectedRikishi2 != null) {
            currentMatchesResponse?.let { displayMatchesResults(it) }
        }
    }

    private fun fetchHeadToHeadMatches() {
        val rikishi1 = selectedRikishi1 ?: return
        val rikishi2 = selectedRikishi2 ?: return

        progressBar.visibility = View.VISIBLE
        summaryContainer.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val matchesResponse = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getRikishiVersusMatches(rikishi1.id, rikishi2.id)
                }
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    displayMatchesResults(matchesResponse)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    // Handle error
                }
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            selectedRikishi1 = it.getParcelable("selectedRikishi1")
            selectedRikishi2 = it.getParcelable("selectedRikishi2")
            currentMatchesResponse = it.getParcelable("currentMatchesResponse")

            selectedRikishi1?.let { rikishi ->
                searchRikishi1.setText(rikishi.shikonaEn?.replace("#", ""))
            }
            selectedRikishi2?.let { rikishi ->
                searchRikishi2.setText(rikishi.shikonaEn?.replace("#", ""))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("selectedRikishi1", selectedRikishi1 as Parcelable?)
        outState.putParcelable("selectedRikishi2", selectedRikishi2 as Parcelable?)
        outState.putParcelable("currentMatchesResponse", currentMatchesResponse as Parcelable?)
    }

    @SuppressLint("SetTextI18n")
    private fun displayMatchesResults(response: RikishiMatchesResponse) {
        currentMatchesResponse = response

        val rikishi1 = getFirstName(selectedRikishi1?.shikonaEn?.replace("#", ""))
        val rikishi2 = getFirstName(selectedRikishi2?.shikonaEn?.replace("#", ""))

        view?.findViewById<TextView>(R.id.rikishi1Name)?.text = rikishi1
        view?.findViewById<TextView>(R.id.rikishi2Name)?.text = rikishi2
        view?.findViewById<TextView>(R.id.scoreText)?.text = "${response.rikishiWins} - ${response.opponentWins}"
        view?.findViewById<TextView>(R.id.totalMatchesText)?.text = "Total matches: ${response.total}"

        kimariteSummaryText.text = buildKimariteSummary(rikishi1, rikishi2, response)

        matchesAdapter.updateData(response.matches)
        summaryContainer.visibility = View.VISIBLE
        matchesRecyclerView.visibility = View.VISIBLE
    }

    private fun showTooltip(anchorView: View, text: String)
    {
        if (tooltipView == null) {
            tooltipView = TextView(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundResource(R.drawable.tooltip_background)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                elevation = 12f
                textSize = 18f
                visibility = View.GONE
            }
            (view as? ViewGroup)?.addView(tooltipView)
        }

        tooltipView?.apply {
            this.text = text
            measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            val anchorLocation = IntArray(2)
            anchorView.getLocationOnScreen(anchorLocation)

            val x = anchorLocation[0] + (anchorView.width / 2) - (measuredWidth / 2)
            val y = anchorLocation[1] - measuredHeight - 16

            this.x = x.toFloat()
            this.y = y.toFloat()
            visibility = View.VISIBLE
        }

        // Show the overlay to dim the screen
        view?.findViewById<FrameLayout>(R.id.overlay_container)?.visibility = View.VISIBLE
    }

    private fun hideTooltip() {
        tooltipView?.visibility = View.GONE
        view?.findViewById<FrameLayout>(R.id.overlay_container)?.visibility = View.GONE
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun getFirstName(fullName: String?): String {
        return fullName?.split(" ")?.firstOrNull() ?: ""
    }

    private fun buildKimariteSummary(
        rikishi1: String,
        rikishi2: String,
        response: RikishiMatchesResponse
    ): String {
        val topWinTechniques = response.kimariteWins.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString { "${it.key} (${it.value})" }

        val topLossTechniques = response.kimariteLosses.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString { "${it.key} (${it.value})" }

        return """
        ${rikishi1}'s winning techniques: $topWinTechniques
        ${rikishi2}'s winning techniques: $topLossTechniques
    """.trimIndent()
    }

    private val kimariteTranslations = mapOf(
        "Oshidashi" to "Frontal push out",
        "Tsukidashi" to "Frontal thrust out",
        "Yorikiri" to "Frontal force out",
        "Uwatenage" to "Overarm throw",
        "Shitatenage" to "Underarm throw",
        "Hatakikomi" to "Slap down",
        "Hikiotoshi" to "Pull down",
        "Tsukiotoshi" to "Thrust down",
        "Okuridashi" to "Rear push out",
        "Kotenage" to "Armlock throw",
        "Abisetaoshi" to "Backward force down",
        "Amiuchi" to "Fishing net throw",
        "Ashitori" to "Leg grab",
        "Chongake" to "Headlock throw",
        "Fusen" to "Default win",
        "Gasshohineri" to "Double hand twist",
        "Hansoku" to "Disqualification",
        "Harimanage" to "Backward twisting throw",
        "Hikkake" to "Arm grabbing twist down",
        "Ipponzeoi" to "One-armed shoulder throw",
        "Isamiashi" to "Advancing foot",
        "Izori" to "Backward body drop",
        "Kakezori" to "Hooking backward body drop",
        "Kawazugake" to "Frog technique",
        "Kekaeshi" to "Kick over",
        "Ketaguri" to "Tripping ankle",
        "Kimedashi" to "Arm-barring force out",
        "Kimetaoshi" to "Arm-barring force down",
        "Kirikaeshi" to "Twist back",
        "Koshikudake" to "Hip burst",
        "Koshinage" to "Hip throw",
        "Kubihineri" to "Neck twist",
        "Kubinage" to "Neck throw",
        "Makiotoshi" to "Wrap around throw down",
        "Mitokorozeme" to "Triple attack",
        "Nichonage" to "Two-handed throw",
        "Nimaigeri" to "Double kicking backward",
        "Okurigake" to "Rear hooking trip",
        "Okurihikiotoshi" to "Rear pull down",
        "Okurinage" to "Rear throw",
        "Okuritaoshi" to "Rear body drop",
        "Okuritsuridashi" to "Rear lift out",
        "Okuritsuriotoshi" to "Rear lift down",
        "Sabaori" to "Hip roll",
        "Sakatottari" to "Reverse arm lock throw",
        "Shitatedashinage" to "Under-shoulder throw",
        "Shitatenage" to "Underarm throw",
        "Shumokuzori" to "Corner drop",
        "Sokubiotoshi" to "Leg trip down",
        "Sotogake" to "Outer hook",
        "Sotomuso" to "Outer thigh scoop",
        "Sototasukizori" to "Outer reverse lift drop",
        "Susoharai" to "Leg sweep",
        "Susotori" to "Leg grab",
        "Tasukizori" to "Lift drop",
        "Tottari" to "Arm lock throw",
        "Tsukaminage" to "Thrusting shoulder throw",
        "Tsukidashi" to "Frontal thrust out",
        "Tsukihiza" to "Thrusting knee",
        "Tsukiotoshi" to "Thrust down",
        "Tsukitaoshi" to "Thrust down",
        "Tsumatori" to "Ankle pick",
        "Uchigake" to "Inner hook",
        "Uchimuso" to "Inner thigh scoop",
        "Ushiromotare" to "Backward slip down",
        "Utchari" to "Rear push out",
        "Uwatedashinage" to "Over-shoulder throw",
        "Uwatenage" to "Overarm throw",
        "Watashikomi" to "Arm entanglement",
        "Yaguranage" to "Post throw",
        "Yobimodoshi" to "Pull back",
        "Yorikiri" to "Frontal force out",
        "Yoritaoshi" to "Frontal crush out",
        "Zubuneri" to "Head twist"
    )
}