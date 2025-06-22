// app/src/main/java/a48626/sumolmbao/second_fragment/FavouritesAdapter.kt
package a48626.sumolmbao.favourites

import a48626.sumolmbao.R
import a48626.sumolmbao.data.Rikishi
import a48626.sumolmbao.data.RikishiDetails
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavouritesAdapter(
    private var favouriteRikishiList: List<RikishiDetails>,
    private var lastTournamentBanzukeMap: Map<Int, Rikishi>? = null,
    private val onCheckScoreClick: (RikishiDetails, TextView) -> Unit,
    private var cachedPreviousTournamentScores: Map<Int, String> // In-memory cache
) : RecyclerView.Adapter<FavouritesAdapter.FavouriteViewHolder>() {

    inner class FavouriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rikishiName: TextView = itemView.findViewById(R.id.rikishiName)
        val rikishiScore: TextView = itemView.findViewById(R.id.rikishiScore)
        val checkScoreButton: TextView = itemView.findViewById(R.id.checkScoreButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favourite_rikishi, parent, false)
        return FavouriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavouriteViewHolder, position: Int) {
        val rikishiDetails = favouriteRikishiList[position]
        holder.rikishiName.text = rikishiDetails.shikonaEn

        // ONLY show score if it's explicitly cached from a 'Check Score' button click
        val cachedScore = cachedPreviousTournamentScores[rikishiDetails.id]
        if (cachedScore != null) {
            holder.rikishiScore.text = cachedScore
            holder.rikishiScore.visibility = View.VISIBLE
        } else {
            // If not explicitly cached, ensure it's hidden on initial bind or re-bind
            holder.rikishiScore.visibility = View.GONE
            holder.rikishiScore.text = "" // Clear any stale text to prevent showing old data
        }

        // Set up click listener for the new button
        holder.checkScoreButton.setOnClickListener {
            onCheckScoreClick(rikishiDetails, holder.rikishiScore)
        }
    }

    override fun getItemCount(): Int = favouriteRikishiList.size

    fun updateData(newFavourites: List<RikishiDetails>, newBanzukeMap: Map<Int, Rikishi>? = null, newCachedPreviousTournamentScores: Map<Int, String>) {
        favouriteRikishiList = newFavourites
        lastTournamentBanzukeMap = newBanzukeMap
        cachedPreviousTournamentScores = newCachedPreviousTournamentScores
        notifyDataSetChanged()
    }

    private fun formatRecord(wins: Int?, losses: Int?, absences: Int?): String {
        val w = wins ?: 0
        val l = losses ?: 0
        val a = absences ?: 0
        return if (a > 0) "$w-$l-$a" else "$w-$l"
    }
}