package a48626.sumolmbao.first_fragment

import a48626.sumolmbao.data.Banzuke
import a48626.sumolmbao.data.BanzukeRow
import a48626.sumolmbao.data.Basho
import a48626.sumolmbao.R
import a48626.sumolmbao.data.TorikumiResponse
import a48626.sumolmbao.retrofit.RetrofitInstance
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
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
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import java.util.Calendar
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.core.content.edit
import androidx.core.view.isVisible
import kotlin.collections.get

class FirstFragment : Fragment() {

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

    private var tooltipView: TextView? = null
    private var isTooltipShowing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    @SuppressLint("SetTextI18n", "CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("UserSelections", Context.MODE_PRIVATE)

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

        overlay.setOnClickListener {
            hideRecyclerViewWithAnimation(yearRecyclerView)
            hideRecyclerViewWithAnimation(monthRecyclerView)
            hideRecyclerViewWithAnimation(divisionRecyclerView)
            overlay.visibility = View.GONE
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Vai buscar as seleções previamente feitas
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

        // Atualiza o UI
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

        // Setup months RecyclerView
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

        // Setup divisions RecyclerView
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

        // Group into pages of 5 numbers each
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

        // Set initial state for first page
        numberRecyclerView.post {
            val layoutManager = numberRecyclerView.layoutManager as LinearLayoutManager
            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            updateNumberArrowsVisibility(firstVisiblePosition)
        }

        numberRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val adapter = numberRecyclerView.adapter as? NumberAdapter

                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING,
                    RecyclerView.SCROLL_STATE_SETTLING -> {
                        // Hide arrows during scrolling
                        val firstVisible = layoutManager.findFirstVisibleItemPosition()
                        adapter?.getViewHolderAt(firstVisible)?.let { viewHolder ->
                            viewHolder.leftArrow.visibility = View.GONE
                            viewHolder.rightArrow.visibility = View.GONE
                        }
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        // Show arrows according to position when scrolling stops
                        val snappedView = snapHelper.findSnapView(layoutManager)
                        val snappedPosition = snappedView?.let { layoutManager.getPosition(it) } ?: return

                        adapter?.getViewHolderAt(snappedPosition)?.let { viewHolder ->
                            viewHolder.leftArrow.visibility = if (snappedPosition > 0) View.VISIBLE else View.GONE
                            viewHolder.rightArrow.visibility = if (snappedPosition < adapter.itemCount - 1) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
        })

        // Handle Year Button click
        yearButton.setOnClickListener {
            animateButtonClick(it)

            // Hide divisionRecyclerView if it's visible
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

        // Handle Month Button click
        monthButton.setOnClickListener {
            animateButtonClick(it)

            // Hide divisionRecyclerView if it's visible
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

        // Handle Division Button click
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
        setupTooltips()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setupTooltips() {
        val titleRikishiLeft = view?.findViewById<TextView>(R.id.titleRikishiLeft)
        val titleTorikumi = view?.findViewById<TextView>(R.id.titleTorikumi)
        val titleRikishiRight = view?.findViewById<TextView>(R.id.titleRikishiRight)
        val overlay = view?.findViewById<View>(R.id.overlay)

        tooltipView = TextView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.tooltip_background)
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
            elevation = 12f
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        (view as? ViewGroup)?.addView(tooltipView)

        val clickListener = View.OnClickListener { v ->
            when (v.id) {
                R.id.titleRikishiLeft, R.id.titleRikishiRight -> {
                    showTooltip(v, getString(R.string.rikishi_meaning))
                }
                R.id.titleTorikumi -> {
                    showTooltip(v, getString(R.string.kimarite_meaning))
                }
            }
        }

        titleRikishiLeft?.setOnClickListener(clickListener)
        titleTorikumi?.setOnClickListener(clickListener)
        titleRikishiRight?.setOnClickListener(clickListener)

        overlay?.setOnClickListener {
            if (isTooltipShowing) {
                hideTooltip()
            } else {
                // Original behavior - hide all recycler views
                hideRecyclerViewWithAnimation(yearRecyclerView)
                hideRecyclerViewWithAnimation(monthRecyclerView)
                hideRecyclerViewWithAnimation(divisionRecyclerView)
            }
        }
    }

    private fun showTooltip(anchorView: View, text: String) {
        if (isTooltipShowing) {
            hideTooltip()
            return
        }

        tooltipView?.apply {
            textSize = 18f // Set your desired text size
            maxWidth = (resources.displayMetrics.widthPixels * 0.8).toInt()
            measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        }

        tooltipView?.text = text
        tooltipView?.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        val anchorLocation = IntArray(2)
        anchorView.getLocationOnScreen(anchorLocation)

        // Calculate position - centered above the clicked view
        val x = anchorLocation[0] + (anchorView.width / 2) - (tooltipView?.measuredWidth ?: 0) / 2
        val y = anchorLocation[1] - (tooltipView?.measuredHeight ?: 0) - 16

        tooltipView?.x = x.toFloat()
        tooltipView?.y = y.toFloat()
        tooltipView?.visibility = View.VISIBLE

        view?.findViewById<View>(R.id.overlay)?.visibility = View.VISIBLE
        isTooltipShowing = true
    }

    private fun hideTooltip() {
        tooltipView?.visibility = View.GONE
        view?.findViewById<View>(R.id.overlay)?.visibility = View.GONE
        isTooltipShowing = false
    }

    override fun onPause() {
        super.onPause()
        // Cancel all animations to prevent leaks
        listOf(yearButton, monthButton, divisionButton, banzukeRecyclerView,
            bashoWinnersContainer, numberRecyclerView, torikumiRecyclerView).forEach {
            it.animate().cancel()
        }
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
        // Hide all content views initially
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

        // Control day selector visibility
        numberRecyclerView.visibility = if (selectedDivision != null && selectedDivision != "Division") {
            View.VISIBLE
        } else {
            View.GONE
        }

        when {
            selectedDivision == null || selectedDivision == "Division" -> {
                // Show basho winners
                bashoWinnersContainer.visibility = View.VISIBLE
                fetchBasho(bashoId)
            }
            selectedDay != null -> {
                // Show torikumi
                torikumiContainer.visibility = View.VISIBLE
                torikumiRecyclerView.visibility = View.VISIBLE
                fetchTorikumi(bashoId, selectedDivision!!, selectedDay!!)
            }
            else -> {
                // Show banzuke
                banzukeContainer.visibility = View.GONE
                banzukeRecyclerView.visibility = View.VISIBLE
                fetchBanzuke(bashoId, selectedDivision!!)
            }
        }
    }

    // Update fetchBasho function:
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

                    // Process main winners (yusho)
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

                    // Process special prizes
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

                    // Process each sanyaku rank type
                    sanyakuRanks.forEach { rankType ->
                        // Get all wrestlers of this rank type, preserving original order
                        val rankWrestlers = (east + west)
                            .filter { it.rank.startsWith(rankType) }
                            .sortedWith(compareBy(
                                // Sort by position number first
                                { wrestler ->
                                    val parts = wrestler.rank.split(" ")
                                    parts.getOrNull(1)?.toIntOrNull() ?: 1
                                },
                                // Then sort East before West
                                { it.side }
                            ))

                        // Group by their position number (1, 2, etc.)
                        val grouped = rankWrestlers.groupBy { wrestler ->
                            val parts = wrestler.rank.split(" ")
                            parts.getOrNull(1)?.toIntOrNull() ?: 1
                        }

                        grouped.forEach { (positionNumber, wrestlers) ->
                            // Find East and West wrestlers for this position
                            val eastWrestler = wrestlers.find { it.side == "East" }
                            val westWrestler = wrestlers.find { it.side == "West" }

                            when {
                                // Case 1: Both East and West exist
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
                                // Case 2: Only East exists
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
                                // Case 3: Only West exists
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

                    // Handle non-sanyaku ranks
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

    private fun fetchTorikumi(bashoId: String, division: String, day: Int) {
        RetrofitInstance.api.getTorikumi(bashoId, division, day).enqueue(object : Callback<TorikumiResponse> {
            override fun onResponse(call: Call<TorikumiResponse>, response: Response<TorikumiResponse>) {
                if (response.isSuccessful) {
                    val torikumiList = response.body()?.torikumi ?: emptyList()
                    val adapter = TorikumiAdapter(torikumiList)

                    // Set up click listener for techniques
                    adapter.onTechniqueClickListener = { kimarite, anchorView ->
                        val translation = kimariteTranslations[kimarite] ?: "No translation available"
                        showTooltip(anchorView, translation)
                    }

                    torikumiRecyclerView.adapter = adapter
                    // Explicitly manage visibility here upon success
                    torikumiContainer.visibility = View.VISIBLE
                    torikumiRecyclerView.visibility = View.VISIBLE
                    banzukeRecyclerView.visibility = View.GONE
                    bashoWinnersContainer.visibility = View.GONE
                } else {
                    Log.e("TorikumiFetch", "Error ${response.code()}: ${response.errorBody()?.string()}")
                    torikumiRecyclerView.visibility = View.GONE
                    banzukeRecyclerView.visibility = View.GONE
                    bashoWinnersContainer.visibility = View.GONE
                }
            }
            override fun onFailure(call: Call<TorikumiResponse>, t: Throwable) {
                Log.e("TorikumiFetch", "Network failure: ${t.message}", t)
                // Hide all containers on failure
                torikumiContainer.visibility = View.GONE
                torikumiRecyclerView.visibility = View.GONE
                banzukeRecyclerView.visibility = View.GONE
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
        // Reset the view first to ensure animations work consistently
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
    fun onDaySelected(day: Int?)
    {
        selectedDay = if (selectedDay == day) null else day
        (numberRecyclerView.adapter as? NumberAdapter)?.setSelectedDay(selectedDay)

        // Save to SharedPreferences
        sharedPreferences.edit {
            if (selectedDay != null)
            {
                putInt("selectedDay", selectedDay!!)
            } else
            {
                remove("selectedDay")
            }
        }
        fetchSpecificCall()
        saveLastApiCallParams()
    }

    private fun clearDaySelection()
    {
        selectedDay = null
        (numberRecyclerView.adapter as? NumberAdapter)?.setSelectedDay(null)
        sharedPreferences.edit { remove("selectedDay") }
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
