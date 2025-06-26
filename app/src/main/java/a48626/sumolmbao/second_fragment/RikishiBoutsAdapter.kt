package a48626.sumolmbao.second_fragment

import a48626.sumolmbao.R
import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.RikishiMatch
import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

// --- FIX: Updated constructor to accept all necessary data ---
class RikishiBoutsAdapter(
    private var bouts: List<RikishiMatch>,
    private val allRikishiMap: Map<Int, RikishiDetails>,
    private var spoilerModeEnabled: Boolean,
    private val onVideoClick: (bout: RikishiMatch, eastSumoDbId: Int?, westSumoDbId: Int?) -> Unit
) : RecyclerView.Adapter<RikishiBoutsAdapter.BoutViewHolder>() {

    private val revealedState = mutableMapOf<Int, Boolean>()
    private var isVideoVisible = false

    inner class BoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eastShikona: TextView = itemView.findViewById(R.id.eastShikona)
        val eastRank: TextView = itemView.findViewById(R.id.eastRank)
        val eastCircle: View = itemView.findViewById(R.id.eastCircle)
        val westShikona: TextView = itemView.findViewById(R.id.westShikona)
        val westRank: TextView = itemView.findViewById(R.id.westRank)
        val westCircle: View = itemView.findViewById(R.id.westCircle)
        val technique: TextView = itemView.findViewById(R.id.technique)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val videoButton: TextView = itemView.findViewById(R.id.videoButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rikishi_bout_row, parent, false)
        return BoutViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BoutViewHolder, position: Int) {
        val bout = bouts[position]
        val boutUniqueId = bout.hashCode()
        val isRevealed = revealedState[boutUniqueId] ?: false

        holder.apply {
            eastShikona.text = getFirstName(bout.eastShikona)
            eastRank.text = convertRankToShort(bout.eastRank)
            westShikona.text = getFirstName(bout.westShikona)
            westRank.text = convertRankToShort(bout.westRank)
            dateText.text = formatBashoDay(bout.bashoId, bout.day)

            videoButton.visibility = if (isVideoVisible) View.VISIBLE else View.GONE

            // Set the click listener to trigger the lambda passed from FavouritesAdapter
            videoButton.setOnClickListener {
                val eastSumoDbId = allRikishiMap[bout.eastId]?.sumodbId
                val westSumoDbId = allRikishiMap[bout.westId]?.sumodbId
                onVideoClick(bout, eastSumoDbId, westSumoDbId)
            }

            if (spoilerModeEnabled && !isRevealed) {
                eastCircle.setBackgroundResource(R.drawable.circle_loser)
                westCircle.setBackgroundResource(R.drawable.circle_loser)
                technique.visibility = View.GONE
                videoButton.visibility = View.GONE // Also hide video button if spoiler is active and not revealed
            } else {
                technique.visibility = View.VISIBLE
                videoButton.visibility = View.VISIBLE

                val eastIsWinner = bout.winnerId == bout.eastId
                eastCircle.setBackgroundResource(if (eastIsWinner) R.drawable.circle_winner else R.drawable.circle_loser)
                westCircle.setBackgroundResource(if (!eastIsWinner) R.drawable.circle_winner else R.drawable.circle_loser)
                technique.text = bout.kimarite.replaceFirstChar { it.uppercase() }
            }

            itemView.setOnClickListener {
                if (spoilerModeEnabled) {
                    toggleSpoiler(position)
                }
            }
        }
    }

    override fun getItemCount(): Int = bouts.size

    fun setVideoVisibility(isVisible: Boolean) {
        if (this.isVideoVisible != isVisible) {
            this.isVideoVisible = isVisible
            notifyDataSetChanged()
        }
    }

    fun getKimariteAt(position: Int): String? {
        return bouts.getOrNull(position)?.kimarite?.replaceFirstChar { it.uppercase() }
    }

    fun toggleSpoiler(position: Int) {
        if (spoilerModeEnabled) {
            val bout = bouts.getOrNull(position) ?: return
            val boutUniqueId = bout.hashCode()
            revealedState[boutUniqueId] = !(revealedState[boutUniqueId] ?: false)
            notifyItemChanged(position)
        }
    }

    fun updateData(newBouts: List<RikishiMatch>, newSpoilerMode: Boolean) {
        bouts = newBouts.sortedByDescending { it.day }
        spoilerModeEnabled = newSpoilerMode
        revealedState.clear()
        notifyDataSetChanged()
    }

    private fun getFirstName(fullName: String?): String { return fullName?.replace("#", "")?.split(" ")?.firstOrNull() ?: "" }
    private fun convertRankToShort(rank: String): String { if (rank.isBlank()) return ""; val r = rank.lowercase(); val num = r.filter { it.isDigit() }; val side = when { r.contains("east") -> "e"; r.contains("west") -> "w"; else -> "" }; return when { r.startsWith("yokozuna") -> "Y$side"; r.startsWith("ozeki") -> "O$side"; r.startsWith("sekiwake") -> "S$side"; r.startsWith("komusubi") -> "K$side"; r.startsWith("maegashira") -> "M$num$side"; r.startsWith("juryo") -> "J$num$side"; r.startsWith("makushita") -> "Ms$num$side"; r.startsWith("sandanme") -> "Sd$num$side"; r.startsWith("jonidan") -> "Jd$num$side"; r.startsWith("jonokuchi") -> "Jk$num$side"; else -> rank } }
    private fun formatBashoDay(bashoId: String, day: Int): String { if (bashoId.length != 6) return ""; return try { val year = bashoId.substring(0, 4); val monthNum = bashoId.substring(4, 6).toInt(); val calendar = Calendar.getInstance(); calendar.set(Calendar.MONTH, monthNum - 1); val monthName = SimpleDateFormat("MMM", Locale.ENGLISH).format(calendar.time); "$monthName $year, Day $day" } catch (e: Exception) { "" } }
}