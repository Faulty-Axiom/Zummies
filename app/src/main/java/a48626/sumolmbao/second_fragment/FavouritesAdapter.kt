package a48626.sumolmbao.favourites

import a48626.sumolmbao.DisplayItem
import a48626.sumolmbao.R
import a48626.sumolmbao.data.Rikishi
import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.TournamentResultDisplayData
import a48626.sumolmbao.second_fragment.TournamentResultAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavouritesAdapter(
    internal var favouriteRikishiList: List<DisplayItem>,
    private var lastTournamentBanzukeMap: Map<Int, Rikishi>? = null,
    private val onCheckScoreClick: (RikishiDetails) -> Unit,
    private var cachedTournamentHistory: Map<Int, List<TournamentResultDisplayData>>,
    private var gridVisibilityState: Map<Int, Boolean>,
    // ADDED: The missing onDivisionHeaderClick parameter
    private val onDivisionHeaderClick: (String) -> Unit // This line was missing
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_RIKISHI = 0
    private val TYPE_DIVISION_SEPARATOR = 1

    inner class RikishiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rikishiName: TextView = itemView.findViewById(R.id.rikishiName)
        val checkScoreButton: TextView = itemView.findViewById(R.id.checkScoreButton)
        val tournamentGridContainer: LinearLayout = itemView.findViewById(R.id.tournamentGridContainer)
        val tournamentGridView: RecyclerView = itemView.findViewById(R.id.tournamentGridView)
    }

    inner class DivisionSeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val divisionNameTextView: TextView = itemView.findViewById(R.id.divisionNameTextView)
    }

    override fun getItemViewType(position: Int): Int {
        return when (favouriteRikishiList[position]) {
            is DisplayItem.RikishiDisplayItem -> TYPE_RIKISHI
            is DisplayItem.DivisionSeparatorItem -> TYPE_DIVISION_SEPARATOR
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_RIKISHI -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_favourite_rikishi, parent, false)
                RikishiViewHolder(view)
            }
            TYPE_DIVISION_SEPARATOR -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_division_separator, parent, false)
                DivisionSeparatorViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

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

                val isGridVisible = gridVisibilityState[rikishiDetails.id] == true
                if (isGridVisible && cachedTournamentHistory.containsKey(rikishiDetails.id)) {
                    rikishiHolder.tournamentGridContainer.visibility = View.VISIBLE
                    rikishiHolder.tournamentGridView.adapter = TournamentResultAdapter(cachedTournamentHistory[rikishiDetails.id] ?: emptyList())
                    rikishiHolder.tournamentGridView.layoutManager = LinearLayoutManager(rikishiHolder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
                } else {
                    rikishiHolder.tournamentGridContainer.visibility = View.GONE
                }

                rikishiHolder.checkScoreButton.setOnClickListener {
                    onCheckScoreClick(rikishiDetails)
                }
            }
            is DisplayItem.DivisionSeparatorItem -> {
                val separatorHolder = holder as DivisionSeparatorViewHolder
                separatorHolder.divisionNameTextView.text = item.divisionName
                // ADDED: Click listener for the division separator
                separatorHolder.itemView.setOnClickListener {
                    onDivisionHeaderClick(item.divisionName)
                }
            }
        }
    }

    override fun getItemCount(): Int = favouriteRikishiList.size

    fun updateData(newFavourites: List<DisplayItem>,
                   newBanzukeMap: Map<Int, Rikishi>? = null,
                   newCachedTournamentHistory: Map<Int, List<TournamentResultDisplayData>>,
                   newGridVisibilityState: Map<Int, Boolean>) {
        favouriteRikishiList = newFavourites
        lastTournamentBanzukeMap = newBanzukeMap
        cachedTournamentHistory = newCachedTournamentHistory
        gridVisibilityState = newGridVisibilityState
        notifyDataSetChanged()
    }

    private fun formatRecord(wins: Int?, losses: Int?, absences: Int?): String {
        val w = wins ?: 0
        val l = losses ?: 0
        val a = absences ?: 0
        return if (a > 0) "$w-$l-$a" else "$w-$l"
    }
}