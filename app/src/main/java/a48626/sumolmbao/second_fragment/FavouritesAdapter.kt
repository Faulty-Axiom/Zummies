package a48626.sumolmbao.favourites

import a48626.sumolmbao.DisplayItem
import a48626.sumolmbao.R
import a48626.sumolmbao.data.Rikishi
import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.RikishiMatch
import a48626.sumolmbao.data.TournamentResultDisplayData
import a48626.sumolmbao.second_fragment.RikishiBoutsAdapter
import a48626.sumolmbao.second_fragment.TournamentResultAdapter
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class FavouritesAdapter(
    internal var favouriteRikishiList: List<DisplayItem>,
    private var lastTournamentBanzukeMap: Map<Int, Rikishi>?,
    private val onCheckScoreClick: (RikishiDetails) -> Unit,
    private var cachedTournamentHistory: Map<Int, List<TournamentResultDisplayData>>,
    private var cachedRikishiBouts: Map<Int, List<RikishiMatch>>,
    private var gridVisibilityState: Map<Int, Boolean>,
    private var boutsVisibilityState: Map<Int, Boolean>,
    private val onDivisionHeaderClick: (String) -> Unit,
    val onRikishiNameClick: (RikishiDetails) -> Unit,
    private val kimariteMap: Map<String, String>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_RIKISHI = 0
    private val TYPE_DIVISION_SEPARATOR = 1

    private val handler = Handler(Looper.getMainLooper())
    private var translationRunnable: Runnable? = null
    private var isTranslationLocked = false
    private val originalKimariteTexts = mutableMapOf<TextView, CharSequence>()

    inner class RikishiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rikishiName: TextView = itemView.findViewById(R.id.rikishiName)
        val checkScoreButton: TextView = itemView.findViewById(R.id.checkScoreButton)
        val tournamentGridContainer: LinearLayout = itemView.findViewById(R.id.tournamentGridContainer)
        val tournamentGridView: RecyclerView = itemView.findViewById(R.id.tournamentGridView)
        val rikishiBoutsRecyclerView: RecyclerView = itemView.findViewById(R.id.rikishiBoutsRecyclerView)
    }

    inner class DivisionSeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val divisionNameTextView: TextView = itemView.findViewById(R.id.divisionNameTextView)
    }

    override fun getItemViewType(position: Int): Int { return if (favouriteRikishiList[position] is DisplayItem.RikishiDisplayItem) TYPE_RIKISHI else TYPE_DIVISION_SEPARATOR }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_RIKISHI -> RikishiViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_favourite_rikishi, parent, false))
            else -> DivisionSeparatorViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_division_separator, parent, false))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = favouriteRikishiList[position]) {
            is DisplayItem.RikishiDisplayItem -> {
                val rikishiDetails = item.rikishiDetails
                val rikishiHolder = holder as RikishiViewHolder

                var displayName = rikishiDetails.shikonaEn
                val isRetired = rikishiDetails.currentRank?.contains("Retired", ignoreCase = true) == true || rikishiDetails.intai == true

                // **FIX**: Use the full 'currentRank' property from the data class directly.
                if (!isRetired && !rikishiDetails.currentRank.isNullOrBlank()) {
                    displayName += " - ${rikishiDetails.currentRank}"
                } else if (isRetired) {
                    displayName += " - Retired"
                }
                rikishiHolder.rikishiName.text = displayName

                rikishiHolder.rikishiName.setOnClickListener { onRikishiNameClick(rikishiDetails) }

                val isGridVisible = gridVisibilityState.getOrDefault(rikishiDetails.id, false)
                if (isGridVisible) { rikishiHolder.tournamentGridContainer.visibility = View.VISIBLE; rikishiHolder.tournamentGridView.adapter = TournamentResultAdapter(cachedTournamentHistory.getOrDefault(rikishiDetails.id, emptyList())); rikishiHolder.tournamentGridView.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false) } else { rikishiHolder.tournamentGridContainer.visibility = View.GONE }

                val areBoutsVisible = boutsVisibilityState.getOrDefault(rikishiDetails.id, false)
                if (areBoutsVisible) {
                    rikishiHolder.rikishiBoutsRecyclerView.visibility = View.VISIBLE
                    val spoilerMode = FavouritesManager.loadSpoilerModePreference(holder.itemView.context)
                    val boutsAdapter = RikishiBoutsAdapter(cachedRikishiBouts.getOrDefault(rikishiDetails.id, emptyList()), rikishiDetails.id, spoilerMode)
                    rikishiHolder.rikishiBoutsRecyclerView.adapter = boutsAdapter
                    rikishiHolder.rikishiBoutsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)

                    val touchSlop = ViewConfiguration.get(holder.itemView.context).scaledTouchSlop
                    rikishiHolder.rikishiBoutsRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                        private var downX = 0f
                        private var downY = 0f

                        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                            if (isTranslationLocked) { return true }
                            when (e.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    downX = e.x; downY = e.y
                                    rv.parent.requestDisallowInterceptTouchEvent(true)
                                    translationRunnable = Runnable {
                                        isTranslationLocked = true
                                        translateAllKimarite(rv, true)
                                    }
                                    handler.postDelayed(translationRunnable!!, 300)
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    if (abs(e.x - downX) > touchSlop || abs(e.y - downY) > touchSlop) {
                                        handler.removeCallbacks(translationRunnable ?: return false)
                                        rv.parent.requestDisallowInterceptTouchEvent(false)
                                    }
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    handler.removeCallbacks(translationRunnable ?: return false)
                                    rv.parent.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                            return false
                        }
                        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                            if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL) {
                                if (isTranslationLocked) {
                                    translateAllKimarite(rv, false)
                                    isTranslationLocked = false
                                    rv.parent.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                        }
                        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                            if (disallowIntercept) {
                                handler.removeCallbacks(translationRunnable ?: return)
                            }
                        }
                    })
                } else {
                    rikishiHolder.rikishiBoutsRecyclerView.visibility = View.GONE
                }
                rikishiHolder.checkScoreButton.setOnClickListener { onCheckScoreClick(rikishiDetails) }
            }
            is DisplayItem.DivisionSeparatorItem -> {
                val separatorHolder = holder as DivisionSeparatorViewHolder
                separatorHolder.divisionNameTextView.text = item.divisionName
                separatorHolder.itemView.setOnClickListener { onDivisionHeaderClick(item.divisionName) }
            }
        }
    }

    private fun translateAllKimarite(recyclerView: RecyclerView, translate: Boolean) {
        val adapter = recyclerView.adapter as? RikishiBoutsAdapter ?: return
        if (translate) {
            originalKimariteTexts.clear()
            for (i in 0 until recyclerView.childCount) {
                val holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i)) as? RikishiBoutsAdapter.BoutViewHolder
                if (holder != null) {
                    val kimarite = adapter.getKimariteAt(holder.adapterPosition) ?: ""
                    originalKimariteTexts[holder.technique] = holder.technique.text
                    holder.technique.text = kimariteMap[kimarite] ?: kimarite
                }
            }
        } else {
            if (originalKimariteTexts.isNotEmpty()) {
                originalKimariteTexts.forEach { (textView, originalText) ->
                    textView.text = originalText
                }
                originalKimariteTexts.clear()
            }
        }
    }

    override fun getItemCount(): Int = favouriteRikishiList.size
    fun updateData(newFavourites: List<DisplayItem>, newBanzukeMap: Map<Int, Rikishi>?, newCachedTournamentHistory: Map<Int, List<TournamentResultDisplayData>>, newCachedRikishiBouts: Map<Int, List<RikishiMatch>>, newGridVisibilityState: Map<Int, Boolean>, newBoutsVisibilityState: Map<Int, Boolean>) {
        this.favouriteRikishiList = newFavourites; this.lastTournamentBanzukeMap = newBanzukeMap; this.cachedTournamentHistory = newCachedTournamentHistory; this.cachedRikishiBouts = newCachedRikishiBouts; this.gridVisibilityState = newGridVisibilityState; this.boutsVisibilityState = newBoutsVisibilityState; notifyDataSetChanged()
    }
}