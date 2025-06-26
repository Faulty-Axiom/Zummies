package a48626.sumolmbao.first_fragment

import a48626.sumolmbao.data.Banzuke
import a48626.sumolmbao.data.BanzukeRow
import a48626.sumolmbao.data.Basho
import a48626.sumolmbao.R
import a48626.sumolmbao.VideoPlayerActivity
import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.Torikumi
import a48626.sumolmbao.data.TorikumiResponse
import a48626.sumolmbao.database.RikishiDatabase
import a48626.sumolmbao.database.toRikishiDetails
import a48626.sumolmbao.retrofit.RetrofitInstance
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.core.view.isGone
import java.util.Calendar
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.collections.get
import kotlin.math.abs

class FirstFragment : Fragment() {

    // --- NEW: Map to hold all rikishi data for quick lookup ---
    private var allRikishiMap: Map<Int, RikishiDetails> = emptyMap()

    private lateinit var bashoWinnersList: TextView
    private lateinit var yearRecyclerView: RecyclerView
    private lateinit var monthRecyclerView: RecyclerView
    private lateinit var divisionRecyclerView: RecyclerView
    private lateinit var banzukeRecyclerView: RecyclerView
    private lateinit var banzukeAdapter: BanzukeAdapter
    private lateinit var yearButton: TextView
    private lateinit var monthButton: TextView
    private lateinit var divisionButton: TextView
    private var selectedYear: Int? = null
    private var selectedMonth: Int? = null
    private var selectedDivision: String? = null
    private var selectedDay: Int? = null
    private var monthNames = mapOf(
        1 to "January", 3 to "March", 5 to "May", 7 to "July",
        9 to "September", 11 to "November"
    )
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var numberRecyclerView: RecyclerView
    private lateinit var overlay: View
    private lateinit var torikumiRecyclerView: RecyclerView
    private lateinit var bashoWinnersContainer: LinearLayout
    private lateinit var bashoSpecialPrizesList: TextView
    private lateinit var torikumiContainer: LinearLayout
    private lateinit var banzukeContainer: LinearLayout

    private var originalTexts = mutableMapOf<TextView, CharSequence>()

