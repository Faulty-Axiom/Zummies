package a48626.sumolmbao.first_fragment

import a48626.sumolmbao.data.BanzukeRow
import a48626.sumolmbao.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BanzukeAdapter(private val banzukeRows: List<BanzukeRow>) :
    RecyclerView.Adapter<BanzukeAdapter.BanzukeViewHolder>() {

    class BanzukeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eastShikona: TextView = view.findViewById(R.id.eastShikona)
        val eastPlaceholder: TextView = view.findViewById(R.id.eastPlaceholder)
        val eastRank: TextView = view.findViewById(R.id.eastRank)
        val eastRecord: TextView = view.findViewById(R.id.eastRecord)
        val westShikona: TextView = view.findViewById(R.id.westShikona)
        val westPlaceholder: TextView = view.findViewById(R.id.westPlaceholder)
        val westRecord: TextView = view.findViewById(R.id.westRecord)
        val westRank: TextView = view.findViewById(R.id.westRank)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BanzukeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banzuke_row, parent, false)
        return BanzukeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BanzukeViewHolder, position: Int) {
        val row = banzukeRows[position]

        // Handle East side
        if (row.east == null && row.isSanyakuRow) {
            holder.eastShikona.visibility = View.GONE
            holder.eastPlaceholder.visibility = View.VISIBLE
            holder.eastRank.text = row.eastRank ?: ""
            holder.eastRecord.text = ""
        } else {
            holder.eastShikona.visibility = View.VISIBLE
            holder.eastPlaceholder.visibility = View.GONE
            holder.eastShikona.text = row.east?.shikonaEn ?: ""
            holder.eastRank.text = row.eastRank ?: ""
            holder.eastRecord.text = row.east?.let { formatRecord(it.wins, it.losses, it.absences) } ?: ""
        }

        // Handle West side
        if (row.west == null && row.isSanyakuRow && row.east != null) {
            holder.westShikona.visibility = View.GONE
            holder.westPlaceholder.visibility = View.VISIBLE
            holder.westRank.text = row.westRank ?: ""
            holder.westRecord.text = ""
        } else {
            holder.westShikona.visibility = View.VISIBLE
            holder.westPlaceholder.visibility = View.GONE
            holder.westShikona.text = row.west?.shikonaEn ?: ""
            holder.westRank.text = row.westRank ?: ""
            holder.westRecord.text = row.west?.let { formatRecord(it.wins, it.losses, it.absences) } ?: ""
        }
    }

    private fun formatRecord(wins: Int?, losses: Int?, absences: Int?): String {
        val w = wins ?: 0
        val l = losses ?: 0
        val a = absences ?: 0
        return if (a > 0) "$w-$l-$a" else "$w-$l"
    }

    override fun getItemCount() = banzukeRows.size
}