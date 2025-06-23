package a48626.sumolmbao.second_fragment

import a48626.sumolmbao.R
import a48626.sumolmbao.data.TournamentResultDisplayData
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TournamentResultAdapter(
    private var results: List<TournamentResultDisplayData>
) : RecyclerView.Adapter<TournamentResultAdapter.TournamentResultViewHolder>() {

    inner class TournamentResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankTextView: TextView = itemView.findViewById(R.id.tournamentRank)
        val scoreTextView: TextView = itemView.findViewById(R.id.tournamentScore)
        val dateTextView: TextView = itemView.findViewById(R.id.tournamentDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TournamentResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tournament_result, parent, false)
        return TournamentResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: TournamentResultViewHolder, position: Int) {
        val result = results[position]
        holder.rankTextView.text = result.rank
        holder.scoreTextView.text = result.score
        holder.dateTextView.text = result.date
    }

    override fun getItemCount(): Int = results.size

    fun updateData(newResults: List<TournamentResultDisplayData>) {
        results = newResults
        notifyDataSetChanged()
    }
}