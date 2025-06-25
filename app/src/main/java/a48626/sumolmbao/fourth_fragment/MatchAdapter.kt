package a48626.sumolmbao.fourth_fragment

import a48626.sumolmbao.R
import a48626.sumolmbao.data.RikishiMatch
import android.annotation.SuppressLint
import android.util.Log // Import Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MatchAdapter(private var matches: List<RikishiMatch>) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    var onKimariteClickListener: ((String, View) -> Unit)? = null

    inner class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bashoDayText: TextView = itemView.findViewById(R.id.bashoDayText)
        val divisionText: TextView = itemView.findViewById(R.id.divisionText)
        val winnerText: TextView = itemView.findViewById(R.id.winnerText)
        val kimariteText: TextView = itemView.findViewById(R.id.kimariteText)
        val eastText: TextView = itemView.findViewById(R.id.eastText)
        val westText: TextView = itemView.findViewById(R.id.westText)
        val technique: TextView = itemView.findViewById(R.id.kimariteText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        return MatchViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_match, parent, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matches[position]
        Log.d("MatchAdapter", "Binding match at position $position: ${match.eastShikona} vs ${match.westShikona}") // NEW: Log when binding
        val (monthName, year) = formatBashoDate(match.bashoId)

        fun getDisplayName(shikonaEn: String?): String {
            return shikonaEn?.replace("#", "")?.split(" ")?.firstOrNull() ?: ""
        }

        holder.apply {
            bashoDayText.text = "$monthName $year - Day ${match.day}"
            divisionText.text = match.division

            holder.technique.text = match.kimarite.replaceFirstChar { it.uppercase() }
            holder.technique.setOnClickListener {
                onKimariteClickListener?.invoke(match.kimarite, holder.technique)
            }

            val eastName = getDisplayName(match.eastShikona)
            val westName = getDisplayName(match.westShikona)

            if (match.winnerId == match.eastId) {
                winnerText.text = "Winner: $eastName"
                eastText.text = "East: $eastName (${match.eastRank})"
                westText.text = "West: $westName (${match.westRank})"
            } else {
                winnerText.text = "Winner: $westName"
                eastText.text = "East: $eastName (${match.eastRank})"
                westText.text = "West: $westName (${match.westRank})"
            }
        }
    }

    private fun formatBashoDate(bashoId: String): Pair<String, String> {
        val year = bashoId.substring(0, 4)
        val month = bashoId.substring(4, 6).toInt()
        val monthName = SimpleDateFormat("MMM", Locale.US).format(
            GregorianCalendar(year.toInt(), month - 1, 1).time
        )
        return Pair(monthName, year)
    }

    override fun getItemCount(): Int = matches.size

    fun updateData(newMatches: List<RikishiMatch>?) {
        matches = (newMatches ?: emptyList()).sortedByDescending {
            it.bashoId + it.day.toString().padStart(2, '0')
        }
        Log.d("MatchAdapter", "updateData called. New matches size: ${matches.size}") // NEW: Log update data size
        notifyDataSetChanged()
    }
}