    // For press-and-hold with delay and scroll lock
    private val handler = Handler(Looper.getMainLooper())
    private var translationRunnable: Runnable? = null
    private var isTranslationLocked = false
    private var touchSlop: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    @SuppressLint("SetTextI18n", "CutPasteId", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- NEW: Load all rikishi into the map on creation ---
        lifecycleScope.launch {
            val rikishiDao = RikishiDatabase.getDatabase(requireContext()).rikishiDao()
            allRikishiMap = rikishiDao.getAllRikishi().mapNotNull { it.toRikishiDetails() }.associateBy { it.id }
            Log.d("FirstFragment", "Initialized allRikishiMap with ${allRikishiMap.size} entries.")
        }

        sharedPreferences = requireContext().getSharedPreferences("UserSelections", Context.MODE_PRIVATE)
        touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop

        yearButton = view.findViewById(R.id.yearButton)
        monthButton = view.findViewById(R.id.monthButton)
        divisionButton = view.findViewById(R.id.divisionButton)
        yearRecyclerView = view.findViewById(R.id.yearRecyclerView)
        monthRecyclerView = view.findViewById(R.id.monthRecyclerView)
        bashoWinnersList = view.findViewById(R.id.bashoWinnersList)
        numberRecyclerView = view.findViewById(R.id.numberRecyclerView)
        divisionRecyclerView = view.findViewById(R.id.divisionRecyclerView)
        banzukeRecyclerView = view.findViewById(R.id.banzukeRecyclerView)
        banzukeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        banzukeAdapter = BanzukeAdapter(emptyList())
        banzukeRecyclerView.adapter = banzukeAdapter
        overlay = view.findViewById(R.id.overlay)
        torikumiRecyclerView = view.findViewById(R.id.torikumiRecyclerView)
        torikumiRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        bashoWinnersContainer = view.findViewById(R.id.bashoWinnersContainer)
        bashoSpecialPrizesList = view.findViewById(R.id.bashoSpecialPrizesList)
        torikumiContainer = view.findViewById(R.id.torikumiContainer)
        banzukeContainer = view.findViewById(R.id.banzukeContainer)

        // --- START OF NEW TOUCH LOGIC ---

        torikumiRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            private var downX = 0f
            private var downY = 0f

            // This runnable will trigger the translation and lock
            private val translationRunnable = Runnable {
                isTranslationLocked = true
                translateAllKimarite(true)
            }

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (isTranslationLocked) { return true }
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = e.x
                        downY = e.y
                        handler.postDelayed(translationRunnable, 300)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (abs(e.x - downX) > touchSlop || abs(e.y - downY) > touchSlop) {
                            handler.removeCallbacks(translationRunnable)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(translationRunnable)
                    }
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL) {
                    translateAllKimarite(false)
                    isTranslationLocked = false
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                if (disallowIntercept) {
                    handler.removeCallbacks(translationRunnable)
                }
            }
        })

        // (The rest of your onViewCreated code remains the same...)
        overlay.setOnClickListener {
            hideRecyclerViewWithAnimation(yearRecyclerView)
            hideRecyclerViewWithAnimation(monthRecyclerView)
            hideRecyclerViewWithAnimation(divisionRecyclerView)
            overlay.visibility = View.GONE
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        selectedYear = sharedPreferences.getInt("selectedYear", -1).takeIf { it != -1 }
        selectedMonth = sharedPreferences.getInt("selectedMonth", -1).takeIf { it != -1 }
        selectedDivision = sharedPreferences.getString("selectedDivision", null)
        selectedDay = sharedPreferences.getInt("selectedDay", -1).takeIf { it != -1 }
        yearButton.text = selectedYear?.toString() ?: "Year"
        monthButton.text = selectedMonth?.let { monthNames[it] } ?: "Month"
        divisionButton.text = selectedDivision ?: "Division"
        if (selectedYear == null || selectedMonth == null) {
            bashoWinnersContainer.visibility = View.GONE
        }
        if (selectedYear != null) {
            yearButton.text = "$selectedYear"
        }
        if (selectedMonth != null) {
            monthButton.text = "${monthNames[selectedMonth]}"
        }
        if (selectedDivision != null) {
            divisionButton.text = "$selectedDivision"
        }
        if (selectedDay != null) {
            (numberRecyclerView.adapter as? NumberAdapter)?.setSelectedDay(selectedDay)
        }

        numberRecyclerView.visibility = if (selectedDivision != null && selectedDivision != "Division") {
            View.VISIBLE
        } else {
            View.GONE
        }

        view.post {
            if (selectedYear != null && selectedMonth != null)
            {
                bashoWinnersList.text = "Loading winners..."
                bashoSpecialPrizesList.text = "Loading Special Prizes..."
                fetchSpecificCall()
                saveLastApiCallParams()
            }
        }

        val years = listOf("Year") + (1958..currentYear).toList().reversed()
        val yearAdapter = YearMonthAdapter(
            items = years.map { it.toString() },
            onItemClick = { year ->
                if (year == "Year") {
                    selectedYear = null
                    yearButton.text = "Year"
                    sharedPreferences.edit { remove("selectedYear") }
                } else {
                    selectedYear = year.toInt()
                    yearButton.text = year
                    sharedPreferences.edit { putInt("selectedYear", selectedYear!!) }
                }
                clearDependentSelections()
                hideRecyclerViewWithAnimation(yearRecyclerView)
                fetchSpecificCall()
                saveLastApiCallParams()
            },
            onDismiss = { /* keep existing */ }
        )
        yearRecyclerView.adapter = yearAdapter
        yearRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        val months = listOf("Month") + listOf(1, 3, 5, 7, 9, 11)
        val monthAdapter = YearMonthAdapter(
            items = months.map { if (it is Int) monthNames[it] ?: "" else it.toString() },
            onItemClick = { monthName ->
                if (monthName == "Month") {
                    selectedMonth = null
                    monthButton.text = "Month"
                    sharedPreferences.edit { remove("selectedMonth") }
                } else {
                    selectedMonth = monthNames.entries.find { it.value == monthName }?.key ?: 1
                    monthButton.text = monthName
                    sharedPreferences.edit { putInt("selectedMonth", selectedMonth!!) }
                }
                clearDependentSelections()
                fetchSpecificCall()
                saveLastApiCallParams()
            },
            onDismiss = {
                hideRecyclerViewWithAnimation(monthRecyclerView)
                overlay.visibility = View.GONE
            }
        )
        monthRecyclerView.adapter = monthAdapter
        monthRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        val divisions = listOf("Division", "Makuuchi", "Juryo", "Makushita", "Sandanme", "Jonidan", "Jonokuchi")
        val divisionAdapter = DivisionAdapter(
            divisions = divisions,
            onDivisionSelected = { division ->
                selectedDivision = division
                if (division != "Division") {
                    divisionButton.text = division
                } else {
                    divisionButton.text = "Division"
                }
                sharedPreferences.edit { putString("selectedDivision", division) }
                if (division == "Division") {
                    clearDaySelection()
                }
                fetchSpecificCall()
                saveLastApiCallParams()
            },
            onDismiss = {
                hideRecyclerViewWithAnimation(divisionRecyclerView)
                overlay.visibility = View.GONE
            }
        )
        divisionRecyclerView.adapter = divisionAdapter
        divisionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        val numbers = (1..15).toList()
        val numberPages = numbers.chunked(5)

        sharedPreferences = requireContext().getSharedPreferences("UserSelections", Context.MODE_PRIVATE)
        selectedDay = sharedPreferences.getInt("selectedDay", -1).takeIf { it != -1 }

        val numberAdapter = NumberAdapter(numberPages).apply {
            onNumberSelected = { day ->
                onDaySelected(day)
            }
            onPageChange = { newPosition ->
                numberRecyclerView.smoothScrollToPosition(newPosition)
            }
            setSelectedDay(selectedDay)
        }
        numberRecyclerView.adapter = numberAdapter
        numberRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(numberRecyclerView)
        numberRecyclerView.post {
            val layoutManager = numberRecyclerView.layoutManager as LinearLayoutManager
            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            updateNumberArrowsVisibility(firstVisiblePosition)
        }

        numberRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // This is the core logic: when scrolling stops, update the arrow visibility.
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateArrowVisibility()
                } else {
                    // While scrolling, temporarily hide the arrows on all visible pages.
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    val lastVisible = layoutManager.findLastVisibleItemPosition()
                    for (i in firstVisible..lastVisible) {
                        (recyclerView.findViewHolderForAdapterPosition(i) as? NumberAdapter.NumberViewHolder)?.let {
                            it.leftArrow.visibility = View.GONE
                            it.rightArrow.visibility = View.GONE
                        }
                    }
                }
            }
        })

        yearButton.setOnClickListener {
            animateButtonClick(it)
            if (divisionRecyclerView.isVisible) {
                hideRecyclerViewWithAnimation(divisionRecyclerView)
            }

            if (yearRecyclerView.isGone) {
                if (monthRecyclerView.isVisible) {
                    hideRecyclerViewWithAnimation(monthRecyclerView) {
                        overlay.visibility = View.VISIBLE
                        showRecyclerViewWithAnimation(yearRecyclerView)
                    }
                } else {
                    overlay.visibility = View.VISIBLE
                    showRecyclerViewWithAnimation(yearRecyclerView)
                }
            } else {
                hideRecyclerViewWithAnimation(yearRecyclerView)
                overlay.visibility = View.GONE
            }
        }
        monthButton.setOnClickListener {
            animateButtonClick(it)
            if (divisionRecyclerView.isVisible) {
                hideRecyclerViewWithAnimation(divisionRecyclerView)
            }

            if (monthRecyclerView.isGone) {
                if (yearRecyclerView.isVisible) {
                    hideRecyclerViewWithAnimation(yearRecyclerView) {
                        overlay.visibility = View.VISIBLE
                        showRecyclerViewWithAnimation(monthRecyclerView)
                    }
                } else {
                    overlay.visibility = View.VISIBLE
                    showRecyclerViewWithAnimation(monthRecyclerView)
                }
            } else {
                hideRecyclerViewWithAnimation(monthRecyclerView)
                overlay.visibility = View.GONE
            }
        }
        divisionButton.setOnClickListener {
            animateButtonClick(it)

            if (divisionRecyclerView.isGone) {
                if (yearRecyclerView.isVisible) {
                    hideRecyclerViewWithAnimation(yearRecyclerView) {
                        overlay.visibility = View.VISIBLE
                        showRecyclerViewWithAnimation(divisionRecyclerView)
                    }
                } else if (monthRecyclerView.isVisible) {
                    hideRecyclerViewWithAnimation(monthRecyclerView) {
                        overlay.visibility = View.VISIBLE
                        showRecyclerViewWithAnimation(divisionRecyclerView)
                    }
                } else {
                    overlay.visibility = View.VISIBLE
                    showRecyclerViewWithAnimation(divisionRecyclerView)
                }
            } else {
                hideRecyclerViewWithAnimation(divisionRecyclerView)
                overlay.visibility = View.GONE
            }
        }
        if (selectedYear != null && selectedMonth != null) {
            fetchSpecificCall()
            saveLastApiCallParams()
        }
    }

    private fun translateAllKimarite(translate: Boolean) {
        val torikumiAdapter = torikumiRecyclerView.adapter as? TorikumiAdapter ?: return

        if (translate) {
            for (i in 0 until torikumiAdapter.itemCount) {
                val viewHolder = torikumiRecyclerView.findViewHolderForAdapterPosition(i) as? TorikumiAdapter.TorikumiViewHolder
                if (viewHolder != null) {
                    val kimarite = torikumiAdapter.getKimariteAt(i)
                    if (!originalTexts.containsKey(viewHolder.technique)) {
                        originalTexts[viewHolder.technique] = viewHolder.technique.text
                    }
                    viewHolder.technique.text = kimariteTranslations[kimarite] ?: kimarite
                }
            }
        } else {
            if (originalTexts.isNotEmpty()) {
                for (i in 0 until torikumiAdapter.itemCount) {
                    val viewHolder = torikumiRecyclerView.findViewHolderForAdapterPosition(i) as? TorikumiAdapter.TorikumiViewHolder
                    if (viewHolder != null && originalTexts.containsKey(viewHolder.technique)) {
                        viewHolder.technique.text = originalTexts[viewHolder.technique]
                    }
                }
                originalTexts.clear()
            }
        }
    }

    private fun fetchTorikumi(bashoId: String, division: String, day: Int) {
        RetrofitInstance.api.getTorikumi(bashoId, division, day).enqueue(object : Callback<TorikumiResponse> {
            override fun onResponse(call: Call<TorikumiResponse>, response: Response<TorikumiResponse>) {
                if (response.isSuccessful) {
                    val torikumiList = response.body()?.torikumi ?: emptyList()
                    // --- NEW: Create and set the adapter with the video click listener ---
                    val adapter = TorikumiAdapter(torikumiList, allRikishiMap) { torikumi, eastSumoDbId, westSumoDbId ->
                        // This lambda is the click handler
                        val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
                            putExtra(VideoPlayerActivity.EXTRA_BASHO_ID, torikumi.bashoId)
                            putExtra(VideoPlayerActivity.EXTRA_DAY, torikumi.day)
                            putExtra(VideoPlayerActivity.EXTRA_RIKISHI1_ID, eastSumoDbId)
                            putExtra(VideoPlayerActivity.EXTRA_RIKISHI2_ID, westSumoDbId)
                        }
                        startActivity(intent)
                    }

                    torikumiRecyclerView.adapter = adapter
                    torikumiContainer.visibility = View.VISIBLE
                    torikumiRecyclerView.visibility = View.VISIBLE
                    banzukeRecyclerView.visibility = View.GONE
                    bashoWinnersContainer.visibility = View.GONE
                    updateArrowVisibility()

                } else {
                    Log.e("TorikumiFetch", "Error ${response.code()}: ${response.errorBody()?.string()}")
                    torikumiRecyclerView.visibility = View.GONE
                    banzukeRecyclerView.visibility = View.GONE
                    bashoWinnersContainer.visibility = View.GONE
                }
            }
            override fun onFailure(call: Call<TorikumiResponse>, t: Throwable) {
                Log.e("TorikumiFetch", "Network failure: ${t.message}", t)
                torikumiContainer.visibility = View.GONE
                torikumiRecyclerView.visibility = View.GONE
                banzukeRecyclerView.visibility = View.GONE
                bashoWinnersContainer.visibility = View.GONE
            }
        })
    }

    private fun updateArrowVisibility() {
        val layoutManager = numberRecyclerView.layoutManager as LinearLayoutManager
        val snapHelper = PagerSnapHelper() // It's safe to create a new one
        val snappedView = snapHelper.findSnapView(layoutManager) ?: return
        val snappedPosition = layoutManager.getPosition(snappedView)

        (numberRecyclerView.findViewHolderForAdapterPosition(snappedPosition) as? NumberAdapter.NumberViewHolder)?.let {
            it.leftArrow.visibility = if (snappedPosition > 0) View.VISIBLE else View.GONE
            it.rightArrow.visibility = if (snappedPosition < (numberRecyclerView.adapter?.itemCount ?: 0) - 1) View.VISIBLE else View.GONE
        }
    }


    override fun onPause() {
        super.onPause()
        listOf(yearButton, monthButton, divisionButton, banzukeRecyclerView,
            bashoWinnersContainer, numberRecyclerView, torikumiRecyclerView).forEach {
            it.animate().cancel()
        }
        translationRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun updateNumberArrowsVisibility(position: Int) {
        val adapter = numberRecyclerView.adapter as? NumberAdapter
        adapter?.getViewHolderAt(position)?.let { viewHolder ->
            viewHolder.leftArrow.visibility = if (position > 0) View.VISIBLE else View.GONE
            viewHolder.rightArrow.visibility = if (position < adapter.itemCount - 1) View.VISIBLE else View.GONE
        }
    }

    private fun isSameAsLastApiCall(): Boolean {
        val lastYear = sharedPreferences.getInt("lastApiYear", -1)
        val lastMonth = sharedPreferences.getInt("lastApiMonth", -1)
        val lastDivision = sharedPreferences.getString("lastApiDivision", null)
        val lastDay = sharedPreferences.getInt("lastApiDay", -1)

        return selectedYear == lastYear &&
                selectedMonth == lastMonth &&
                selectedDivision == lastDivision &&
                (selectedDay ?: -1) == lastDay
    }

    private fun saveLastApiCallParams() {
        sharedPreferences.edit {
            putInt("lastApiYear", selectedYear ?: -1)
            putInt("lastApiMonth", selectedMonth ?: -1)
            putString("lastApiDivision", selectedDivision)
            putInt("lastApiDay", selectedDay ?: -1)
        }
    }

    private fun saveLastApiCallUrl() {
        val apiUrl = buildLastApiUrl()
        sharedPreferences.edit {
            putString("lastApiUrl", apiUrl)
        }
    }

    private fun buildLastApiUrl(): String {
        val year = selectedYear ?: return ""
        val month = selectedMonth?.toString()?.padStart(2, '0') ?: return ""
        val division = selectedDivision?.lowercase() ?: return ""

        var url = "https://www.sumo-api.com/basho/${year}${month}/banzuke/${division}"
        if (selectedDay != null) {
            url += "/day/${selectedDay}"
        }
        return url
    }

    @SuppressLint("SetTextI18n")
    private fun clearDependentSelections() {
        if (selectedYear == null || selectedMonth == null) {
            selectedDivision = null
            divisionButton.text = "Division"
            clearDaySelection()
            sharedPreferences.edit {
                remove("selectedDivision")
                remove("selectedDay")
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun fetchSpecificCall() {
        bashoWinnersContainer.visibility = View.GONE
        banzukeRecyclerView.visibility = View.GONE
        banzukeContainer.visibility = View.GONE
        torikumiRecyclerView.visibility = View.GONE
        torikumiContainer.visibility = View.GONE
        numberRecyclerView.visibility = View.GONE
        bashoWinnersList.visibility = View.GONE
        bashoSpecialPrizesList.visibility = View.GONE

        if (selectedYear == null || selectedMonth == null) {
            return
        }

        val bashoId = String.format("%04d%02d", selectedYear, selectedMonth)

        saveLastApiCallParams()
        saveLastApiCallUrl()

        if (isSameAsLastApiCall()) {
            Log.d("API_CALL", "Using same parameters as last API call")
        } else {
            Log.d("API_CALL", "Making a new API call")
        }

        numberRecyclerView.visibility = if (selectedDivision != null && selectedDivision != "Division") {
            View.VISIBLE
        } else {
            View.GONE
        }

        when {
            selectedDivision == null || selectedDivision == "Division" -> {
                bashoWinnersContainer.visibility = View.VISIBLE
                fetchBasho(bashoId)
            }
            selectedDay != null -> {
                torikumiContainer.visibility = View.VISIBLE
                torikumiRecyclerView.visibility = View.VISIBLE
                fetchTorikumi(bashoId, selectedDivision!!, selectedDay!!)
            }
            else -> {
                banzukeContainer.visibility = View.GONE
                banzukeRecyclerView.visibility = View.VISIBLE
                fetchBanzuke(bashoId, selectedDivision!!)
            }
        }
    }

    private fun fetchBasho(bashoId: String)
    {
        bashoWinnersContainer.visibility = View.VISIBLE
        banzukeRecyclerView.visibility = View.GONE
        torikumiRecyclerView.visibility = View.GONE

        bashoWinnersList.visibility = View.VISIBLE
        bashoSpecialPrizesList.visibility = View.VISIBLE

        RetrofitInstance.api.getBasho(bashoId).enqueue(object : Callback<Basho>
        {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call<Basho>, response: Response<Basho>)
            {
                if (response.isSuccessful)
                {
                    val basho = response.body()
                    val yushoList = basho?.yusho ?: emptyList()
                    val orderedDivisions = listOf(
                        "Makuuchi", "Juryo", "Makushita",
                        "Sandanme", "Jonidan", "Jonokuchi"
                    )

                    val winnersText = orderedDivisions.joinToString("\n") { division ->
                        val winner = yushoList.firstOrNull { it.type == division }
                        if (winner != null)
                        {
                            "$division: ${winner.shikonaEn}"
                        } else
                        {
                            "$division: Not determined yet"
                        }
                    }

                    bashoWinnersList.text = winnersText
                    val specialPrizes = basho?.specialPrizes ?: emptyList()
                    val prizesText = if (specialPrizes.isNotEmpty())
                    {
                        specialPrizes.joinToString("\n") { prize ->
                            "${prize.type}: ${prize.shikonaEn}"
                        }
                    } else
                    {
                        "No special prizes awarded"
                    }

                    bashoSpecialPrizesList.text = prizesText
                } else
                {
                    bashoWinnersList.text = "Error loading winners"
                    bashoSpecialPrizesList.text = "Error loading special prizes"
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onFailure(call: Call<Basho>, t: Throwable)
            {
                bashoWinnersList.text = "Network error: ${t.message}"
                bashoSpecialPrizesList.text = "Network error"
            }
        })
    }

    private fun fetchBanzuke(bashoId: String, division: String)
    {
        RetrofitInstance.api.getBanzuke(bashoId, division).enqueue(object : Callback<Banzuke>
        {
            override fun onResponse(call: Call<Banzuke>, response: Response<Banzuke>)
            {
                if (response.isSuccessful)
                {
                    val banzuke = response.body()
                    val east = banzuke?.east ?: emptyList()
                    val west = banzuke?.west ?: emptyList()

                    val pairedList = mutableListOf<BanzukeRow>()

                    val sanyakuRanks = listOf("Yokozuna", "Ozeki", "Sekiwake", "Komusubi")
                    sanyakuRanks.forEach { rankType ->
                        val rankWrestlers = (east + west)
                            .filter { it.rank.startsWith(rankType) }
                            .sortedWith(compareBy(
                                { wrestler ->
                                    val parts = wrestler.rank.split(" ")
                                    parts.getOrNull(1)?.toIntOrNull() ?: 1
                                },
                                { it.side }
                            ))
                        val grouped = rankWrestlers.groupBy { wrestler ->
                            val parts = wrestler.rank.split(" ")
                            parts.getOrNull(1)?.toIntOrNull() ?: 1
                        }

                        grouped.forEach { (positionNumber, wrestlers) ->
                            val eastWrestler = wrestlers.find { it.side == "East" }
                            val westWrestler = wrestlers.find { it.side == "West" }

                            when {
                                eastWrestler != null && westWrestler != null -> {
                                    pairedList.add(
                                        BanzukeRow(
                                            east = eastWrestler,
                                            west = westWrestler,
                                            eastRank = eastWrestler.rank,
                                            westRank = westWrestler.rank,
                                            isSanyakuRow = true
                                        )
                                    )
                                }
                                eastWrestler != null -> {
                                    pairedList.add(
                                        BanzukeRow(
                                            east = eastWrestler,
                                            west = null,
                                            eastRank = eastWrestler.rank,
                                            westRank = null,
                                            isSanyakuRow = true
                                        )
                                    )
                                }
                                westWrestler != null -> {
                                    pairedList.add(
                                        BanzukeRow(
                                            east = null,
                                            west = westWrestler,
                                            eastRank = null,
                                            westRank = westWrestler.rank,
                                            isSanyakuRow = true
                                        )
                                    )
                                }
                            }
                        }
                    }
                    val eastNonSanyaku = east.filterNot { it.rank.split(" ")[0] in sanyakuRanks }
                    val westNonSanyaku = west.filterNot { it.rank.split(" ")[0] in sanyakuRanks }

                    val maxNonSanyakuSize = maxOf(eastNonSanyaku.size, westNonSanyaku.size)
                    for (i in 0 until maxNonSanyakuSize)
                    {
                        pairedList.add(
                            BanzukeRow(
                                east = eastNonSanyaku.getOrNull(i),
                                west = westNonSanyaku.getOrNull(i),
                                eastRank = eastNonSanyaku.getOrNull(i)?.rank,
                                westRank = westNonSanyaku.getOrNull(i)?.rank,
                                isSanyakuRow = false
                            )
                        )
                    }

                    banzukeRecyclerView.adapter = BanzukeAdapter(pairedList)
                    banzukeRecyclerView.layoutManager = LinearLayoutManager(requireContext())

                    banzukeContainer.visibility = View.VISIBLE
                    banzukeRecyclerView.visibility = View.VISIBLE
                    torikumiRecyclerView.visibility = View.GONE
                    bashoWinnersContainer.visibility = View.GONE
                }
            }
            override fun onFailure(call: Call<Banzuke>, t: Throwable)
            {
                Log.e("BanzukeFetch", "Network failure: ${t.message}", t)
                banzukeContainer.visibility = View.GONE
                banzukeRecyclerView.visibility = View.GONE
                torikumiRecyclerView.visibility = View.GONE
                bashoWinnersContainer.visibility = View.GONE
            }
        })
    }

    private fun animateButtonClick(view: View)
    {
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_up)
        view.startAnimation(animation)
    }

    private fun showRecyclerViewWithAnimation(view: RecyclerView)
    {
        overlay.visibility = View.VISIBLE
        view.visibility = View.INVISIBLE
        view.post {
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down)
            view.startAnimation(animation)
            view.visibility = View.VISIBLE
        }
    }

    private fun hideRecyclerViewWithAnimation(view: RecyclerView, callback: () -> Unit = {})
    {
        if (view.isVisible)
        {
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
            animation.setAnimationListener(object : Animation.AnimationListener
            {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?)
                {
                    view.visibility = View.GONE
                    overlay.visibility = View.GONE
                    callback()
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            view.startAnimation(animation)
        } else
        {
            callback()
        }
    }
    @SuppressLint("DefaultLocale")
    fun onDaySelected(day: Int?) {
        selectedDay = if (selectedDay == day) null else day
        (numberRecyclerView.adapter as? NumberAdapter)?.setSelectedDay(selectedDay)

        sharedPreferences.edit {
            if (selectedDay != null) {
                putInt("selectedDay", selectedDay!!)
            } else {
                remove("selectedDay")
            }
        }
        fetchSpecificCall()
        saveLastApiCallParams()

        // Re-check and show arrows after the selection UI updates
        handler.postDelayed({
            updateArrowVisibility()
        }, 100) // A small delay ensures the RecyclerView has finished its layout pass.
    }

    private fun clearDaySelection()
    {
        selectedDay = null
        (numberRecyclerView.adapter as? NumberAdapter)?.setSelectedDay(null)
        sharedPreferences.edit { remove("selectedDay") }
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