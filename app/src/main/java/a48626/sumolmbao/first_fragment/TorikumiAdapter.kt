package a48626.sumolmbao.first_fragment

import a48626.sumolmbao.R
import a48626.sumolmbao.data.RikishiDetails
import a48626.sumolmbao.data.Torikumi
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TorikumiAdapter(
    private val torikumiList: List<Torikumi>,
    private val rikishiMap: Map<Int, RikishiDetails>,
    private val onVideoClick: (Torikumi, Int?, Int?) -> Unit
) :
    RecyclerView.Adapter<TorikumiAdapter.TorikumiViewHolder>() {

    class TorikumiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eastShikona: TextView = view.findViewById(R.id.eastShikona)
        val eastRank: TextView = view.findViewById(R.id.eastRank)
        val eastCircle: View = view.findViewById(R.id.eastCircle)
        val westShikona: TextView = view.findViewById(R.id.westShikona)
        val westRank: TextView = view.findViewById(R.id.westRank)
        val westCircle: View = view.findViewById(R.id.westCircle)
        val technique: TextView = view.findViewById(R.id.technique)
        val videoButton: TextView = view.findViewById(R.id.videoButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorikumiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_torikumi_row, parent, false)
        return TorikumiViewHolder(view)
    }

    override fun onBindViewHolder(holder: TorikumiViewHolder, position: Int) {
        val torikumi = torikumiList[position]
        Log.d("KimariteDebug", "Original kimarite: ${torikumi.kimarite}")

        val eastIsWinner = torikumi.winnerId == torikumi.eastId

        holder.eastShikona.text = torikumi.eastShikona
        holder.westShikona.text = torikumi.westShikona
        holder.eastRank.text = convertRankToShort(torikumi.eastRank)
        holder.westRank.text = convertRankToShort(torikumi.westRank)

        val kimarite = torikumi.kimarite.replaceFirstChar { it.uppercase() }
        Log.d("KimariteDebug", "Processed kimarite: $kimarite")
        holder.technique.text = kimarite

        holder.eastCircle.setBackgroundResource(
            if (eastIsWinner) R.drawable.circle_winner else R.drawable.circle_loser
        )
        holder.westCircle.setBackgroundResource(
            if (!eastIsWinner) R.drawable.circle_winner else R.drawable.circle_loser
        )

        holder.videoButton.setOnClickListener {
            val eastSumoDbId = rikishiMap[torikumi.eastId]?.sumodbId
            val westSumoDbId = rikishiMap[torikumi.westId]?.sumodbId
            if (eastSumoDbId != null && westSumoDbId != null) {
                onVideoClick(torikumi, eastSumoDbId, westSumoDbId)
            } else {
                Log.e("TorikumiAdapter", "Could not find sumodbId for one or both rikishi. East: ${torikumi.eastId}, West: ${torikumi.westId}")
            }
        }
    }

    override fun getItemCount() = torikumiList.size

    fun getKimariteAt(position: Int): String {
        return torikumiList[position].kimarite.replaceFirstChar { it.uppercase() }
    }

    private fun convertRankToShort(rank: String): String {
        val regex = Regex("""(Yokozuna|Ozeki|Sekiwake|Komusubi|Maegashira|Juryo|Makushita|Sandanme|Jonidan|Jonokuchi)\s*(\d*)\s*(East|West)""", RegexOption.IGNORE_CASE)
        val match = regex.find(rank) ?: return rank

        val (name, number, side) = match.destructured
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
            else -> ""
        }
        return "$short$number${side.lowercase().first()}"
    }
}