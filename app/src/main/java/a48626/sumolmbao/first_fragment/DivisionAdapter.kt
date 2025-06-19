package a48626.sumolmbao.first_fragment

import a48626.sumolmbao.R
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DivisionAdapter(
    private val divisions: List<String>,
    private val onDivisionSelected: (String) -> Unit,
    private val onDismiss: () -> Unit
) : RecyclerView.Adapter<DivisionAdapter.DivisionViewHolder>() {

    inner class DivisionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textContainer: View = itemView.findViewById(R.id.textContainer)
        val divisionText: TextView = itemView.findViewById(R.id.itemText)
        private val divider: View = itemView.findViewById(R.id.divider) // Add this

        @SuppressLint("SetTextI18n")
        fun bind(division: String, position: Int, isLast: Boolean) { // Add isLast
            val displayText = if (division != "Division") {
                val ordinalPrefix = when (position) {
                    1 -> "1st"
                    2 -> "2nd"
                    3 -> "3rd"
                    4 -> "4th"
                    5 -> "5th"
                    6 -> "6th"
                    else -> "${position + 1}th"
                }
                "$ordinalPrefix Division: $division"
            } else {
                division
            }

            divisionText.text = displayText
            divider.visibility = if (isLast) View.GONE else View.VISIBLE // Hide if last

            textContainer.setOnClickListener {
                onDivisionSelected(division)
                onDismiss()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DivisionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recycler_view, parent, false)
        return DivisionViewHolder(view)
    }

    override fun onBindViewHolder(holder: DivisionViewHolder, position: Int) {
        val isLast = position == divisions.size - 1 // Check if it's the last item
        holder.bind(divisions[position], position, isLast)
    }

    override fun getItemCount(): Int = divisions.size
}