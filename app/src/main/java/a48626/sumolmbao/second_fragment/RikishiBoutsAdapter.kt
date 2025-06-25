package a48626.sumolmbao.second_fragment

import a48626.sumolmbao.R
import a48626.sumolmbao.data.RikishiMatch
import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class RikishiBoutsAdapter(
    private var bouts: List<RikishiMatch>,
    private val rikishiId: Int,
    private var spoilerModeEnabled: Boolean // This is the correct class property name
) : RecyclerView.Adapter<RikishiBoutsAdapter.BoutViewHolder>() {

    // Use bout's unique identifier for revealed state to handle specific bouts
    private val revealedState = mutableMapOf<Int, Boolean>()

    inner class BoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eastShikona: TextView = itemView.findViewById(R.id.eastShikona)
        val eastRank: TextView = itemView.findViewById(R.id.eastRank)
        val eastCircle: View = itemView.findViewById(R.id.eastCircle)
        val westShikona: TextView = itemView.findViewById(R.id.westShikona)
        val westRank: TextView = itemView.findViewById(R.id.westRank)
        val westCircle: View = itemView.findViewById(R.id.westCircle)
        val technique: TextView = itemView.findViewById(R.id.technique)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rikishi_bout_row, parent, false)
        return BoutViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BoutViewHolder, position: Int) {
        val bout = bouts[position]
        // A unique ID for the bout to manage its revealed state consistently
        val boutUniqueId = "${bout.bashoId}-${bout.day}-${bout.matchNo}".hashCode()

        // If spoilerModeEnabled is true, start with hidden (false) unless already revealed
        val isRevealed = revealedState[boutUniqueId] ?: false // Default to false if not in map

        holder.apply {
            eastShikona.text = getFirstName(bout.eastShikona)
            eastRank.text = convertRankToShort(bout.eastRank)
            westShikona.text = getFirstName(bout.westShikona)
            westRank.text = convertRankToShort(bout.westRank)

            dateText.text = formatBashoDay(bout.bashoId, bout.day)

            if (spoilerModeEnabled && !isRevealed) {
                // NO SPOILER MODE: Hide winner info by showing both circles black and hiding technique
                eastCircle.setBackgroundResource(R.drawable.circle_loser)
                westCircle.setBackgroundResource(R.drawable.circle_loser)
                technique.visibility = View.GONE
            } else {
                // REVEALED or SPOILER MODE IS OFF: Show actual winner and technique
                val eastIsWinner = bout.winnerId == bout.eastId
                eastCircle.setBackgroundResource(if (eastIsWinner) R.drawable.circle_winner else R.drawable.circle_loser)
                westCircle.setBackgroundResource(if (!eastIsWinner) R.drawable.circle_winner else R.drawable.circle_loser)
                technique.text = bout.kimarite.replaceFirstChar { it.uppercase() }
                technique.visibility = View.VISIBLE
            }

            // Click listener to toggle spoiler for THIS specific bout
            itemView.setOnClickListener {
                if (spoilerModeEnabled) { // Only toggle if spoiler mode is active
                    val newState = !(revealedState[boutUniqueId] ?: false) // Toggle current state
                    revealedState[boutUniqueId] = newState
                    notifyItemChanged(position) // Rebind this item to update its display
                }
            }
        }
    }

    override fun getItemCount(): Int = bouts.size

    // Modified: Sort newBouts in descending order by day
    fun updateData(newBouts: List<RikishiMatch>, newSpoilerMode: Boolean) {
        bouts = newBouts.sortedByDescending { it.day } // Sort descending by day (Day 15 to Day 1)
        spoilerModeEnabled = newSpoilerMode
        revealedState.clear() // Clear revealed state on any data update to ensure consistency
        notifyDataSetChanged()
    }

    private fun getFirstName(fullName: String?): String {
        return fullName?.replace("#", "")?.split(" ")?.firstOrNull() ?: ""
    }

    private fun convertRankToShort(rank: String): String {
        val regex = Regex("""(Yokozuna|Ozeki|Sekiwake|Komusubi|Maegashira|Juryo|Makushita|Sandanme|Jonidan|Jonokuchi)\s*(\d*)\s*(East|West)?""", RegexOption.IGNORE_CASE)
        val match = regex.find(rank) ?: return rank

        return match.let {
            val (name, number, side) = it.destructured
            val short = when (name.lowercase()) {
                "yokozuna" -> "Y"
                "ozeki" -> "O"
                "sekiwake" -> "S"
                "komusubi" -> "K"
                "maegashira" -> "M"
                "juryo" -> "J"
                "makushita" -> "Ms"
                "sandanme" -> "Sd"
                "jonidan" -> "Jd"
                "jonokuchi" -> "Jk"
                else -> name.substring(0, Math.min(2, name.length))
            }
            val shortSide = when (side.lowercase()) {
                "east" -> "e"
                "west" -> "w"
                else -> ""
            }
            if (number.isNotBlank()) "$short$number$shortSide" else short
        }
    }

    // Modified: formatBashoDay to use full year (YYYY)
    private fun formatBashoDay(bashoId: String, day: Int): String {
        if (bashoId.length != 6) return ""
        try {
            val year = bashoId.substring(0, 4) // Get full year
            val monthNum = bashoId.substring(4, 6).toInt()
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.MONTH, monthNum - 1)
            val monthName = SimpleDateFormat("MMM", Locale.ENGLISH).format(calendar.time)
            return "$monthName $year - Day $day" // Use full year here
        } catch (e: Exception) {
            Log.e("RikishiBoutsAdapter", "Error formatting bashoId: $bashoId", e)
            return ""
        }
    }
}