package a48626.sumolmbao.favourites

import a48626.sumolmbao.DisplayItem
import a48626.sumolmbao.R
import a48626.sumolmbao.VideoPlayerActivity
import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.RikishiMatch
import a48626.sumolmbao.data.TournamentResultDisplayData
import a48626.sumolmbao.second_fragment.RikishiBoutsAdapter
import a48626.sumolmbao.second_fragment.TournamentResultAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
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
    private var allRikishiMap: Map<Int, RikishiDetails>,
    private val onCheckScoreClick: (RikishiDetails) -> Unit,
    private var cachedTournamentHistory: Map<Int, List<TournamentResultDisplayData>>,
    private var cachedRikishiBouts: Map<Int, List<RikishiMatch>>,
    private var gridVisibilityState: Map<Int, Boolean>,
    private var boutsVisibilityState: Map<Int, Boolean>,
    private var videoVisibilityState: Map<Int, Boolean>,
    private val onDivisionHeaderClick: (String) -> Unit,
    val onRikishiNameClick: (RikishiDetails) -> Unit,
    private val kimariteMap: Map<String, String>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_RIKISHI = 0
    private val TYPE_DIVISION_SEPARATOR = 1

    private val handler = Handler(Looper.getMainLooper())
    private var isTranslationLocked = false
    private val originalKimariteTexts = mutableMapOf<TextView, CharSequence>()

    // A map to keep track of listeners to prevent adding them multiple times
    private val listenerMap = mutableMapOf<RecyclerView, RecyclerView.OnItemTouchListener>()

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

    override fun getItemViewType(position: Int): Int {
        return if (favouriteRikishiList[position] is DisplayItem.RikishiDisplayItem) TYPE_RIKISHI else TYPE_DIVISION_SEPARATOR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_RIKISHI -> RikishiViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_favourite_rikishi, parent, false))
            else -> DivisionSeparatorViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_division_separator, parent, false))
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = favouriteRikishiList[position]) {
            is DisplayItem.RikishiDisplayItem -> {
                val rikishiDetails = item.rikishiDetails
                val rikishiHolder = holder as RikishiViewHolder

                var displayName = rikishiDetails.shikonaEn
                val isRetired = rikishiDetails.currentRank?.contains("Retired", ignoreCase = true) == true || rikishiDetails.intai == true

                if (!isRetired && !rikishiDetails.currentRank.isNullOrBlank()) {
                    displayName += " - ${rikishiDetails.currentRank}"
                } else if (isRetired) {
                    displayName += " - Retired"
                }
                rikishiHolder.rikishiName.text = displayName
                rikishiHolder.rikishiName.setOnClickListener { onRikishiNameClick(rikishiDetails) }

                val isGridVisible = gridVisibilityState.getOrDefault(rikishiDetails.id, false)
                if (isGridVisible) {
                    rikishiHolder.tournamentGridContainer.visibility = View.VISIBLE
                    rikishiHolder.tournamentGridView.adapter = TournamentResultAdapter(cachedTournamentHistory.getOrDefault(rikishiDetails.id, emptyList()))
                    rikishiHolder.tournamentGridView.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
                } else {
                    rikishiHolder.tournamentGridContainer.visibility = View.GONE
                }

                val areBoutsVisible = boutsVisibilityState.getOrDefault(rikishiDetails.id, false)
                val isVideoVisible = videoVisibilityState.getOrDefault(rikishiDetails.id, false)

                if (areBoutsVisible) {
                    rikishiHolder.rikishiBoutsRecyclerView.visibility = View.VISIBLE
                    val spoilerMode = FavouritesManager.loadSpoilerModePreference(holder.itemView.context)

                    val boutsAdapter = RikishiBoutsAdapter(
                        bouts = cachedRikishiBouts.getOrDefault(rikishiDetails.id, emptyList()),
                        allRikishiMap = allRikishiMap,
                        spoilerModeEnabled = spoilerMode,
                        onVideoClick = { bout, eastSumoDbId, westSumoDbId ->
                            val intent = Intent(holder.itemView.context, VideoPlayerActivity::class.java).apply {
                                putExtra(VideoPlayerActivity.EXTRA_BASHO_ID, bout.bashoId)
                                putExtra(VideoPlayerActivity.EXTRA_DAY, bout.day)
                                putExtra(VideoPlayerActivity.EXTRA_RIKISHI1_ID, eastSumoDbId)
                                putExtra(VideoPlayerActivity.EXTRA_RIKISHI2_ID, westSumoDbId)
                            }
                            holder.itemView.context.startActivity(intent)
                        }
                    )

                    boutsAdapter.setVideoVisibility(isVideoVisible)
                    rikishiHolder.rikishiBoutsRecyclerView.adapter = boutsAdapter
                    rikishiHolder.rikishiBoutsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)

                    // --- FIX: Corrected Touch Listener Logic ---
                    // Remove any old listener before adding a new one to prevent duplicates.
                    listenerMap[rikishiHolder.rikishiBoutsRecyclerView]?.let {
                        rikishiHolder.rikishiBoutsRecyclerView.removeOnItemTouchListener(it)
                    }

                    val touchSlop = ViewConfiguration.get(holder.itemView.context).scaledTouchSlop
                    val listener = object : RecyclerView.OnItemTouchListener {
                        private var downX = 0f
                        private var downY = 0f
                        private var translationRunnable: Runnable? = null

                        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                            when (e.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    downX = e.x
                                    downY = e.y
                                    translationRunnable = Runnable {
                                        isTranslationLocked = true
                                        rv.parent.requestDisallowInterceptTouchEvent(true)
                                        translateAllKimarite(rv, true)
                                    }
                                    handler.postDelayed(translationRunnable!!, 300)
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    if (abs(e.x - downX) > touchSlop || abs(e.y - downY) > touchSlop) {
                                        handler.removeCallbacks(translationRunnable ?: return false)
                                    }
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    handler.removeCallbacks(translationRunnable ?: return false)
                                    if (isTranslationLocked) {
                                        translateAllKimarite(rv, false)
                                        isTranslationLocked = false
                                        rv.parent.requestDisallowInterceptTouchEvent(false)
                                    }
                                }
                            }
                            return isTranslationLocked
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
                    }
                    rikishiHolder.rikishiBoutsRecyclerView.addOnItemTouchListener(listener)
                    listenerMap[rikishiHolder.rikishiBoutsRecyclerView] = listener

                } else {
                    rikishiHolder.rikishiBoutsRecyclerView.visibility = View.GONE
                }
                rikishiHolder.checkScoreButton.setOnClickListener { onCheckScoreClick(rikishiDetails) }
            }
            is DisplayItem.DivisionSeparatorItem -> {
                val separatorHolder = holder as DivisionSeparatorViewHolder
                separatorHolder.divisionNameTextView.text = "${item.divisionName} (${item.count})"
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

    fun updateData(
        newFavourites: List<DisplayItem>,
        newAllRikishiMap: Map<Int, RikishiDetails>,
        newCachedTournamentHistory: Map<Int, List<TournamentResultDisplayData>>,
        newCachedRikishiBouts: Map<Int, List<RikishiMatch>>,
        newGridVisibilityState: Map<Int, Boolean>,
        newBoutsVisibilityState: Map<Int, Boolean>,
        newVideoVisibilityState: Map<Int, Boolean>
    ) {
        this.favouriteRikishiList = newFavourites
        this.allRikishiMap = newAllRikishiMap
        this.cachedTournamentHistory = newCachedTournamentHistory
        this.cachedRikishiBouts = newCachedRikishiBouts
        this.gridVisibilityState = newGridVisibilityState
        this.boutsVisibilityState = newBoutsVisibilityState
        this.videoVisibilityState = newVideoVisibilityState
        notifyDataSetChanged()
    }
}